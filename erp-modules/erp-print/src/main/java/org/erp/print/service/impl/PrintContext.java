package org.erp.print.service.impl;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 打印渲染只读上下文（DOC/14 §4 变量字典）
 *
 * 全部字段在 Java 侧预计算，Thymeleaf 模板只做展示（th:text 转义）。
 */
@Data
public class PrintContext {

    private Tenant tenant = new Tenant();

    private Doc doc = new Doc();

    private Partner partner = new Partner();

    private List<Item> items = new ArrayList<>();

    private Totals totals = new Totals();

    /**
     * 仅对账单场景
     */
    private Statement statement;

    /**
     * 仅送货单 showProfit=true
     */
    private Profit profit;

    private Signatures signatures = new Signatures();

    /**
     * 打印日期 yyyy-MM-dd
     */
    private String printDate;

    private Integer copies = 1;

    private Boolean showProfit = false;

    @Data
    public static class Tenant {
        private String name;
        private String title;
        private String address;
        private String phone;
        private String logoUrl;
    }

    @Data
    public static class Doc {
        private String no;
        private Date date;
        private String type;
        private String remark;
        private String status;
        /**
         * 收付款单：收款账户
         */
        private String account;
    }

    @Data
    public static class Partner {
        private String name;
        private String contact;
        private String phone;
        private String address;
    }

    @Data
    public static class Item {
        private Integer index;
        private String name;
        private String spec;
        private String unit;
        private BigDecimal qty;
        private BigDecimal price;
        private BigDecimal amount;
        private String taxType;
        private String remark;
        /**
         * 收付款单核销明细：关联单号/日期
         */
        private String no;
        private Date date;
    }

    @Data
    public static class Totals {
        private BigDecimal qtyTotal;
        private BigDecimal amountTotal;
        /**
         * 人民币大写
         */
        private String amountUpper;
    }

    @Data
    public static class Statement {
        private String month;
        private BigDecimal opening;
        private BigDecimal salesAmount;
        private BigDecimal receiptAmount;
        private BigDecimal closing;
        private List<StatementItem> items = new ArrayList<>();
    }

    @Data
    public static class StatementItem {
        private Date date;
        private String no;
        private String content;
        /**
         * 销售行的商品名称（收款行为空）
         */
        private String productName;
        private BigDecimal qty;
        /**
         * 数量展示文本：按商品数量精度（md_product.qty_scale）预格式化，模板直接输出；
         * 商品已删除或精度缺失时回落 3 位保真，收款行与整单汇总行为空
         */
        private String qtyText;
        /**
         * 销售明细行单价（收款行、整单汇总行为空）
         */
        private BigDecimal price;
        private BigDecimal amount;
        private BigDecimal settled;
        private BigDecimal unsettled;
    }

    @Data
    public static class Profit {
        private BigDecimal costAmount;
        private BigDecimal grossProfit;
        private BigDecimal grossMargin;
    }

    @Data
    public static class Signatures {
        private String maker;
        private String warehouse;
        private String customer;
    }
}
