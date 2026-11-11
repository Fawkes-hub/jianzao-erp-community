package org.erp.master.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.erp.master.domain.bo.ErpProductBo;
import org.erp.master.domain.bo.ErpRecipeItemBo;
import org.erp.master.domain.vo.ErpProductVo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 产品/配比 服务接口
 */
public interface IErpProductService {

    /**
     * 商品分页列表（含分类/供应商名称）
     *
     * @param keyword    编码/名称/规格模糊匹配
     * @param type       成品/原料，null=全部
     * @param status     状态，null=全部
     * @param categoryId 分类ID，null=全部
     * @param supplierId 供应商ID，null=全部
     * @param pageQuery  分页参数
     * @return 分页结果
     */
    TableDataInfo<ErpProductVo> selectPage(String keyword, String type, Integer status, Long categoryId, Long supplierId, PageQuery pageQuery);

    /**
     * 商品全量列表（含分类/供应商名称），供选品器与下拉使用
     *
     * @param keyword    编码/名称/规格模糊匹配
     * @param type       成品/原料，null=全部
     * @param categoryId 分类ID，null=全部
     * @return 商品列表
     */
    List<ErpProductVo> selectList(String keyword, String type, Long categoryId);


    ErpProductVo selectById(Long id);

    /**
     * 新增商品：编码留空按名称自动生成，含编码唯一与库存上下限校验。
     * 现录现卖建品也走此入口，保证与商品资料同一套编码规则和校验。
     *
     * @param bo 商品信息
     * @return 新建商品ID
     */
    Long insert(ErpProductBo bo);

    void update(ErpProductBo bo);

    void deleteByIds(List<Long> ids);

    List<ErpRecipeItemBo> getRecipe(Long productId);

    void saveRecipe(Long productId, List<ErpRecipeItemBo> items);

    /**
     * 回写最近入库成本（D4/D15）：采购、生产、期初、其他入库确认时统一调用
     */
    void updateCostPrice(Long productId, BigDecimal costPrice);
}
