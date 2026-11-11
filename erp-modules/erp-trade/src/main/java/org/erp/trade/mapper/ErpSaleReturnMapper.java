package org.erp.trade.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.erp.trade.domain.ErpSaleReturn;
import org.erp.trade.domain.vo.ErpSaleReturnVo;

/**
 * 销售退货单 Mapper
 */
public interface ErpSaleReturnMapper extends BaseMapperPlus<ErpSaleReturn, ErpSaleReturnVo> {

    /**
     * 按主键锁定销售退货单头（SELECT ... FOR UPDATE）。
     *
     * <p>确认/撤回/作废的第一把锁必须是本方法，之后才锁原执行单头与订单头（DOC/31 §6.8.1、§6.8.2 防线 2）。
     *
     * @param id 退货单ID
     * @return 锁定到的退货单；不存在或已逻辑删除返回 null
     */
    @Select("select * from biz_sale_return where id = #{id} and del_flag = '0' for update")
    ErpSaleReturn lockById(@Param("id") Long id);

    /**
     * 锁定原销售单头行（确认事务内串行化同原单的并发退货，DOC/24 §6.2）。
     */
    @Select("select id from biz_sale where id = #{id} for update")
    Long lockSaleForReturn(@Param("id") Long saleId);
}
