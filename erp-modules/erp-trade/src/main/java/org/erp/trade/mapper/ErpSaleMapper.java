package org.erp.trade.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.erp.trade.domain.ErpSale;
import org.erp.trade.domain.vo.ErpSaleVo;

/**
 * 销售单 Mapper
 */
public interface ErpSaleMapper extends BaseMapperPlus<ErpSale, ErpSaleVo> {

    /**
     * 按主键锁定销售单头（SELECT ... FOR UPDATE）。
     *
     * <p>确认/撤回/作废的第一把锁必须是本方法：加锁顺序为 单据头 → 订单头 → 订单行 → 台账 → 库存
     * （DOC/31 §6.8.1），也保证本事务后续一致性读能看到并发事务已提交的执行单。
     *
     * @param id 销售单ID
     * @return 锁定到的销售单；不存在或已逻辑删除返回 null
     */
    @Select("select * from biz_sale where id = #{id} and del_flag = '0' for update")
    ErpSale lockById(@Param("id") Long id);
}
