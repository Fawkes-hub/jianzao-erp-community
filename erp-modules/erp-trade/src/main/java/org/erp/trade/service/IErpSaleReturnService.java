package org.erp.trade.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.erp.trade.domain.bo.ErpSaleReturnBo;
import org.erp.trade.domain.vo.ErpSaleReturnVo;

import java.util.List;

/**
 * 销售退货服务（DOC/24）
 */
public interface IErpSaleReturnService {

    TableDataInfo<ErpSaleReturnVo> selectPage(String keyword, Long customerId, String status, PageQuery pageQuery);

    /**
     * 可退原单候选：该客户 CONFIRMED 销售单。
     */
    List<ErpSaleReturnVo> returnableOrders(Long customerId);

    /**
     * 原单明细（含原行数量/已退量/可退量，只读展示）。
     */
    List<ErpSaleReturnVo.ItemVo> originalItems(Long saleId);

    ErpSaleReturnVo selectById(Long id);

    ErpSaleReturnVo insert(ErpSaleReturnBo bo);

    ErpSaleReturnVo update(Long id, ErpSaleReturnBo bo);

    void confirm(Long id);

    void withdraw(Long id);

    void voidBill(Long id);

    /**
     * 删除草稿销售退货单：仅 DRAFT 可删，主表与明细均逻辑删除，单号不回收。
     *
     * @param id 销售退货单ID
     */
    void deleteDraft(Long id);
}
