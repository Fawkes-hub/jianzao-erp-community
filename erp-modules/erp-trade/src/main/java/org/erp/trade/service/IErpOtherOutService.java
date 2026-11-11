package org.erp.trade.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.erp.trade.domain.bo.ErpOtherOutBo;
import org.erp.trade.domain.vo.ErpOtherOutVo;

/**
 * 其他出库服务
 *
 * 确认：扣库存（OTHER_OUT 流水）+ 快照成本 + 写 fin_cost_ledger（OTHER_OUT）；
 * 不产生应收（S3/R17）；撤回走库存 reverse + 成本台账 REVERSED。
 */
public interface IErpOtherOutService {

    TableDataInfo<ErpOtherOutVo> selectPage(String keyword, String costCategory, String status, PageQuery pageQuery);

    ErpOtherOutVo selectById(Long id);

    ErpOtherOutVo insert(ErpOtherOutBo bo);

    ErpOtherOutVo update(Long id, ErpOtherOutBo bo);

    void confirm(Long id);

    void withdraw(Long id);

    void voidBill(Long id);

    /**
     * 删除草稿其他出库单：仅 DRAFT 可删，主表与明细均逻辑删除，单号不回收。
     *
     * @param id 其他出库单ID
     */
    void deleteDraft(Long id);
}
