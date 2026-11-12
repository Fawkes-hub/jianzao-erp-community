package org.erp.trade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.erp.fin.domain.FinCostLedger;
import org.erp.fin.mapper.FinCostLedgerMapper;
import org.erp.stock.service.IInventoryService;
import org.erp.trade.domain.ErpSale;
import org.erp.trade.domain.ErpSaleItem;
import org.erp.trade.domain.ErpSaleReturn;
import org.erp.trade.domain.vo.FinCostReportVo;
import org.erp.trade.mapper.ErpSaleItemMapper;
import org.erp.trade.mapper.ErpSaleMapper;
import org.erp.trade.mapper.ErpSaleReturnMapper;
import org.erp.trade.service.IFinCostReportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 综合成本/利润报表实现（R20）
 *
 * 口径（DOC/02 R20、DOC/11 §8）：
 * - 综合成本 = 销售成本（biz_sale cost_amount）+ 其他出库成本 + 计入成本的其他支出；
 * - 生产材料成本仅作参考展示：已通过成品成本随销售出库计入，不再重复计入综合成本；
 * - 销售收入 = biz_sale status=CONFIRMED 按月 total_amount 合计；
 * - 利润 = 销售收入 − 综合成本。
 */
@RequiredArgsConstructor
@Service
public class FinCostReportServiceImpl implements IFinCostReportService {

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final FinCostLedgerMapper costLedgerMapper;
    private final ErpSaleMapper saleMapper;
    private final ErpSaleReturnMapper saleReturnMapper;
    private final ErpSaleItemMapper saleItemMapper;
    private final IInventoryService inventoryService;

    @Override
    @Transactional(readOnly = true)
    public FinCostReportVo report(String month) {
        String m = (month == null || month.isBlank())
            ? LocalDate.now().format(MONTH_FMT) : month;
        LocalDate first;
        try {
            first = LocalDate.parse(m + "-01");
        } catch (Exception e) {
            throw new ServiceException("账期格式必须是 yyyy-MM");
        }
        LocalDate next = first.plusMonths(1);
        Date from = Date.from(first.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date to = Date.from(next.atStartOfDay(ZoneId.systemDefault()).toInstant());

        // 生产材料成本只作参考：材料成本已进入成品成本，销售时随 cost_amount 计入综合成本
        BigDecimal material = sumLedger("PRODUCTION_MATERIAL", from, to);
        BigDecimal otherOut = sumLedger("OTHER_OUT", from, to);
        BigDecimal otherExpense = sumLedger("OTHER_EXPENSE", from, to);

        List<ErpSale> sales = saleMapper.selectList(new LambdaQueryWrapper<ErpSale>()
            .select(ErpSale::getId, ErpSale::getTotalAmount, ErpSale::getCostAmount)
            .eq(ErpSale::getStatus, "CONFIRMED")
            .ge(ErpSale::getBizDate, from)
            .lt(ErpSale::getBizDate, to));
        BigDecimal salesTotal = BigDecimal.ZERO;
        BigDecimal salesCost = BigDecimal.ZERO;
        List<Long> saleIds = new ArrayList<>();
        for (ErpSale sale : sales) {
            salesTotal = salesTotal.add(sale.getTotalAmount() == null ? BigDecimal.ZERO : sale.getTotalAmount());
            salesCost = salesCost.add(sale.getCostAmount() == null ? BigDecimal.ZERO : sale.getCostAmount());
            if (sale.getId() != null) {
                saleIds.add(sale.getId());
            }
        }
        // 成本缺失标识（DOC/26 §2.4 防线③）：本期已确认销售中成本金额为 0 的明细行数
        int costMissingCount = 0;
        if (!saleIds.isEmpty()) {
            Long count = saleItemMapper.selectCount(new LambdaQueryWrapper<ErpSaleItem>()
                .in(ErpSaleItem::getOrderId, saleIds)
                .eq(ErpSaleItem::getCostAmount, BigDecimal.ZERO));
            costMissingCount = count == null ? 0 : count.intValue();
        }
        // 退货口径（DOC/24 §6.6）：净销售额 = 销售额 − 退货额；综合成本 = (销售成本 − 退货成本回冲) + 其他出库 + 其他支出
        List<ErpSaleReturn> returns = saleReturnMapper.selectList(new LambdaQueryWrapper<ErpSaleReturn>()
            .select(ErpSaleReturn::getTotalAmount, ErpSaleReturn::getReturnCostAmount)
            .eq(ErpSaleReturn::getStatus, "CONFIRMED")
            .ge(ErpSaleReturn::getBizDate, from)
            .lt(ErpSaleReturn::getBizDate, to));
        BigDecimal returnAmount = BigDecimal.ZERO;
        BigDecimal returnCost = BigDecimal.ZERO;
        for (ErpSaleReturn r : returns) {
            returnAmount = returnAmount.add(r.getTotalAmount() == null ? BigDecimal.ZERO : r.getTotalAmount());
            returnCost = returnCost.add(r.getReturnCostAmount() == null ? BigDecimal.ZERO : r.getReturnCostAmount());
        }
        BigDecimal netSales = salesTotal.subtract(returnAmount);
        BigDecimal netSalesCost = salesCost.subtract(returnCost);
        BigDecimal totalCost = netSalesCost.add(otherOut).add(otherExpense);

        FinCostReportVo vo = new FinCostReportVo();
        vo.setMonth(m);
        vo.setSaleReturnAmount(returnAmount.setScale(2, RoundingMode.HALF_UP));
        vo.setSaleReturnCostAmount(returnCost.setScale(2, RoundingMode.HALF_UP));
        BigDecimal salesAmount = netSales.setScale(2, RoundingMode.HALF_UP);
        BigDecimal grossProfit = netSales.subtract(totalCost).setScale(2, RoundingMode.HALF_UP);
        vo.setSalesAmount(salesAmount);
        vo.setSalesCost(salesCost.setScale(2, RoundingMode.HALF_UP));
        vo.setProductionMaterialCost(material.setScale(2, RoundingMode.HALF_UP));
        vo.setOtherOutCost(otherOut.setScale(2, RoundingMode.HALF_UP));
        vo.setOtherExpenseCost(otherExpense.setScale(2, RoundingMode.HALF_UP));
        vo.setTotalCost(totalCost.setScale(2, RoundingMode.HALF_UP));
        vo.setGrossProfit(grossProfit);
        // 毛利率与销售单口径一致：profit*100/total，销售收入为 0 时不可计算（null）
        vo.setGrossMargin(salesAmount.compareTo(BigDecimal.ZERO) > 0
            ? grossProfit.multiply(BigDecimal.valueOf(100)).divide(salesAmount, 2, RoundingMode.HALF_UP)
            : null);
        // 库存金额：期初/采购等入库形成的库存资产，体现在库存而非本期损益（DOC/26 §9.3）
        vo.setStockAmount(inventoryService.totalStockAmount());
        vo.setCostMissingCount(costMissingCount);
        return vo;
    }

    private BigDecimal sumLedger(String costType, Date from, Date to) {
        return costLedgerMapper.selectList(new LambdaQueryWrapper<FinCostLedger>()
                .select(FinCostLedger::getAmount)
                .eq(FinCostLedger::getCostType, costType)
                .eq(FinCostLedger::getStatus, "ACTIVE")
                .ge(FinCostLedger::getBizDate, from)
                .lt(FinCostLedger::getBizDate, to))
            .stream()
            .map(r -> r.getAmount() == null ? BigDecimal.ZERO : r.getAmount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
