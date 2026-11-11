package org.erp.print.service.impl;

import org.erp.fin.mapper.FinArApMapper;
import org.erp.fin.mapper.FinReceiptMapper;
import org.erp.master.domain.ErpCustomer;
import org.erp.master.domain.ErpProduct;
import org.erp.master.mapper.ErpCustomerMapper;
import org.erp.master.mapper.ErpProductMapper;
import org.erp.print.domain.PrintTemplate;
import org.erp.print.service.IPrintTemplateService;
import org.erp.trade.domain.ErpSale;
import org.erp.trade.domain.ErpSaleItem;
import org.erp.trade.mapper.ErpSaleItemMapper;
import org.erp.trade.mapper.ErpSaleMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 对账单数量列精度测试：数量必须按商品数量精度展示（整数商品 1、两位小数商品 2.50），
 * 商品已删除时回落 3 位保真。
 */
@Tag("dev")
class PrintStatementQtyScaleTest {

    @Test
    @DisplayName("数量按商品精度展示")
    void rendersQtyByProductScale() {
        IPrintTemplateService templateService = mock(IPrintTemplateService.class);
        ErpSaleMapper saleMapper = mock(ErpSaleMapper.class);
        ErpSaleItemMapper saleItemMapper = mock(ErpSaleItemMapper.class);
        FinArApMapper arApMapper = mock(FinArApMapper.class);
        FinReceiptMapper receiptMapper = mock(FinReceiptMapper.class);
        ErpCustomerMapper customerMapper = mock(ErpCustomerMapper.class);
        ErpProductMapper productMapper = mock(ErpProductMapper.class);

        ErpSale sale = new ErpSale();
        sale.setId(1L);
        sale.setCustomerId(9L);
        sale.setNo("XS202611070001");
        sale.setBizDate(new Date());
        sale.setStatus("CONFIRMED");
        sale.setTotalAmount(new BigDecimal("1029.05"));
        when(saleMapper.selectList(any())).thenReturn(List.of(sale));
        when(saleItemMapper.selectList(any())).thenReturn(List.of(
            saleItem(1L, new BigDecimal("1.000")), saleItem(2L, new BigDecimal("2.5")), saleItem(3L, new BigDecimal("1.234"))));
        when(productMapper.selectById(1L)).thenReturn(product("压射室", 0));
        when(productMapper.selectById(2L)).thenReturn(product("压射头", 2));
        // 商品 3 已删除：精度未知，回落 3 位保真
        when(arApMapper.selectList(any())).thenReturn(List.of());
        when(receiptMapper.selectList(any())).thenReturn(List.of());
        when(customerMapper.selectById(9L)).thenReturn(new ErpCustomer());
        when(templateService.resolve(any(), any(), any())).thenReturn((PrintTemplate) null);

        PrintServiceImpl service = new PrintServiceImpl(templateService, saleMapper, receiptMapper, null, null, null,
            arApMapper, null, customerMapper, null, productMapper, saleItemMapper, null);
        String html = service.renderStatement(9L, "2026-11", null);

        assertTrue(html.contains(">1<"), "整数精度商品应显示 1");
        assertTrue(html.contains(">2.50<"), "两位精度商品应显示 2.50");
        assertTrue(html.contains(">1.234<"), "商品已删除时应回落三位保真");
        assertFalse(html.contains("1.000"), "整数精度商品不应显示 1.000");
    }

    private static ErpSaleItem saleItem(Long productId, BigDecimal qty) {
        ErpSaleItem item = new ErpSaleItem();
        item.setProductId(productId);
        item.setQty(qty);
        item.setPrice(new BigDecimal("100.00"));
        item.setAmount(new BigDecimal("100.00"));
        return item;
    }

    private static ErpProduct product(String name, Integer qtyScale) {
        ErpProduct product = new ErpProduct();
        product.setName(name);
        product.setQtyScale(qtyScale);
        return product;
    }
}
