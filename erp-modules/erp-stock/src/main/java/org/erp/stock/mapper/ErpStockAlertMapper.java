package org.erp.stock.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.erp.stock.domain.vo.ErpStockAlertVo;

import java.util.List;

/**
 * 库存预警查询 Mapper（DOC/25 §3.2）
 */
public interface ErpStockAlertMapper {

    /**
     * 预警明细：md_product LEFT JOIN st_stock 合计（IFNULL 0），
     * LEFT JOIN 是关键——纯 UPDATE 路径不建 st_stock 行，从未动账产品必须按 0 参与比对进 LOW。
     * tenant_id 由多租户插件注入，不手写条件。
     *
     * @param alertType LOW=低于下限 / HIGH=高于上限 / null=全部
     */
    @Select("""
        <script>
        select p.id           as productId,
               p.code         as productCode,
               p.name         as productName,
               p.spec         as spec,
               p.unit         as unit,
               p.min_stock    as minStock,
               p.max_stock    as maxStock,
               ifnull(s.total_qty, 0) as totalQty,
               <choose>
                   <when test="alertType == 'HIGH'">ifnull(s.total_qty, 0) - p.max_stock</when>
                   <otherwise>ifnull(s.total_qty, 0) - p.min_stock</otherwise>
               </choose>      as gap,
               <choose>
                   <when test="alertType == 'HIGH'">'HIGH'</when>
                   <otherwise>'LOW'</otherwise>
               </choose>      as alertType
        from md_product p
        left join (select product_id, sum(qty) as total_qty
                   from st_stock group by product_id) s on s.product_id = p.id
        where p.del_flag = '0'
          and p.status = 1
          and (p.min_stock is not null or p.max_stock is not null)
          <choose>
              <when test="alertType == 'LOW'">
                  and p.min_stock is not null
                  and ifnull(s.total_qty, 0) &lt; p.min_stock
              </when>
              <when test="alertType == 'HIGH'">
                  and p.max_stock is not null
                  and ifnull(s.total_qty, 0) &gt; p.max_stock
              </when>
              <otherwise>
                  and (
                      (p.min_stock is not null and ifnull(s.total_qty, 0) &lt; p.min_stock)
                      or (p.max_stock is not null and ifnull(s.total_qty, 0) &gt; p.max_stock)
                  )
              </otherwise>
          </choose>
        <if test="productId != null"> and p.id = #{productId} </if>
        order by abs(ifnull(s.total_qty, 0) - ifnull(p.min_stock, p.max_stock)) desc
        </script>
        """)
    List<ErpStockAlertVo> selectAlerts(@Param("alertType") String alertType, @Param("productId") Long productId);
}
