package org.erp.trade.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.erp.trade.domain.bo.ErpPurchaseReturnBo;
import org.erp.trade.domain.vo.ErpPurchaseReturnVo;

import java.util.List;

/**
 * 采购退货服务（DOC/24 §6.5，对称销售退货）
 */
public interface IErpPurchaseReturnService {

    TableDataInfo<ErpPurchaseReturnVo> selectPage(String keyword, Long supplierId, String status, PageQuery pageQuery);

    List<ErpPurchaseReturnVo> returnableOrders(Long supplierId);

    List<ErpPurchaseReturnVo.ItemVo> originalItems(Long purchaseId);

    ErpPurchaseReturnVo selectById(Long id);

    ErpPurchaseReturnVo insert(ErpPurchaseReturnBo bo);

    ErpPurchaseReturnVo update(Long id, ErpPurchaseReturnBo bo);

    void confirm(Long id);

    void withdraw(Long id);

    void voidBill(Long id);

    /**
     * 删除草稿采购退货单：仅 DRAFT 可删，主表与明细均逻辑删除，单号不回收。
     *
     * @param id 采购退货单ID
     */
    void deleteDraft(Long id);
}
