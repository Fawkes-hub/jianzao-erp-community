package org.erp.trade.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.erp.trade.domain.ErpPurchaseReturn;
import org.erp.trade.domain.vo.ErpPurchaseReturnVo;

/**
 * 采购退货单 Mapper
 */
public interface ErpPurchaseReturnMapper extends BaseMapperPlus<ErpPurchaseReturn, ErpPurchaseReturnVo> {

    /**
     * 按主键锁定采购退货单头（SELECT ... FOR UPDATE）。
     *
     * <p>确认/撤回/作废的第一把锁必须是本方法，之后才锁原执行单头与订单头（DOC/31 §6.8.1、§6.8.2 防线 2）。
     *
     * @param id 退货单ID
     * @return 锁定到的退货单；不存在或已逻辑删除返回 null
     */
    @Select("select * from biz_purchase_return where id = #{id} and del_flag = '0' for update")
    ErpPurchaseReturn lockById(@Param("id") Long id);

    /**
     * 锁定原采购单头行（对称销售退货，DOC/24 §6.2）。
     */
    @Select("select id from biz_purchase where id = #{id} for update")
    Long lockPurchaseForReturn(@Param("id") Long purchaseId);
}
