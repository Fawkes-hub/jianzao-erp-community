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
     * 收款单渲染。
     */
    String renderReceipt(Long receiptId, Long templateId);

    /**
     * 付款单渲染。
     */
    String renderPayment(Long paymentId, Long templateId);

    /**
     * 模板编辑预览：用示例数据渲染编辑中的模板内容。
     */
    String preview(String scene, String content, Long customerId, boolean showProfit);
}
