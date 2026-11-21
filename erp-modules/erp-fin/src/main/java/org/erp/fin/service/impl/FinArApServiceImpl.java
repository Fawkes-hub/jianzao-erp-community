package org.erp.fin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.erp.common.enums.StatusText;
import org.erp.fin.domain.FinArAp;
import org.erp.fin.domain.FinPayment;
import org.erp.fin.domain.FinReceipt;
import org.erp.fin.domain.FinSourceNo;
import org.erp.fin.domain.vo.FinArApSummaryVo;
import org.erp.fin.domain.vo.FinArApVo;
import org.erp.fin.mapper.FinArApMapper;
import org.erp.fin.mapper.FinPaymentMapper;
import org.erp.fin.mapper.FinReceiptMapper;
import org.erp.fin.mapper.FinSourceNoMapper;
import org.erp.fin.service.IFinArApService;
import org.erp.master.domain.ErpCustomer;
import org.erp.master.domain.ErpSupplier;
import org.erp.master.mapper.ErpCustomerMapper;
import org.erp.master.mapper.ErpSupplierMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 应收应付台账服务实现
 *
 * 规则（DOC/11 §5.5、DOC/02 D1/R7/R16、DOC/12 错误码表）：
 * - 确认销售→AR、确认采购→AP（金额=total_amount 含运杂费）、期初确认→OPENING；
 * - 撤回/作废：settled_amount=0 才能冲销（VOID），否则 40904；无台账幂等成功；
 * - 汇总（S8）：总额 + 已结 + 未结，AR 客户余额另含 md_customer.opening_debt 展示（D1 不参与汇总）。
 */
@RequiredArgsConstructor
@Service
public class FinArApServiceImpl implements IFinArApService {

    private static final String PARTY_AR = "AR";
    private static final String PARTY_AP = "AP";
    private static final String STATUS_VOID = "VOID";
    private static final String STATUS_SETTLED = "SETTLED";
    private static final String SOURCE_OPENING = "OPENING";
    private static final String SOURCE_SALE = "SALE";
    private static final String SOURCE_PURCHASE = "PURCHASE";

