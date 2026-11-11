package org.erp.trade.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.erp.trade.domain.ErpPurchase;
import org.erp.trade.domain.vo.ErpPurchaseVo;

/**
 * 采购单 Mapper
 */
public interface ErpPurchaseMapper extends BaseMapperPlus<ErpPurchase, ErpPurchaseVo> {

    /**
     * 按主键锁定采购单头（SELECT ... FOR UPDATE）。
     *
     * <p>确认/撤回/作废的第一把锁必须是本方法：加锁顺序为 单据头 → 订单头 → 订单行 → 台账 → 库存
     * （DOC/31 §6.8.1），也保证本事务后续一致性读能看到并发事务已提交的执行单。
     *
     * @param id 采购单ID
     * @return 锁定到的采购单；不存在或已逻辑删除返回 null
     */
    @Select("select * from biz_purchase where id = #{id} and del_flag = '0' for update")
    ErpPurchase lockById(@Param("id") Long id);
}
