package org.erp.fin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.erp.common.enums.StatusText;
import org.erp.common.sequence.SequenceService;
import org.erp.fin.domain.FinArAp;
import org.erp.fin.domain.FinPayment;
import org.erp.fin.domain.FinPaymentAlloc;
import org.erp.fin.domain.bo.FinPaymentBo;
import org.erp.fin.domain.vo.FinPaymentVo;
import org.erp.fin.mapper.FinPaymentAllocMapper;
import org.erp.fin.mapper.FinPaymentMapper;
import org.erp.fin.service.IFinArApService;
import org.erp.fin.service.IFinPaymentService;
import org.erp.master.domain.ErpSupplier;
import org.erp.master.mapper.ErpSupplierMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 付款单服务实现
 *
 * 规则与收款单对称：确认落 fin_payment_alloc 核销 AP 台账；
 * 撤回先反核销；作废走撤回；支持部分付款、一付多单。
 */
@RequiredArgsConstructor
@Service
public class FinPaymentServiceImpl implements IFinPaymentService {

    private final FinPaymentMapper paymentMapper;
    private final org.erp.fin.mapper.FinAccountMapper accountMapper;
    private final FinPaymentAllocMapper allocMapper;
    private final ErpSupplierMapper supplierMapper;
    private final IFinArApService arApService;
    private final SequenceService sequenceService;

