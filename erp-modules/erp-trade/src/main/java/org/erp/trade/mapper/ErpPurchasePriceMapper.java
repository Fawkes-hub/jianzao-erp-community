package org.erp.trade.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.erp.trade.domain.vo.ErpPriceHistoryVo;

import java.util.List;

/**
 * 采购历史价 Mapper（DOC/26 §7）
 */
public interface ErpPurchasePriceMapper {

    /**
     * 该商品的全部历史采购价（不限供应商），按业务日期倒序取最近 limit 条（DOC/26 §7）。
     * 只取已确认且未删除的单据，退货单不参与；tenant_id 由多租户插件注入。
     *
     * @param productId  产品ID
     * @param limit      返回条数
     * @return 历史采购记录（日期、单号、供应商名、单价、数量、金额）
     */
    @Select("""
        select p.biz_date as bizDate,
               p.no       as orderNo,
               s.name     as partyName,
               i.price    as price,
               i.qty      as qty,
               i.amount   as amount
        from biz_purchase_item i
        join biz_purchase p on p.id = i.order_id
        left join md_supplier s on s.id = p.supplier_id
        where i.product_id = #{productId}
          and p.status = 'CONFIRMED'
          and p.del_flag = '0'
          and i.del_flag = '0'
        order by p.biz_date desc, p.id desc, i.id desc
        limit #{limit}
        """)
    List<ErpPriceHistoryVo> selectPriceHistory(@Param("productId") Long productId,
                                               @Param("limit") int limit);
}
