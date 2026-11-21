package org.erp.print.service;

/**
 * 打印渲染服务（DOC/12 §9、DOC/14）
 *
 * 渲染返回完整 HTML（含内联 CSS），前端 iframe 加载后 window.print()。
 */
public interface IPrintService {

    /**
     * 送货单渲染。showProfit=true 时附带成本/毛利（内部查看）；copies 为打印份数（联单）。
     */
    String renderSaleDelivery(Long saleId, Long templateId, boolean showProfit, int copies);

    /**
     * 对账单渲染（D3：期初 + 本期销售按单据日期 − 本期收款按收款日期 = 期末）。
     */
    String renderStatement(Long customerId, String month, Long templateId);

    /**
     * 对账单渲染（支持自定义闭区间）。
     *
     * startDate、endDate 同时给出时按区间对账并忽略 month；均为 yyyy-MM-dd，结束日含当日。
     * 只传其中一个、格式非法、开始日晚于结束日或跨度超限时抛 ServiceException。
     *
     * @param customerId 客户ID
     * @param month      账期 yyyy-MM，区间模式可空
     * @param startDate  区间开始日（含），可空
     * @param endDate    区间结束日（含），可空
     * @param templateId 指定模板ID，可空
     * @return 渲染后的完整 HTML
     */
    String renderStatement(Long customerId, String month, String startDate, String endDate, Long templateId);

    /**
     * 收款单渲染。
     */
    String renderReceipt(Long receiptId, Long templateId);

    /**
     * 付款单渲染。
     */
    String renderPayment(Long paymentId, Long templateId);

    /**
     * 模板编辑预览：用示例数据渲染编辑中的模板内容。
     * paper 为模板纸张字段（A4/A5/宽x高 mm/AUTO），按同样规则注入 @page，预览即所得。
     */
    String preview(String scene, String content, Long customerId, boolean showProfit, String paper);
}
