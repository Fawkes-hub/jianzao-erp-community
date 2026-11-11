package org.erp.trade.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.erp.trade.domain.bo.ErpPurchaseBo;
import org.erp.trade.domain.vo.ErpPurchaseVo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 采购入库服务接口
 */
public interface IErpPurchaseService {

    TableDataInfo<ErpPurchaseVo> selectPage(String keyword, Long supplierId, String status, PageQuery pageQuery);

    ErpPurchaseVo selectById(Long id);

    ErpPurchaseVo insert(ErpPurchaseBo bo);

    ErpPurchaseVo update(Long id, ErpPurchaseBo bo);

    void confirm(Long id);

    void withdraw(Long id);

    void voidBill(Long id);

    /**
     * 删除草稿采购单：仅 DRAFT 可删，主表与明细均逻辑删除，单号不回收。
     *
     * @param id 采购单ID
     */
    void deleteDraft(Long id);

    /**
     * 运杂费分摊预览（不落库）
     *
     * @return 每行 [productId, freightAmount, freightPerUnit, costPrice]
     */
    List<ErpPurchaseVo.ItemVo> previewAllocation(ErpPurchaseBo bo);

    /**
     * 计算行成本单价（11 §8.2 规则）
     */
    BigDecimal lineCostPrice(BigDecimal price, String taxType, BigDecimal taxRate, BigDecimal freightPerUnit);
}
