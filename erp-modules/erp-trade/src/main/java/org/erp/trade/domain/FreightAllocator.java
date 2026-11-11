package org.erp.trade.domain;

import org.dromara.common.core.exception.ServiceException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 运杂费分摊计算（采购入库与销售出库共用，DOC/26 §6.5）。
 */
public final class FreightAllocator {

    private FreightAllocator() {
    }

    /**
     * 按数量分摊运杂费：行分摊额 = 总额 × 行数量 / Σ数量，尾差计入最后一行。
     *
     * @param qtys         各行数量，顺序与明细一致
     * @param freightTotal 运杂费总额，null 按 0 处理
     * @return 与入参顺序一致的行分摊额，两位小数
     */
    public static List<BigDecimal> byQty(List<BigDecimal> qtys, BigDecimal freightTotal) {
        BigDecimal total = freightTotal == null ? BigDecimal.ZERO : freightTotal;
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return qtys.stream().map(q -> BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)).toList();
        }
        BigDecimal qtyTotal = qtys.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (qtyTotal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException("数量合计必须大于0");
        }
        List<BigDecimal> result = new ArrayList<>();
        BigDecimal allocated = BigDecimal.ZERO;
        for (int i = 0; i < qtys.size(); i++) {
            BigDecimal freight;
            if (i == qtys.size() - 1) {
                freight = total.subtract(allocated).setScale(2, RoundingMode.HALF_UP);
            } else {
                freight = total.multiply(qtys.get(i)).divide(qtyTotal, 2, RoundingMode.HALF_UP);
                allocated = allocated.add(freight);
            }
            result.add(freight);
        }
        return result;
    }
}
