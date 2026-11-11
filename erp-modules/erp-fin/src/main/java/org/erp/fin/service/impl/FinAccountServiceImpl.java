package org.erp.fin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.erp.fin.domain.FinAccount;
import org.erp.fin.domain.FinOtherFlow;
import org.erp.fin.domain.bo.FinAccountBo;
import org.erp.fin.domain.vo.FinAccountFlowVo;
import org.erp.fin.domain.vo.FinAccountVo;
import org.erp.fin.mapper.FinAccountMapper;
import org.erp.fin.mapper.FinOtherFlowMapper;
import org.erp.fin.mapper.FinPaymentMapper;
import org.erp.fin.mapper.FinReceiptMapper;
import org.erp.fin.service.IFinAccountService;
import org.erp.master.domain.ErpCustomer;
import org.erp.master.domain.ErpSupplier;
import org.erp.master.mapper.ErpCustomerMapper;
import org.erp.master.mapper.ErpSupplierMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 结算账户服务实现（DOC/25 §3.4/§3.5）
 *
 * 规则：
 * - 余额 = 期初 + Σ收款 − Σ付款 + Σ其他收入 − Σ其他支出（仅 account_id 命中且 CONFIRMED），实时计算不落表；
 * - code 不可改；期初余额仅无动账时可改；余额≠0 拒绝停用；
 * - 删除（逻辑删）：名下存在任意状态（含 DRAFT）收付款/其他收支单或余额≠0 时拒绝；
 * - 流水：三类单据内存 UNION，按 biz_date+id 倒序分页。
 */
@RequiredArgsConstructor
@Service
public class FinAccountServiceImpl implements IFinAccountService {

    private final FinAccountMapper accountMapper;
    private final FinReceiptMapper receiptMapper;
    private final FinPaymentMapper paymentMapper;
    private final FinOtherFlowMapper otherFlowMapper;
    private final ErpCustomerMapper customerMapper;
    private final ErpSupplierMapper supplierMapper;

