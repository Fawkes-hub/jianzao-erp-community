package org.erp.trade.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.erp.trade.domain.vo.ErpPriceHistoryVo;
import org.erp.trade.domain.vo.ErpSaleLastPriceVo;

import java.util.List;

/**
 * 销售最近成交价 Mapper（DOC/25 §3.7）
 */
public interface ErpSalePriceMapper {

    /**
     * 最近一次已确认成交价：scope=CUSTOMER 限客户，MARKET 兜底全市场。
     * tenant_id 由多租户插件注入，不手写条件；取 biz_date+id 倒序第一条。
     *
     * @param customerId 客户ID（MARKET 时忽略）
     * @param productId  产品ID
     * @param scope      CUSTOMER / MARKET
     */
    @Select("""
        select i.price  as price,
               s.biz_date as bizDate,
               s.no     as saleNo,
               #{scope} as scope
        from biz_sale_item i
        join biz_sale s on s.id = i.order_id
        where i.product_id = #{productId}
          and s.status = 'CONFIRMED'
          and s.del_flag = '0'
          and (#{scope} = 'MARKET' or s.customer_id = #{customerId})
        order by s.biz_date desc, s.id desc
        limit 1
        """)
    ErpSaleLastPriceVo selectLastPrice(@Param("customerId") Long customerId,
                                       @Param("productId") Long productId,
                                       @Param("scope") String scope);

    /**
     * 该商品的全部历史成交价（不限客户），按业务日期倒序取最近 limit 条（DOC/26 §7）。
     * 只取已确认且未删除的单据，退货单不参与；tenant_id 由多租户插件注入。
     *
     * @param productId  产品ID
     * @param limit      返回条数
     * @return 历史成交记录（日期、单号、客户名、单价、数量、金额）
     */
    @Select("""
        select s.biz_date as bizDate,
               s.no       as orderNo,
               c.name     as partyName,
               i.price    as price,
               i.qty      as qty,
               i.amount   as amount
        from biz_sale_item i
        join biz_sale s on s.id = i.order_id
        left join md_customer c on c.id = s.customer_id
        where i.product_id = #{productId}
          and s.status = 'CONFIRMED'
          and s.del_flag = '0'
          and i.del_flag = '0'
        order by s.biz_date desc, s.id desc, i.id desc
        limit #{limit}
        """)
    List<ErpPriceHistoryVo> selectPriceHistory(@Param("productId") Long productId,
                                               @Param("limit") int limit);
}
