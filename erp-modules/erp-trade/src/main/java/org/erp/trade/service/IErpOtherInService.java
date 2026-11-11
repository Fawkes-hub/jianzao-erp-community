package org.erp.trade.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.erp.trade.domain.bo.ErpOtherInBo;
import org.erp.trade.domain.vo.ErpOtherInVo;

/**
 * 其他入库服务
 *
 * 确认：按录入单价入库（OTHER_IN 流水）+ 移动加权更新成本；
 * 不产生应收应付、不写成本台账；撤回走库存 reverse + 重放重算 avg_cost。
 */
public interface IErpOtherInService {

    TableDataInfo<ErpOtherInVo> selectPage(String keyword, String reason, String status, PageQuery pageQuery);

    ErpOtherInVo selectById(Long id);

    ErpOtherInVo insert(ErpOtherInBo bo);

    ErpOtherInVo update(Long id, ErpOtherInBo bo);

    void confirm(Long id);

    void withdraw(Long id);

    void voidBill(Long id);

    /**
     * 删除草稿其他入库单：仅 DRAFT 可删，主表与明细均逻辑删除，单号不回收。
     *
     * @param id 其他入库单ID
     */
    void deleteDraft(Long id);
}
