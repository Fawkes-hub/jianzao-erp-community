package org.erp.trade.mapper;

import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.erp.trade.domain.ErpPurchaseOrderItem;
import org.erp.trade.domain.vo.ErpPurchaseOrderVo;

/**
 * 采购订单行 Mapper
 *
 * <p>订单行的批量锁定读（ORDER BY id ASC）按 DOC/31 §5.4 落在 ErpPurchaseOrderMapper 上，
 * 与销售订单同一口径；本 Mapper 只承担常规增删改查。
 */
public interface ErpPurchaseOrderItemMapper extends BaseMapperPlus<ErpPurchaseOrderItem, ErpPurchaseOrderVo> {
}