    @Override
    public TableDataInfo<FinPaymentVo> selectPage(Long supplierId, String status, PageQuery pageQuery) {
        LambdaQueryWrapper<FinPayment> lqw = new LambdaQueryWrapper<>();
        lqw.eq(supplierId != null, FinPayment::getSupplierId, supplierId);
        lqw.eq(status != null && !status.isBlank(), FinPayment::getStatus, status);
        lqw.orderByDesc(FinPayment::getId);
        Page<FinPayment> page = paymentMapper.selectPage(pageQuery.build(), lqw);
        Page<FinPaymentVo> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::toVo).toList());
        return TableDataInfo.build(voPage);
    }

    @Override
    public FinPaymentVo selectById(Long id) {
        FinPayment head = paymentMapper.selectById(id);
        if (head == null) {
            throw new ServiceException("付款单不存在");
        }
        FinPaymentVo vo = toVo(head);
        vo.setAllocations(loadAllocs(id));
        return vo;
    }

    private FinPaymentVo toVo(FinPayment head) {
        FinPaymentVo vo = new FinPaymentVo();
        vo.setId(head.getId());
        vo.setNo(head.getNo());
        vo.setSupplierId(head.getSupplierId());
        ErpSupplier s = supplierMapper.selectById(head.getSupplierId());
        vo.setSupplierName(s == null ? null : s.getName());
        vo.setAmount(head.getAmount());
        vo.setBizDate(head.getBizDate());
        vo.setAccount(head.getAccount());
        vo.setAccountId(head.getAccountId());
        vo.setRemark(head.getRemark());
        vo.setStatus(head.getStatus());
        vo.setStatusText(StatusText.bill(head.getStatus()));
        vo.setCreateTime(head.getCreateTime());
        return vo;
    }

    private List<FinPaymentVo.AllocVo> loadAllocs(Long paymentId) {
        List<FinPaymentAlloc> rows = allocMapper.selectList(new LambdaQueryWrapper<FinPaymentAlloc>()
            .eq(FinPaymentAlloc::getPaymentId, paymentId)
            .orderByAsc(FinPaymentAlloc::getId));
        List<FinPaymentVo.AllocVo> result = new ArrayList<>();
        for (FinPaymentAlloc row : rows) {
            FinPaymentVo.AllocVo vo = new FinPaymentVo.AllocVo();
            vo.setId(row.getId());
            vo.setArApId(row.getArApId());
            vo.setAmount(row.getAmount());
            result.add(vo);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinPaymentVo insert(FinPaymentBo bo) {
        FinPayment head = new FinPayment();
        head.setNo(sequenceService.nextNo("FK"));
        saveHead(head, bo);
        saveAllocations(head.getId(), bo);
        if (Boolean.TRUE.equals(bo.getConfirm())) {
            confirmInternal(head);
        }
        return selectById(head.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinPaymentVo update(Long id, FinPaymentBo bo) {
        FinPayment head = paymentMapper.selectById(id);
        if (head == null) {
            throw new ServiceException("付款单不存在");
        }
        if (!"DRAFT".equals(head.getStatus())) {
            throw new ServiceException("仅草稿可编辑", 40902);
        }
        saveHead(head, bo);
        saveAllocations(head.getId(), bo);
        if (Boolean.TRUE.equals(bo.getConfirm())) {
            confirmInternal(head);
        }
        return selectById(id);
    }

    private void saveHead(FinPayment head, FinPaymentBo bo) {
        head.setSupplierId(bo.getSupplierId());
        head.setAmount(bo.getAmount().setScale(2, RoundingMode.HALF_UP));
        head.setBizDate(bo.getBizDate());
        // DOC/25 §3.6：新增必须选账户；存量草稿编辑（id 非空）不强制回填
        if (head.getId() == null && bo.getAccountId() == null) {
            throw new ServiceException("请选择结算账户");
        }
        applyAccount(head, bo.getAccountId(), bo.getAccount());
        head.setRemark(bo.getRemark());
        head.setStatus("DRAFT");
        if (head.getId() == null) {
            paymentMapper.insert(head);
        } else {
            paymentMapper.updateById(head);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long id) {
        FinPayment head = paymentMapper.selectById(id);
        if (head == null) {
            throw new ServiceException("付款单不存在");
        }
        if (!"DRAFT".equals(head.getStatus())) {
            throw new ServiceException("仅草稿可确认", 40902);
        }
        confirmInternal(head);
    }

    private void confirmInternal(FinPayment head) {
        // 先验后核：Σalloc=amount 校验通过后再逐行 settle
        List<FinPaymentAlloc> pending = allocMapper.selectList(new LambdaQueryWrapper<FinPaymentAlloc>()
            .eq(FinPaymentAlloc::getPaymentId, head.getId()));
        BigDecimal sum = pending.stream()
            .map(FinPaymentAlloc::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sum.compareTo(head.getAmount()) != 0) {
            throw new ServiceException("核销明细合计(" + sum + ")必须等于付款金额(" + head.getAmount() + ")");
        }
        for (FinPaymentAlloc row : pending) {
            arApService.settle(row.getArApId(), row.getAmount(), "AP", head.getSupplierId());
        }
        head.setStatus("CONFIRMED");
        head.setConfirmedAt(new Date());
        head.setConfirmedBy(currentUserId());
        paymentMapper.updateById(head);
    }

    private void saveAllocations(Long paymentId, FinPaymentBo bo) {
        allocMapper.delete(new LambdaQueryWrapper<FinPaymentAlloc>()
            .eq(FinPaymentAlloc::getPaymentId, paymentId));
        for (FinPaymentBo.AllocationBo alloc : bo.getAllocations()) {
            FinPaymentAlloc row = new FinPaymentAlloc();
            row.setPaymentId(paymentId);
            row.setArApId(alloc.getArApId());
            row.setAmount(alloc.getAmount().setScale(2, RoundingMode.HALF_UP));
            allocMapper.insert(row);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdraw(Long id) {
        FinPayment head = paymentMapper.selectById(id);
        if (head == null) {
            throw new ServiceException("付款单不存在");
        }
        if (!"CONFIRMED".equals(head.getStatus())) {
            throw new ServiceException("仅已确认单据可撤回", 40902);
        }
        unsettleAll(head.getId());
        head.setStatus("DRAFT");
        head.setWithdrawnAt(new Date());
        head.setWithdrawnBy(currentUserId());
        paymentMapper.updateById(head);
    }

    private void unsettleAll(Long paymentId) {
        List<FinPaymentAlloc> rows = allocMapper.selectList(new LambdaQueryWrapper<FinPaymentAlloc>()
            .eq(FinPaymentAlloc::getPaymentId, paymentId));
        for (FinPaymentAlloc row : rows) {
            arApService.unsettle(row.getArApId(), row.getAmount());
        }
        // 保留核销明细：撤回后单据回草稿，用户可直接重新确认；
        // 终态作废时才由 voidBill 物理清理。
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void voidBill(Long id) {
        FinPayment head = paymentMapper.selectById(id);
        if (head == null) {
            throw new ServiceException("付款单不存在");
        }
        if ("VOID".equals(head.getStatus())) {
            throw new ServiceException("单据已作废");
        }
        // DRAFT 草稿不再走作废：草稿应走删除（逻辑删除），作废会把草稿固化成终态单据并占用唯一键
        if (!"CONFIRMED".equals(head.getStatus())) {
            throw new ServiceException("草稿单据请使用删除", 40902);
        }
        // CONFIRMED：先撤回反核销回草稿，再置作废
        withdraw(id);
        // 作废清理草稿期残留核销明细
        allocMapper.delete(new LambdaQueryWrapper<FinPaymentAlloc>()
            .eq(FinPaymentAlloc::getPaymentId, id));
        head = paymentMapper.selectById(id);
        head.setStatus("VOID");
        head.setVoidedAt(new Date());
        head.setVoidedBy(currentUserId());
        paymentMapper.updateById(head);
    }

    /**
     * 账户引用写入（DOC/25 §3.6）：accountId 非空时写 account_id 并回填账户名称到 account 展示列；
     * 为空（存量草稿）时保留原 account 文本不动。
     */
    private void applyAccount(FinPayment head, Long accountId, String accountText) {
        if (accountId != null) {
            org.erp.fin.domain.FinAccount acc = accountMapper.selectById(accountId);
            if (acc == null) {
                throw new ServiceException("结算账户不存在：" + accountId);
            }
            head.setAccountId(accountId);
            head.setAccount(acc.getName());
        } else if (head.getAccountId() == null && accountText != null) {
            head.setAccount(accountText);
        }
    }

    private Long currentUserId() {
        try {
            return LoginHelper.getUserId();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 删除草稿付款单（逻辑删除）。
     *
     * 规则：
     * - 仅 DRAFT 可删，CONFIRMED/VOID 一律 40902 拒绝；
     * - 逻辑删除（del_flag=1），主表与核销明细一并逻辑删除，不物理 DELETE；
     * - 单号不回收（uk_fin_payment_no 含已删行，新建同名单号会被唯一键挡住）；
     * - 草稿未核销过台账（核销只在确认时发生），无需反核销。
     *
     * @param id 付款单ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDraft(Long id) {
        FinPayment head = paymentMapper.selectById(id);
        if (head == null) {
            throw new ServiceException("付款单不存在");
        }
        if (!"DRAFT".equals(head.getStatus())) {
            throw new ServiceException("仅草稿单据可删除，已确认/已作废单据请先撤回或作废处理", 40902);
        }
        // 明细一并逻辑删除，避免残留核销行影响后续查询
        allocMapper.delete(new LambdaQueryWrapper<FinPaymentAlloc>()
            .eq(FinPaymentAlloc::getPaymentId, id));
        paymentMapper.deleteById(id);
    }
}
