package org.erp.stock.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.erp.stock.domain.StOpening;
import org.erp.stock.domain.vo.StOpeningVo;

import java.math.BigDecimal;

/**
 * 期初库存 Mapper
 */
public interface StOpeningMapper extends BaseMapperPlus<StOpening, StOpeningVo> {

    /**
     * 按 product_id + warehouse_id 查询已逻辑删除的期初行 ID。
     *
     * 唯一键 uk_st_opening 含 (tenant_id, product_id, warehouse_id) 不含 del_flag，
     * 逻辑删除后该唯一键仍被占用，重新录入同一产品+仓库必须复活原行而不是插入新行。
     * tenant_id 由多租户插件注入，不手写条件。
     *
     * @param productId   产品ID
     * @param warehouseId 仓库ID
     * @return 已删除行ID，不存在时返回 null
     */
    @Select("select id from st_opening where product_id = #{productId} and warehouse_id = #{warehouseId} and del_flag = '1' limit 1")
    Long selectDeletedId(@Param("productId") Long productId, @Param("warehouseId") Long warehouseId);

    /**
     * 复活已逻辑删除的期初行：del_flag 置回 0 并写入新值。
     *
     * 唯一键 uk_st_opening 不含 del_flag，删除后重建只能复用原行；
     * MyBatis-Plus 的 update 走逻辑删除过滤（自动附加 del_flag='0'），
     * 故这里用显式 update 语句复活。
     *
     * @param id         期初行ID
     * @param qty        期初数量
     * @param unitCost   期初成本
     * @return 影响行数
     */
    @Update("""
        update st_opening
        set del_flag = '0',
            qty = #{qty},
            unit_cost = #{unitCost},
            status = 'DRAFT'
        where id = #{id}
          and del_flag = '1'
        """)
    int reviveDeleted(@Param("id") Long id,
                      @Param("qty") BigDecimal qty,
                      @Param("unitCost") BigDecimal unitCost);
}
