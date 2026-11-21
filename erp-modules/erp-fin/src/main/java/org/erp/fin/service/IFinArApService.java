package org.erp.fin.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.erp.fin.domain.vo.FinArApSummaryVo;
import org.erp.fin.domain.vo.FinArApVo;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 应收应付台账服务
 *
 * 台账只增不改（除核销/状态流转）；业务单据确认时写入，撤回/作废时冲销。
 */
public interface IFinArApService {

    /**
     * 确认销售单时生成应收（partyType=AR, sourceType=SALE）。
     * 幂等：同 source 已存在非 VOID 台账则抛 40902。
     */
    void createBySale(Long saleId, String saleNo, Long customerId, BigDecimal amount, Date bizDate);

    /**
     * 确认采购单时生成应付（partyType=AP, sourceType=PURCHASE，金额含运杂费）。
     * 幂等：同 source 已存在非 VOID 台账则抛 40902。
     */
    void createByPurchase(Long purchaseId, String purchaseNo, Long supplierId, BigDecimal amount, Date bizDate);

    /**
     * 期初确认（D1）：fin_opening 确认后落台账 sourceType=OPENING。
     * 同 party 同类型已存在 OPENING 台账则先冲销旧期初（期初可重录，取最新）。
     */
    void createOpening(String partyType, Long partyId, BigDecimal amount, Long openingId, Date bizDate);

    /**
     * 确认销售退货时生成负数应收行（DOC/24 §6.3，sourceType=SALE_RETURN，amount<0）。
     * 幂等：同 source 已存在非 VOID 台账则抛 40902；原销售单 AR 行不动。
     */
    void createBySaleReturn(Long returnId, String returnNo, Long customerId, BigDecimal amount, Date bizDate);

    /**
     * 确认采购退货时生成负数应付行（DOC/24 §6.5，sourceType=PURCHASE_RETURN，amount<0）。
     */
    void createByPurchaseReturn(Long returnId, String returnNo, Long supplierId, BigDecimal amount, Date bizDate);

    /**
     * 按来源取非 VOID 台账行（DOC/24 §3.1）：退货退款取原单 AR 行 id 与 settled_amount。
     * 不存在时返回 null。
     */
    FinArApVo getBySource(String sourceType, Long sourceId);

    /**
     * 单据撤回/作废时冲销台账：将该 source 下全部非 VOID 台账置 VOID。
     * settled_amount > 0 抛 40904（已核销需先反核销）；
     * 无台账时幂等成功（生产单等无台账来源）。
     */
    void voidBySource(String sourceType, Long sourceId);

    /**
     * 台账分页查询。
     */
    TableDataInfo<FinArApVo> selectPage(String partyType, Long partyId, String status, PageQuery pageQuery);

    /**
     * S8 汇总：总应收/总应付 + 按客户/供应商分组明细（含期初拆分）。
     */
    FinArApSummaryVo summary(String partyType);

    /**
     * S8 明细：单客户/供应商台账列表。
     */
    List<FinArApVo> listByParty(String partyType, Long partyId);

    /**
     * 核销：累计 settled_amount 并推进状态（OPEN/PARTIAL → SETTLED）。
     * 校验：台账存在且未作废、归属该 party、行核销 ≤ 未核销余额。
     */
    void settle(Long arApId, BigDecimal amount, String partyType, Long partyId);

    /**
     * 反核销：settled_amount 减回并回退状态（SETTLED → PARTIAL/OPEN）。
     * 校验：台账存在、反核销额 ≤ 已核销额。
     */
    void unsettle(Long arApId, BigDecimal amount);

    /**
     * 统计期初截止日之前已确认的业务单据数：AR 为销售/销售退货/收款，AP 为采购/采购退货/付款。
     *
     * 期初金额表示截止日之前的账面余额，若该区间已有业务单据，对账单会把期初与台账重复累加，
     * 因此期初确认前必须先拦截。
     *
     * @param partyType AR/AP
     * @param partyId   客户/供应商ID
     * @param before    时间上界（不含），由截止日次日的 00:00 换算
     * @return 区间内已确认单据数，0 表示无重叠
     */
    long countBusinessBefore(String partyType, Long partyId, Date before);
}
