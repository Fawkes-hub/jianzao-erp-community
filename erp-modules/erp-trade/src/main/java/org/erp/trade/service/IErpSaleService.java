package org.erp.trade.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.erp.trade.domain.bo.ErpSaleBo;
import org.erp.trade.domain.vo.ErpSaleVo;

/**
 * 销售出库服务接口
 */
public interface IErpSaleService {

    TableDataInfo<ErpSaleVo> selectPage(String keyword, Long customerId, String status, PageQuery pageQuery);

    ErpSaleVo selectById(Long id);

    ErpSaleVo insert(ErpSaleBo bo);

    ErpSaleVo update(Long id, ErpSaleBo bo);

    /**
     * 确认销售单：写成本快照与应收，costWarnings 回传成本为 0 的商品名（仅提示不阻断）
     */
    ErpSaleVo confirm(Long id);

    void withdraw(Long id);

    void voidBill(Long id);

    /**
     * 删除草稿销售单：仅 DRAFT 可删，主表与明细均逻辑删除，单号不回收。
     *
     * @param id 销售单ID
     */
    void deleteDraft(Long id);
}
