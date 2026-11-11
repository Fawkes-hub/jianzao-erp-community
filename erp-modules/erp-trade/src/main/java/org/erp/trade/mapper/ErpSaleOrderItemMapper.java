package org.erp.trade.mapper;

import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.erp.trade.domain.ErpSaleOrderItem;
import org.erp.trade.domain.vo.ErpSaleOrderVo;

/**
 * 销售订单行 Mapper
 *
 * <p>订单行的批量锁定读（ORDER BY id ASC）按 DOC/31 §5.4 落在 ErpSaleOrderMapper 上，
 * 与采购订单同一口径；本 Mapper 只承担常规增删改查。
 */
public interface ErpSaleOrderItemMapper extends BaseMapperPlus<ErpSaleOrderItem, ErpSaleOrderVo> {
}
