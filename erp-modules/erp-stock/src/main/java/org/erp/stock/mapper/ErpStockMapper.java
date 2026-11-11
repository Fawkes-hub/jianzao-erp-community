package org.erp.stock.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.erp.stock.domain.ErpStock;
import org.erp.stock.domain.vo.ErpStockVo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 库存快照 Mapper
 */
public interface ErpStockMapper extends BaseMapperPlus<ErpStock, ErpStockVo> {

    /**
     * 条件扣减：qty >= 扣减量才更新，影响行数 0 即库存不足（防超卖）。
     * tenant_id 由多租户插件注入。
     */
    @Update("""
        update st_stock
        set qty = qty - #{qty}
        where product_id = #{productId}
          and warehouse_id = #{warehouseId}
          and qty >= #{qty}
          and del_flag = '0'
        """)
    int deductQty(@Param("productId") Long productId,
                  @Param("warehouseId") Long warehouseId,
                  @Param("qty") BigDecimal qty);

    /**
     * 无条件回补（入库/反向流水）。
     */
    @Update("""
        update st_stock
        set qty = qty + #{qty}
        where product_id = #{productId}
          and warehouse_id = #{warehouseId}
          and del_flag = '0'
        """)
    int addQty(@Param("productId") Long productId,
               @Param("warehouseId") Long warehouseId,
               @Param("qty") BigDecimal qty);

    /**
     * 锁定库存行（事务内 SELECT ... FOR UPDATE），保证读-算-写原子。
     */
    @org.apache.ibatis.annotations.Select("""
        select * from st_stock
        where product_id = #{productId}
          and warehouse_id = #{warehouseId}
          and del_flag = '0'
        for update
        """)
    ErpStock selectForUpdate(@Param("productId") Long productId, @Param("warehouseId") Long warehouseId);

    /**
     * 按商品ID集合批量锁定库存行（事务内 SELECT ... FOR UPDATE），SQL 内固定 product_id、warehouse_id 升序。
     * 加锁顺序由 SQL 决定，调用方传入顺序不影响锁序（DOC/31 §6.8.1）。
     *
     * @param productIds 商品ID集合，调用方需保证非空（空集合不生成合法 SQL）
     * @return 锁定到的库存行，按 product_id、warehouse_id 升序
     */
    @org.apache.ibatis.annotations.Select("""
        <script>
        select * from st_stock
        where product_id in
        <foreach collection="productIds" item="productId" open="(" separator="," close=")">#{productId}</foreach>
          and del_flag = '0'
        order by product_id, warehouse_id
        for update
        </script>
        """)
    List<ErpStock> selectByProductIdsForUpdate(@Param("productIds") List<Long> productIds);

    /**
     * 更新移动加权平均成本。
     */
    @Update("""
        update st_stock
        set avg_cost = #{avgCost}
        where product_id = #{productId}
          and warehouse_id = #{warehouseId}
          and del_flag = '0'
        """)
    int updateAvgCost(@Param("productId") Long productId,
                      @Param("warehouseId") Long warehouseId,
                      @Param("avgCost") BigDecimal avgCost);
}