    @Override
    public TableDataInfo<FinAccountVo> selectPage(String keyword, String type, Integer status, PageQuery pageQuery) {
        LambdaQueryWrapper<FinAccount> lqw = new LambdaQueryWrapper<>();
        lqw.eq(type != null && !type.isBlank(), FinAccount::getType, type);
        lqw.eq(status != null, FinAccount::getStatus, status);
        if (keyword != null && !keyword.isBlank()) {
            lqw.and(w -> w.like(FinAccount::getCode, keyword).or().like(FinAccount::getName, keyword));
        }
        lqw.orderByAsc(FinAccount::getCode);
        Page<FinAccount> page = accountMapper.selectPage(pageQuery.build(), lqw);
        Page<FinAccountVo> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(r -> toVo(r, true)).toList());
        return TableDataInfo.build(voPage);
    }

    @Override
    public FinAccountVo selectById(Long id) {
        FinAccount head = accountMapper.selectById(id);
        if (head == null) {
            throw new ServiceException("结算账户不存在");
        }
        return toVo(head, true);
    }

    private FinAccountVo toVo(FinAccount head, boolean withBalance) {
        FinAccountVo vo = new FinAccountVo();
        vo.setId(head.getId());
        vo.setCode(head.getCode());
        vo.setName(head.getName());
        vo.setType(head.getType());
        vo.setOpeningBalance(head.getOpeningBalance());
        vo.setStatus(head.getStatus());
        vo.setRemark(head.getRemark());
        vo.setCreateTime(head.getCreateTime());
        if (withBalance) {
            vo.setBalance(balance(head.getId()));
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void insert(FinAccountBo bo) {
        // 同码已删档：复用该行重建，避免 uk(tenant_id, code) 含已删行导致"删了建不回"（ErpProductMapper 先例）
        Long deletedId = accountMapper.selectDeletedIdByCode(bo.getCode());
        if (deletedId != null) {
            throw new ServiceException("编码已被已删除账户占用：" + bo.getCode() + "，请更换编码", 40903);
        }
        checkCodeUnique(null, bo.getCode());
        FinAccount head = new FinAccount();
        head.setCode(bo.getCode());
        head.setName(bo.getName());
        head.setType(bo.getType());
        head.setOpeningBalance(bo.getOpeningBalance() == null ? BigDecimal.ZERO
            : bo.getOpeningBalance().setScale(2, RoundingMode.HALF_UP));
        head.setStatus(bo.getStatus() == null ? 1 : bo.getStatus());
        head.setRemark(bo.getRemark());
        accountMapper.insert(head);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(FinAccountBo bo) {
        if (bo.getId() == null) {
            throw new ServiceException("账户ID不能为空");
        }
        FinAccount head = accountMapper.selectById(bo.getId());
        if (head == null) {
            throw new ServiceException("结算账户不存在");
        }
        if (bo.getCode() != null && !bo.getCode().equals(head.getCode())) {
            throw new ServiceException("账户编码不可修改", 40902);
        }
        boolean hasMovement = hasAnyBill(head.getId());
        // 期初余额仅无动账时可改（有动账后期初语义已锚定）
        if (bo.getOpeningBalance() != null
            && bo.getOpeningBalance().setScale(2, RoundingMode.HALF_UP).compareTo(head.getOpeningBalance()) != 0
            && hasMovement) {
            throw new ServiceException("账户已发生收支动账，期初余额不可修改", 40902);
        }
        // 余额≠0 拒绝停用（欠款账户停用后动账不可见）
        if (bo.getStatus() != null && bo.getStatus() == 0 && balance(head.getId()).compareTo(BigDecimal.ZERO) != 0) {
            throw new ServiceException("账户余额不为零，不允许停用", 40902);
        }
        head.setName(bo.getName());
        head.setType(bo.getType());
        if (bo.getOpeningBalance() != null) {
            head.setOpeningBalance(bo.getOpeningBalance().setScale(2, RoundingMode.HALF_UP));
        }
        if (bo.getStatus() != null) {
            head.setStatus(bo.getStatus());
        }
        head.setRemark(bo.getRemark());
        accountMapper.updateById(head);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id) {
        FinAccount head = accountMapper.selectById(id);
        if (head == null) {
            throw new ServiceException("结算账户不存在");
        }
        // 引用校验含 DRAFT：放任"建草稿→删户→确认草稿"会把钱记入已删除、列表不可见的账户
        if (hasAnyBill(id)) {
            throw new ServiceException("账户已被收付款/其他收支单据引用，不允许删除", 40902);
        }
        if (balance(id).compareTo(BigDecimal.ZERO) != 0) {
            throw new ServiceException("账户余额不为零，不允许删除", 40902);
        }
        accountMapper.deleteById(id);
    }

    @Override
    public List<FinAccountVo> options() {
        List<FinAccount> rows = accountMapper.selectList(new LambdaQueryWrapper<FinAccount>()
            .eq(FinAccount::getStatus, 1)
            .orderByAsc(FinAccount::getCode));
        return rows.stream().map(r -> toVo(r, false)).toList();
    }

    @Override
    public TableDataInfo<FinAccountFlowVo> flows(Long accountId, PageQuery pageQuery) {
        FinAccount head = accountMapper.selectById(accountId);
        if (head == null) {
            throw new ServiceException("结算账户不存在");
        }
        // 单租户小数据量：三类单据各查一次后内存 UNION 排序分页，避免跨表 SQL 复杂度
        List<FinAccountFlowVo> all = new ArrayList<>();
        receiptMapper.selectList(new LambdaQueryWrapper<org.erp.fin.domain.FinReceipt>()
                .eq(org.erp.fin.domain.FinReceipt::getAccountId, accountId)
                .eq(org.erp.fin.domain.FinReceipt::getStatus, "CONFIRMED"))
            .forEach(r -> {
                FinAccountFlowVo vo = new FinAccountFlowVo();
                vo.setBizType("RECEIPT");
                vo.setBizNo(r.getNo());
                vo.setBizDate(r.getBizDate());
                vo.setAmount(r.getAmount());
                vo.setDirection("IN");
                vo.setCounterparty(customerName(r.getCustomerId()));
                vo.setRowId(r.getId());
                all.add(vo);
            });
        paymentMapper.selectList(new LambdaQueryWrapper<org.erp.fin.domain.FinPayment>()
                .eq(org.erp.fin.domain.FinPayment::getAccountId, accountId)
                .eq(org.erp.fin.domain.FinPayment::getStatus, "CONFIRMED"))
            .forEach(r -> {
                FinAccountFlowVo vo = new FinAccountFlowVo();
                vo.setBizType("PAYMENT");
                vo.setBizNo(r.getNo());
                vo.setBizDate(r.getBizDate());
                vo.setAmount(r.getAmount());
                vo.setDirection("OUT");
                vo.setCounterparty(supplierName(r.getSupplierId()));
                vo.setRowId(r.getId());
                all.add(vo);
            });
        otherFlowMapper.selectList(new LambdaQueryWrapper<FinOtherFlow>()
                .eq(FinOtherFlow::getAccountId, accountId)
                .eq(FinOtherFlow::getStatus, "CONFIRMED"))
            .forEach(r -> {
                FinAccountFlowVo vo = new FinAccountFlowVo();
                vo.setBizType("OTHER_FLOW");
                vo.setBizNo(r.getNo());
                vo.setBizDate(r.getBizDate());
                vo.setAmount(r.getAmount());
                vo.setDirection("INCOME".equals(r.getFlowType()) ? "IN" : "OUT");
                vo.setCounterparty(r.getRemark());
                vo.setRowId(r.getId());
                all.add(vo);
            });
        all.sort(Comparator.comparing(FinAccountFlowVo::getBizDate,
            Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(FinAccountFlowVo::getRowId,
            Comparator.reverseOrder()));

        long total = all.size();
        int from = (int) Math.min((pageQuery.getPageNum() - 1L) * pageQuery.getPageSize(), total);
        int to = (int) Math.min(from + pageQuery.getPageSize(), total);
        Page<FinAccountFlowVo> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize(), total);
        page.setRecords(all.subList(from, to));
        return TableDataInfo.build(page);
    }

    /**
     * 实时余额 = 期初 + Σ收 − Σ付 + Σ其他收入 − Σ其他支出（仅 account_id 命中且 CONFIRMED）。
     */
    private BigDecimal balance(Long accountId) {
        BigDecimal receipt = receiptMapper.selectList(new LambdaQueryWrapper<org.erp.fin.domain.FinReceipt>()
                .select(org.erp.fin.domain.FinReceipt::getAmount)
                .eq(org.erp.fin.domain.FinReceipt::getAccountId, accountId)
                .eq(org.erp.fin.domain.FinReceipt::getStatus, "CONFIRMED"))
            .stream().map(r -> r.getAmount() == null ? BigDecimal.ZERO : r.getAmount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal payment = paymentMapper.selectList(new LambdaQueryWrapper<org.erp.fin.domain.FinPayment>()
                .select(org.erp.fin.domain.FinPayment::getAmount)
                .eq(org.erp.fin.domain.FinPayment::getAccountId, accountId)
                .eq(org.erp.fin.domain.FinPayment::getStatus, "CONFIRMED"))
            .stream().map(r -> r.getAmount() == null ? BigDecimal.ZERO : r.getAmount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<FinOtherFlow> flows = otherFlowMapper.selectList(new LambdaQueryWrapper<FinOtherFlow>()
            .select(FinOtherFlow::getAmount, FinOtherFlow::getFlowType)
            .eq(FinOtherFlow::getAccountId, accountId)
            .eq(FinOtherFlow::getStatus, "CONFIRMED"));
        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;
        for (FinOtherFlow f : flows) {
            BigDecimal amt = f.getAmount() == null ? BigDecimal.ZERO : f.getAmount();
            if ("INCOME".equals(f.getFlowType())) {
                income = income.add(amt);
            } else {
                expense = expense.add(amt);
            }
        }
        FinAccount head = accountMapper.selectById(accountId);
        BigDecimal opening = head == null || head.getOpeningBalance() == null
            ? BigDecimal.ZERO : head.getOpeningBalance();
        return opening.add(receipt).subtract(payment).add(income).subtract(expense)
            .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 名下是否存在任意状态（含草稿）的收付款/其他收支单。
     */
    private boolean hasAnyBill(Long accountId) {
        Long n1 = receiptMapper.selectCount(new LambdaQueryWrapper<org.erp.fin.domain.FinReceipt>()
            .eq(org.erp.fin.domain.FinReceipt::getAccountId, accountId));
        Long n2 = paymentMapper.selectCount(new LambdaQueryWrapper<org.erp.fin.domain.FinPayment>()
            .eq(org.erp.fin.domain.FinPayment::getAccountId, accountId));
        Long n3 = otherFlowMapper.selectCount(new LambdaQueryWrapper<FinOtherFlow>()
            .eq(FinOtherFlow::getAccountId, accountId));
        return (n1 != null && n1 > 0) || (n2 != null && n2 > 0) || (n3 != null && n3 > 0);
    }

    private void checkCodeUnique(Long id, String code) {
        Long count = accountMapper.selectCount(new LambdaQueryWrapper<FinAccount>()
            .eq(FinAccount::getCode, code)
            .ne(id != null, FinAccount::getId, id));
        if (count != null && count > 0) {
            throw new ServiceException("账户编码已存在：" + code, 40903);
        }
    }

    private String customerName(Long customerId) {
        if (customerId == null) {
            return null;
        }
        ErpCustomer c = customerMapper.selectById(customerId);
        return c == null ? null : c.getName();
    }

    private String supplierName(Long supplierId) {
        if (supplierId == null) {
            return null;
        }
        ErpSupplier s = supplierMapper.selectById(supplierId);
        return s == null ? null : s.getName();
    }
}
