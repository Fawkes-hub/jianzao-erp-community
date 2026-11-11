package org.erp.fin.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.erp.fin.domain.bo.FinOtherFlowBo;
import org.erp.fin.domain.vo.FinOtherFlowVo;

/**
 * 其他收支服务（S7/R19）
 *
 * 独立于应收应付；支出 affect_cost=1 确认时写 fin_cost_ledger(OTHER_EXPENSE)。
 */
public interface IFinOtherFlowService {

    TableDataInfo<FinOtherFlowVo> selectPage(String flowType, String category, String status, PageQuery pageQuery);

    FinOtherFlowVo selectById(Long id);

    FinOtherFlowVo insert(FinOtherFlowBo bo);

    FinOtherFlowVo update(Long id, FinOtherFlowBo bo);

    void confirm(Long id);

    void withdraw(Long id);

    void voidBill(Long id);

    /**
     * 删除草稿其他收支单（逻辑删除，仅 DRAFT 可删）
     *
     * @param id 其他收支单ID
     */
    void deleteDraft(Long id);
}