    private final FinArApMapper arApMapper;
    private final FinSourceNoMapper sourceNoMapper;
    private final FinReceiptMapper receiptMapper;
    private final FinPaymentMapper paymentMapper;
    private final ErpCustomerMapper customerMapper;
    private final ErpSupplierMapper supplierMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createBySale(Long saleId, String saleNo, Long customerId, BigDecimal amount, Date bizDate) {
        create(PARTY_AR, customerId, "SALE", saleId, amount, bizDate);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createByPurchase(Long purchaseId, String purchaseNo, Long supplierId, BigDecimal amount, Date bizDate) {
        create(PARTY_AP, supplierId, "PURCHASE", purchaseId, amount, bizDate);
    }

    private void create(String partyType, Long partyId, String sourceType, Long sourceId,
                        BigDecimal amount, Date bizDate) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new ServiceException("应收应付金额不能为负");
        }
        Long exists = arApMapper.selectCount(new LambdaQueryWrapper<FinArAp>()
            .eq(FinArAp::getSourceType, sourceType)
            .eq(FinArAp::getSourceId, sourceId)
            .ne(FinArAp::getStatus, STATUS_VOID));
        if (exists != null && exists > 0) {
            throw new ServiceException("该单据已生成应收应付台账", 40902);
        }
        FinArAp row = new FinArAp();
        row.setPartyType(partyType);
        row.setPartyId(partyId);
        row.setSourceType(sourceType);
        row.setSourceId(sourceId);
        row.setBizDate(bizDate);
        row.setAmount(amount.setScale(2, java.math.RoundingMode.HALF_UP));
        row.setSettledAmount(BigDecimal.ZERO);
        row.setStatus("OPEN");
        try {
            arApMapper.insert(row);
        } catch (DuplicateKeyException e) {
            // 并发确认时两个事务都通过 selectCount 判重，靠 uk_fin_ar_ap_source 兜底并转 40902（§6.8.2 防线 5）
            throw new ServiceException("该单据已生成应收应付台账", 40902);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createBySaleReturn(Long returnId, String returnNo, Long customerId, BigDecimal amount, Date bizDate) {
        createNegative(PARTY_AR, customerId, "SALE_RETURN", returnId, amount, bizDate);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createByPurchaseReturn(Long returnId, String returnNo, Long supplierId, BigDecimal amount, Date bizDate) {
        createNegative(PARTY_AP, supplierId, "PURCHASE_RETURN", returnId, amount, bizDate);
    }

    /**
     * 退货负数台账行：与 create() 相同的 source 判重，但放开负数校验（DOC/24 §0「应收/应付红冲落点」）。
     * 负数行 settled 恒为 0（红字收款单对原 AR 行做 unsettle，不对负数行核销）。
     */
    private void createNegative(String partyType, Long partyId, String sourceType, Long sourceId,
                                BigDecimal amount, Date bizDate) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) >= 0) {
            throw new ServiceException("退货台账金额必须为负数");
        }
        Long exists = arApMapper.selectCount(new LambdaQueryWrapper<FinArAp>()
            .eq(FinArAp::getSourceType, sourceType)
            .eq(FinArAp::getSourceId, sourceId)
            .ne(FinArAp::getStatus, STATUS_VOID));
        if (exists != null && exists > 0) {
            throw new ServiceException("该退货单已生成红冲台账", 40902);
        }
        FinArAp row = new FinArAp();
        row.setPartyType(partyType);
        row.setPartyId(partyId);
        row.setSourceType(sourceType);
        row.setSourceId(sourceId);
        row.setBizDate(bizDate);
        row.setAmount(amount.setScale(2, java.math.RoundingMode.HALF_UP));
        row.setSettledAmount(BigDecimal.ZERO);
        row.setStatus("OPEN");
        try {
            arApMapper.insert(row);
        } catch (DuplicateKeyException e) {
            // 退货确认并发时同样靠唯一键兜底，转 40902 而不是通用 409（§6.8.2 防线 5）
            throw new ServiceException("该退货单已生成红冲台账", 40902);
        }
    }

    @Override
    public FinArApVo getBySource(String sourceType, Long sourceId) {
        FinArAp row = arApMapper.selectOne(new LambdaQueryWrapper<FinArAp>()
            .eq(FinArAp::getSourceType, sourceType)
            .eq(FinArAp::getSourceId, sourceId)
            .ne(FinArAp::getStatus, STATUS_VOID)
            .orderByDesc(FinArAp::getId)
            .last("limit 1"));
        return row == null ? null : toVo(row, Map.of());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createOpening(String partyType, Long partyId, BigDecimal amount, Long openingId, Date bizDate) {
        if (!PARTY_AR.equals(partyType) && !PARTY_AP.equals(partyType)) {
            throw new ServiceException("期初类型必须是 AR 或 AP");
        }
        // 期初可重录：先冲销旧期初（旧期初不允许已核销）。
        // 逐行加行锁再判 settled，避免读到 0 之后被并发收款核销掉
        List<FinArAp> oldOpenings = arApMapper.selectList(new LambdaQueryWrapper<FinArAp>()
            .eq(FinArAp::getPartyType, partyType)
            .eq(FinArAp::getPartyId, partyId)
            .eq(FinArAp::getSourceType, SOURCE_OPENING)
            .ne(FinArAp::getStatus, STATUS_VOID));
        for (FinArAp old : oldOpenings) {
            FinArAp locked = arApMapper.selectByIdForUpdate(old.getId());
            if (locked.getSettledAmount().compareTo(BigDecimal.ZERO) > 0) {
                throw new ServiceException("期初已发生核销，禁止重录", 40904);
            }
            locked.setStatus(STATUS_VOID);
            arApMapper.updateById(locked);
        }
        create(partyType, partyId, SOURCE_OPENING, openingId, amount, bizDate);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void voidBySource(String sourceType, Long sourceId) {
        List<FinArAp> rows = arApMapper.selectList(new LambdaQueryWrapper<FinArAp>()
            .eq(FinArAp::getSourceType, sourceType)
            .eq(FinArAp::getSourceId, sourceId)
            .ne(FinArAp::getStatus, STATUS_VOID));
        // 先逐行加锁校验，避免 check-then-act 竞态
        for (FinArAp row : rows) {
            FinArAp locked = arApMapper.selectByIdForUpdate(row.getId());
            if (locked.getSettledAmount().compareTo(BigDecimal.ZERO) > 0) {
                throw new ServiceException("已核销的应收应付需先反核销", 40904);
            }
        }
        for (FinArAp row : rows) {
            row.setStatus(STATUS_VOID);
            arApMapper.updateById(row);
        }
    }

    @Override
    public TableDataInfo<FinArApVo> selectPage(String partyType, Long partyId, String status, PageQuery pageQuery) {
        LambdaQueryWrapper<FinArAp> lqw = new LambdaQueryWrapper<>();
        lqw.eq(partyType != null && !partyType.isBlank(), FinArAp::getPartyType, partyType);
        lqw.eq(partyId != null, FinArAp::getPartyId, partyId);
        lqw.eq(status != null && !status.isBlank(), FinArAp::getStatus, status);
        lqw.orderByDesc(FinArAp::getId);
        Page<FinArAp> page = arApMapper.selectPage(pageQuery.build(), lqw);
        Page<FinArApVo> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        Map<Long, String> sourceNos = loadSourceNos(page.getRecords());
        voPage.setRecords(page.getRecords().stream().map(r -> toVo(r, sourceNos)).toList());
        return TableDataInfo.build(voPage);
    }

    @Override
    public FinArApSummaryVo summary(String partyType) {
        if (!PARTY_AR.equals(partyType) && !PARTY_AP.equals(partyType)) {
            throw new ServiceException("partyType 必须是 AR 或 AP");
        }
        List<FinArAp> rows = arApMapper.selectList(new LambdaQueryWrapper<FinArAp>()
            .eq(FinArAp::getPartyType, partyType)
            .ne(FinArAp::getStatus, STATUS_VOID));

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal settled = BigDecimal.ZERO;
        BigDecimal opening = BigDecimal.ZERO;
        Map<Long, FinArApSummaryVo.PartySummary> byParty = new LinkedHashMap<>();
        for (FinArAp row : rows) {
            total = total.add(row.getAmount());
            settled = settled.add(row.getSettledAmount());
            boolean isOpening = SOURCE_OPENING.equals(row.getSourceType());
            if (isOpening) {
                opening = opening.add(row.getAmount());
            }
            FinArApSummaryVo.PartySummary ps = byParty.computeIfAbsent(row.getPartyId(), k -> {
                FinArApSummaryVo.PartySummary s = new FinArApSummaryVo.PartySummary();
                s.setPartyId(k);
                s.setPartyName(resolvePartyName(partyType, k));
                s.setAmount(BigDecimal.ZERO);
                s.setSettled(BigDecimal.ZERO);
                s.setOpening(BigDecimal.ZERO);
                return s;
            });
            ps.setAmount(ps.getAmount().add(row.getAmount()));
            ps.setSettled(ps.getSettled().add(row.getSettledAmount()));
            if (isOpening) {
                ps.setOpening(ps.getOpening().add(row.getAmount()));
            }
        }
        for (FinArApSummaryVo.PartySummary ps : byParty.values()) {
            ps.setUnsettled(ps.getAmount().subtract(ps.getSettled()));
        }

        FinArApSummaryVo vo = new FinArApSummaryVo();
        vo.setPartyType(partyType);
        vo.setTotalAmount(total.setScale(2, java.math.RoundingMode.HALF_UP));
        vo.setSettledAmount(settled.setScale(2, java.math.RoundingMode.HALF_UP));
        vo.setUnsettledAmount(total.subtract(settled).setScale(2, java.math.RoundingMode.HALF_UP));
        vo.setOpeningAmount(opening.setScale(2, java.math.RoundingMode.HALF_UP));
        vo.setParties(new ArrayList<>(byParty.values()));
        return vo;
    }

    @Override
    public List<FinArApVo> listByParty(String partyType, Long partyId) {
        List<FinArAp> rows = arApMapper.selectList(new LambdaQueryWrapper<FinArAp>()
            .eq(FinArAp::getPartyType, partyType)
            .eq(FinArAp::getPartyId, partyId)
            .ne(FinArAp::getStatus, STATUS_VOID)
            .orderByAsc(FinArAp::getBizDate)
            .orderByAsc(FinArAp::getId));
        Map<Long, String> sourceNos = loadSourceNos(rows);
        return rows.stream().map(r -> toVo(r, sourceNos)).toList();
    }

    @Override
    public void settle(Long arApId, BigDecimal amount, String partyType, Long partyId) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException("核销金额必须大于0");
        }
        FinArAp row = arApMapper.selectByIdForUpdate(arApId);
        if (row == null || STATUS_VOID.equals(row.getStatus())) {
            throw new ServiceException("应收应付台账不存在或已作废：" + arApId);
        }
        if (!row.getPartyType().equals(partyType) || !row.getPartyId().equals(partyId)) {
            throw new ServiceException("核销台账不属于该往来单位：" + arApId);
        }
        BigDecimal unsettled = row.getAmount().subtract(row.getSettledAmount());
        if (amount.compareTo(unsettled) > 0) {
            throw new ServiceException("核销金额超过未核销余额：" + arApId
                + "，未核销 " + unsettled);
        }
        BigDecimal newSettled = row.getSettledAmount().add(amount).setScale(2, java.math.RoundingMode.HALF_UP);
        row.setSettledAmount(newSettled);
        row.setStatus(newSettled.compareTo(row.getAmount()) >= 0 ? STATUS_SETTLED : "PARTIAL");
        arApMapper.updateById(row);
    }

    @Override
    public void unsettle(Long arApId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException("反核销金额必须大于0");
        }
        FinArAp row = arApMapper.selectByIdForUpdate(arApId);
        if (row == null || STATUS_VOID.equals(row.getStatus())) {
            throw new ServiceException("应收应付台账不存在或已作废：" + arApId);
        }
        if (amount.compareTo(row.getSettledAmount()) > 0) {
            throw new ServiceException("反核销金额超过已核销金额：" + arApId);
        }
        BigDecimal newSettled = row.getSettledAmount().subtract(amount).setScale(2, java.math.RoundingMode.HALF_UP);
        row.setSettledAmount(newSettled);
        row.setStatus(newSettled.compareTo(BigDecimal.ZERO) == 0 ? "OPEN" : "PARTIAL");
        arApMapper.updateById(row);
    }

    /**
     * 批量加载来源单号（SALE→biz_sale.no，PURCHASE→biz_purchase.no，OPENING→无）。
     * 避免列表接口 N+1 查询。
     */
    private Map<Long, String> loadSourceNos(Collection<FinArAp> rows) {
        Map<Long, String> nos = new HashMap<>();
        List<Long> saleIds = rows.stream()
            .filter(r -> SOURCE_SALE.equals(r.getSourceType()))
            .map(FinArAp::getSourceId).filter(Objects::nonNull).distinct().toList();
        if (!saleIds.isEmpty()) {
            for (FinSourceNo row : sourceNoMapper.selectSaleNos(saleIds)) {
                nos.put(row.getSourceId(), row.getNo());
            }
        }
        List<Long> purchaseIds = rows.stream()
            .filter(r -> SOURCE_PURCHASE.equals(r.getSourceType()))
            .map(FinArAp::getSourceId).filter(Objects::nonNull).distinct().toList();
        if (!purchaseIds.isEmpty()) {
            for (FinSourceNo row : sourceNoMapper.selectPurchaseNos(purchaseIds)) {
                nos.put(row.getSourceId(), row.getNo());
            }
        }
        List<Long> saleReturnIds = rows.stream()
            .filter(r -> "SALE_RETURN".equals(r.getSourceType()))
            .map(FinArAp::getSourceId).filter(Objects::nonNull).distinct().toList();
        if (!saleReturnIds.isEmpty()) {
            for (FinSourceNo row : sourceNoMapper.selectSaleReturnNos(saleReturnIds)) {
                nos.put(row.getSourceId(), row.getNo());
            }
        }
        List<Long> purchaseReturnIds = rows.stream()
            .filter(r -> "PURCHASE_RETURN".equals(r.getSourceType()))
            .map(FinArAp::getSourceId).filter(Objects::nonNull).distinct().toList();
        if (!purchaseReturnIds.isEmpty()) {
            for (FinSourceNo row : sourceNoMapper.selectPurchaseReturnNos(purchaseReturnIds)) {
                nos.put(row.getSourceId(), row.getNo());
            }
        }
        return nos;
    }

    private FinArApVo toVo(FinArAp row, Map<Long, String> sourceNos) {
        FinArApVo vo = new FinArApVo();
        vo.setId(row.getId());
        vo.setPartyType(row.getPartyType());
        vo.setPartyId(row.getPartyId());
        vo.setPartyName(resolvePartyName(row.getPartyType(), row.getPartyId()));
        vo.setSourceType(row.getSourceType());
        vo.setSourceId(row.getSourceId());
        vo.setSourceNo(row.getSourceId() == null ? null : sourceNos.get(row.getSourceId()));
        vo.setBizDate(row.getBizDate());
        vo.setAmount(row.getAmount());
        vo.setSettledAmount(row.getSettledAmount());
        vo.setUnsettledAmount(row.getAmount().subtract(row.getSettledAmount()));
        vo.setStatus(row.getStatus());
        vo.setStatusText(StatusText.arAp(row.getStatus()));
        return vo;
    }

    private String resolvePartyName(String partyType, Long partyId) {
        if (partyId == null) {
            return null;
        }
        if (PARTY_AR.equals(partyType)) {
            ErpCustomer c = customerMapper.selectById(partyId);
            return c == null ? null : c.getName();
        }
        ErpSupplier s = supplierMapper.selectById(partyId);
        return s == null ? null : s.getName();
    }

    /**
     * 统计期初截止日之前已确认的业务单据数（AR=销售/销售退货/收款，AP=采购/采购退货/付款）。
     *
     * @param partyType AR/AP
     * @param partyId   客户/供应商ID
     * @param before    时间上界（不含）
     * @return 区间内已确认单据数
     */
    @Override
    public long countBusinessBefore(String partyType, Long partyId, Date before) {
        boolean ar = PARTY_AR.equals(partyType);
        // 只列业务来源，天然排除 OPENING 自身，避免重录期初时把旧期初行算成"已存在单据"
        List<String> sources = ar
            ? List.of("SALE", "SALE_RETURN")
            : List.of("PURCHASE", "PURCHASE_RETURN");
        Long ledger = arApMapper.selectCount(new LambdaQueryWrapper<FinArAp>()
            .eq(FinArAp::getPartyType, partyType)
            .eq(FinArAp::getPartyId, partyId)
            .in(FinArAp::getSourceType, sources)
            .ne(FinArAp::getStatus, STATUS_VOID)
            .lt(FinArAp::getBizDate, before));
        Long settled = ar
            ? receiptMapper.selectCount(new LambdaQueryWrapper<FinReceipt>()
                .eq(FinReceipt::getCustomerId, partyId)
                .eq(FinReceipt::getStatus, "CONFIRMED")
                .lt(FinReceipt::getBizDate, before))
            : paymentMapper.selectCount(new LambdaQueryWrapper<FinPayment>()
                .eq(FinPayment::getSupplierId, partyId)
                .eq(FinPayment::getStatus, "CONFIRMED")
                .lt(FinPayment::getBizDate, before));
        return (ledger == null ? 0L : ledger) + (settled == null ? 0L : settled);
    }
}
