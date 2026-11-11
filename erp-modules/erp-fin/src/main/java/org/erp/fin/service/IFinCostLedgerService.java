package org.erp.fin.service;

import java.util.Date;

/**
 * 成本台账服务（综合成本，R20）
 *
 * 生产材料成本 + 其他出库成本 + 计入成本的其他支出；
 * 撤回置 REVERSED 不删行（审计）。
 */
public interface IFinCostLedgerService {

    /**
     * 记一笔成本。
     *
     * @param costType  PRODUCTION_MATERIAL/OTHER_OUT/OTHER_EXPENSE
     * @param sourceType PRODUCTION/OTHER_OUT/OTHER_FLOW
     */
    void record(String costType, String sourceType, Long sourceId, String sourceNo,
                Date bizDate, java.math.BigDecimal amount, String remark);

    /**
     * 按 source 反向：ACTIVE → REVERSED。幂等：无 ACTIVE 行时静默成功。
     */
    void reverseBySource(String sourceType, Long sourceId);
}
