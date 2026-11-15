package org.erp.trade.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 工作台库存预警卡片 VO（DOC/25 §3.3）
 */
@Data
public class DashboardStockAlertVo {

    /**
     * 低于下限产品数
     */
    private Long lowCount;

    /**
     * 高于上限产品数
     */
    private Long highCount;

    /**
     * 缺口最大的前 5 个低限预警
     */
    private List<Item> lowTop5 = new ArrayList<>();

    /**
     * 缺口最大的前 5 个高限预警
     */
    private List<Item> highTop5 = new ArrayList<>();

    @Data
    public static class Item {
        private Long productId;
        private String productName;
        private BigDecimal totalQty;
        private BigDecimal gap;
    }
}
