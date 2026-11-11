package org.erp.stock.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.erp.master.domain.ErpCustomer;
import org.erp.master.domain.ErpProduct;
import org.erp.master.domain.ErpSupplier;
import org.erp.master.mapper.ErpCustomerMapper;
import org.erp.master.mapper.ErpProductMapper;
import org.erp.master.mapper.ErpSupplierMapper;
import org.erp.stock.domain.ErpStock;
import org.erp.stock.domain.ErpStockLog;
import org.erp.stock.domain.vo.ErpStockLogVo;
import org.erp.stock.domain.vo.ErpStockVo;
import org.erp.stock.mapper.ErpStockLogMapper;
import org.erp.stock.mapper.ErpStockMapper;
import org.erp.stock.service.IInventoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 库存服务实现
 *
 * 规则（DOC/11 §8.2/8.3）：
 * - 出库条件扣减防超卖；入库回补并移动加权；
 * - 流水只增不改；撤回/作废写反向流水后按流水重放重算 avg_cost；
 * - 中间精度 6 位，入库精度：数量 3 位、成本 4 位；
 * - 新增入库/出库/盘点差异按商品 md_product.qty_scale 校验数量小数位（DOC/26 §10）。
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class InventoryServiceImpl implements IInventoryService {

    private static final int LOG_TYPE_VOID = 1;

    private final ErpStockMapper stockMapper;
    private final ErpStockLogMapper stockLogMapper;
    private final ErpProductMapper productMapper;
    private final ErpSupplierMapper supplierMapper;
    private final ErpCustomerMapper customerMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void outbound(String bizType, String bizNo, Long bizId, Long productId, Long warehouseId,
                         BigDecimal qty, String partnerType, Long partnerId, Date bizDate, int seq) {
        if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException("出库数量必须大于0");
        }
        checkQtyScale(productId, qty);
        ensureStockRow(productId, warehouseId);
        // 行锁：保证读均价-扣减-快照原子
        stockMapper.selectForUpdate(productId, warehouseId);
        int rows = stockMapper.deductQty(productId, warehouseId, qty);
        if (rows == 0) {
            throw new ServiceException("库存不足，单据保存失败", 40901);
        }
        BigDecimal avgCost = getAvgCost(productId, warehouseId);
        writeLog(bizType, bizNo, bizId, productId, warehouseId, qty.negate(),
            avgCost, avgCost.multiply(qty).setScale(2, RoundingMode.HALF_UP),
            partnerType, partnerId, bizDate, seq);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void outbound(String bizType, String bizNo, Long bizId, Long productId, Long warehouseId,
                         BigDecimal qty, BigDecimal unitCost, String partnerType, Long partnerId,
                         Date bizDate, int seq) {
        if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException("出库数量必须大于0");
        }
        checkQtyScale(productId, qty);
        ensureStockRow(productId, warehouseId);
        stockMapper.selectForUpdate(productId, warehouseId);
        int rows = stockMapper.deductQty(productId, warehouseId, qty);
        if (rows == 0) {
            throw new ServiceException("库存不足，单据保存失败", 40901);
        }
        // 成本快照由调用方指定（退货按原行成本回冲），不取当前 avg_cost
        BigDecimal cost = unitCost == null ? BigDecimal.ZERO : unitCost;
        writeLog(bizType, bizNo, bizId, productId, warehouseId, qty.negate(),
            cost, cost.multiply(qty).setScale(2, RoundingMode.HALF_UP),
            partnerType, partnerId, bizDate, seq);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void inbound(String bizType, String bizNo, Long bizId, Long productId, Long warehouseId,
                        BigDecimal qty, BigDecimal unitCost, String partnerType, Long partnerId,
                        Date bizDate, int seq) {
        if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException("入库数量必须大于0");
        }
        checkQtyScale(productId, qty);
        ensureStockRow(productId, warehouseId);
        // 行锁：加权计算原子
        stockMapper.selectForUpdate(productId, warehouseId);
        BigDecimal oldQty = getQty(productId, warehouseId);
        BigDecimal oldCost = getAvgCost(productId, warehouseId);
        BigDecimal cost = unitCost == null ? BigDecimal.ZERO : unitCost;

        BigDecimal totalQty = oldQty.add(qty);
        BigDecimal newAvg;
        if (totalQty.compareTo(BigDecimal.ZERO) > 0
            && oldQty.multiply(oldCost).add(qty.multiply(cost)).compareTo(BigDecimal.ZERO) > 0) {
            newAvg = oldQty.multiply(oldCost).add(qty.multiply(cost))
                .divide(totalQty, 6, RoundingMode.HALF_UP)
                .setScale(4, RoundingMode.HALF_UP);
        } else {
            newAvg = cost.setScale(4, RoundingMode.HALF_UP);
        }

        stockMapper.addQty(productId, warehouseId, qty);
        stockMapper.updateAvgCost(productId, warehouseId, newAvg);
        writeLog(bizType, bizNo, bizId, productId, warehouseId, qty,
            cost, cost.multiply(qty).setScale(2, RoundingMode.HALF_UP),
            partnerType, partnerId, bizDate, seq);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reverse(String bizNo, Long bizId, Long productId, Long warehouseId, Date bizDate, int seq) {
        // 行锁：防并发双重回退写双份 VOID
        stockMapper.selectForUpdate(productId, warehouseId);
        // 单据撤回可跨确认代次发生（confirm→withdraw→confirm→void），
        // 判重按"原始流水是否已被 VOID 配对"：VOID 行 partner_id 记被冲销的原始流水 id。
        List<ErpStockLog> origin = stockLogMapper.selectList(new LambdaQueryWrapper<ErpStockLog>()
            .eq(ErpStockLog::getBizNo, bizNo)
            .eq(ErpStockLog::getProductId, productId)
            .eq(ErpStockLog::getWarehouseId, warehouseId)
            .ne(ErpStockLog::getBizType, "VOID")
            .orderByAsc(ErpStockLog::getId));
        if (origin.isEmpty()) {
            throw new ServiceException("未找到可回退的库存流水：" + bizNo);
        }
        List<ErpStockLog> voids = stockLogMapper.selectList(new LambdaQueryWrapper<ErpStockLog>()
            .eq(ErpStockLog::getBizNo, bizNo)
            .eq(ErpStockLog::getProductId, productId)
            .eq(ErpStockLog::getWarehouseId, warehouseId)
            .eq(ErpStockLog::getBizType, "VOID"));
        java.util.Set<Long> voidedOriginIds = new java.util.HashSet<>();
        for (ErpStockLog v : voids) {
            if (v.getPartnerId() != null) {
                voidedOriginIds.add(v.getPartnerId());
            }
        }
        // 回退顺序：先冲销出库流水（正数回补）再冲销入库流水（负数扣减）。
        // 现录现卖同单先入库后出库，若按流水 id 先扣回入库量，库存已被销售清空时会误判「回退后库存不足」。
        List<ErpStockLog> pending = origin.stream()
            .filter(r -> !voidedOriginIds.contains(r.getId()))
            .sorted(Comparator.comparing((ErpStockLog r) -> r.getQty().compareTo(BigDecimal.ZERO) > 0))
            .toList();
        if (pending.isEmpty()) {
            throw new ServiceException("该单据已撤回过，禁止重复回退：" + bizNo);
        }
        for (ErpStockLog row : pending) {
            BigDecimal reverseQty = row.getQty().negate();
            if (reverseQty.compareTo(BigDecimal.ZERO) > 0) {
                stockMapper.addQty(productId, warehouseId, reverseQty);
            } else {
                int rows = stockMapper.deductQty(productId, warehouseId, reverseQty.negate());
                if (rows == 0) {
                    throw new ServiceException("回退后库存不足，无法撤回：" + bizNo, 40901);
                }
            }
            writeLogWithOrigin("VOID", bizNo, bizId, productId, warehouseId, reverseQty,
                row.getUnitCost(), row.getAmount().negate(), bizDate, seq, row.getId());
        }
        replayAvgCost(productId, warehouseId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reverseByBill(String bizNo, Long bizId, Date bizDate) {
        List<ErpStockLog> origin = stockLogMapper.selectList(new LambdaQueryWrapper<ErpStockLog>()
            .eq(ErpStockLog::getBizNo, bizNo)
            .ne(ErpStockLog::getBizType, "VOID")
            .orderByAsc(ErpStockLog::getId));
        if (origin.isEmpty()) {
            throw new ServiceException("未找到可回退的库存流水：" + bizNo);
        }
        List<ErpStockLog> voids = stockLogMapper.selectList(new LambdaQueryWrapper<ErpStockLog>()
            .eq(ErpStockLog::getBizNo, bizNo)
            .eq(ErpStockLog::getBizType, "VOID"));
        Set<Long> voidedOriginIds = new HashSet<>();
        for (ErpStockLog v : voids) {
            if (v.getPartnerId() != null) {
                voidedOriginIds.add(v.getPartnerId());
            }
        }
        List<ErpStockLog> pending = origin.stream()
            .filter(r -> !voidedOriginIds.contains(r.getId()))
            .sorted(Comparator.comparing((ErpStockLog r) -> r.getQty().compareTo(BigDecimal.ZERO) > 0)
                .thenComparing(ErpStockLog::getProductId)
                .thenComparing(ErpStockLog::getId))
            .toList();
        if (pending.isEmpty()) {
            throw new ServiceException("该单据已撤回过，禁止重复回退：" + bizNo);
        }
        // 先按 product_id 升序锁齐本单涉及的全部库存行，避免跨单据 ABBA 死锁（DOC/31 §6.8.1）
        List<ErpStockLog> lockOrder = pending.stream()
            .sorted(Comparator.comparing(ErpStockLog::getProductId)
                .thenComparing(ErpStockLog::getWarehouseId)
                .thenComparing(ErpStockLog::getId))
            .toList();
        Set<String> locked = new HashSet<>();
        for (ErpStockLog row : lockOrder) {
            String key = row.getProductId() + ":" + row.getWarehouseId();
            if (locked.add(key)) {
                ensureStockRow(row.getProductId(), row.getWarehouseId());
                stockMapper.selectForUpdate(row.getProductId(), row.getWarehouseId());
            }
        }
        // 回退顺序：先冲销出库流水（正数回补）再冲销入库流水（负数扣减）
        int seq = 0;
        for (ErpStockLog row : pending) {
            BigDecimal reverseQty = row.getQty().negate();
            if (reverseQty.compareTo(BigDecimal.ZERO) > 0) {
                stockMapper.addQty(row.getProductId(), row.getWarehouseId(), reverseQty);
            } else {
                int rows = stockMapper.deductQty(row.getProductId(), row.getWarehouseId(), reverseQty.negate());
                if (rows == 0) {
                    throw new ServiceException("回退后库存不足，无法撤回：" + bizNo, 40901);
                }
            }
            writeLogWithOrigin("VOID", bizNo, bizId, row.getProductId(), row.getWarehouseId(), reverseQty,
                row.getUnitCost(), row.getAmount().negate(), bizDate, seq++, row.getId());
        }
        // 逐商品重放均价：同一商品在本单的多行流水已全部冲销后再重放，避免中途重复计算
        List<String> replayed = new ArrayList<>();
        for (ErpStockLog row : lockOrder) {
            String key = row.getProductId() + ":" + row.getWarehouseId();
            if (!replayed.contains(key)) {
                replayAvgCost(row.getProductId(), row.getWarehouseId());
                replayed.add(key);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replayAvgCost(Long productId, Long warehouseId) {
        // D2：按流水全量重放，禁止直接回滚旧值
        List<ErpStockLog> logs = stockLogMapper.selectList(new LambdaQueryWrapper<ErpStockLog>()
            .eq(ErpStockLog::getProductId, productId)
            .eq(ErpStockLog::getWarehouseId, warehouseId));
        // 重放必须按"写入序"（OPENING 最前，其余按 id 升序，雪花 id 天然等于写入时间），
        // 而非业务日期序：出库流水写入时账面必然够扣（deductQty 熔断保证），
        // 按写入序重演永远不会中途为负。若按业务日期排序，后补录入库单（业务日期晚于
        // 旧出库单）会被排到出库之前，重放中途库存变负，导致撤回/作废报
        // 「流水重放出现负库存，数据异常」。业务日期只影响报表分组，不决定成本重放因果序。
        logs.sort(Comparator
            .comparing((ErpStockLog row) -> "OPENING".equals(row.getBizType()) ? 0 : 1)
            .thenComparing(ErpStockLog::getId));

        BigDecimal qty = BigDecimal.ZERO;
        BigDecimal avgCost = BigDecimal.ZERO;
        for (ErpStockLog row : logs) {
            BigDecimal delta = row.getQty();
            BigDecimal cost = row.getUnitCost() == null ? BigDecimal.ZERO : row.getUnitCost();
            boolean isVoid = "VOID".equals(row.getBizType());
            if (delta.compareTo(BigDecimal.ZERO) > 0) {
                if (isVoid) {
                    // 出库的反向流水：回补，不改变 avg_cost（出库本就不改均价）
                    qty = qty.add(delta);
                } else {
                    BigDecimal totalQty = qty.add(delta);
                    if (totalQty.compareTo(BigDecimal.ZERO) > 0) {
                        avgCost = qty.multiply(avgCost).add(delta.multiply(cost))
                            .divide(totalQty, 6, RoundingMode.HALF_UP)
                            .setScale(4, RoundingMode.HALF_UP);
                    }
                    qty = totalQty;
                }
            } else if (delta.compareTo(BigDecimal.ZERO) < 0) {
                if (isVoid) {
                    // 入库的反向流水：按其原入库成本从加权中剔除
                    BigDecimal remainQty = qty.add(delta);
                    if (remainQty.compareTo(BigDecimal.ZERO) > 0 && cost.compareTo(BigDecimal.ZERO) > 0) {
                        avgCost = qty.multiply(avgCost).subtract(delta.negate().multiply(cost))
                            .divide(remainQty, 6, RoundingMode.HALF_UP)
                            .setScale(4, RoundingMode.HALF_UP);
                    } else if (remainQty.compareTo(BigDecimal.ZERO) == 0) {
                        avgCost = BigDecimal.ZERO;
                    }
                    qty = remainQty;
                } else {
                    // 正常出库：按当前 avg_cost 转出，avg_cost 不变
                    qty = qty.add(delta);
                }
                if (qty.compareTo(BigDecimal.ZERO) < 0) {
                    throw new ServiceException("流水重放出现负库存，数据异常：产品" + productId);
                }
            } else {
                throw new ServiceException("库存流水中存在零数量记录：" + row.getId());
            }
        }
        ensureStockRow(productId, warehouseId);
        stockMapper.updateAvgCost(productId, warehouseId, avgCost.setScale(4, RoundingMode.HALF_UP));
        if (log.isDebugEnabled()) {
            log.debug("replay avg_cost product={} warehouse={} qty={} avg={}", productId, warehouseId, qty, avgCost);
        }
    }

    private void writeLog(String bizType, String bizNo, Long bizId, Long productId, Long warehouseId,
                          BigDecimal qty, BigDecimal unitCost, BigDecimal amount,
                          String partnerType, Long partnerId, Date bizDate, int seq) {
        ErpStockLog row = new ErpStockLog();
        row.setBizType(bizType);
        row.setBizNo(bizNo);
        row.setBizId(bizId);
        row.setProductId(productId);
        row.setWarehouseId(warehouseId);
        row.setQty(qty);
        row.setUnitCost(unitCost == null ? BigDecimal.ZERO : unitCost);
        row.setAmount(amount);
        row.setPartnerType(StringUtils.isBlank(partnerType) ? "NONE" : partnerType);
        row.setPartnerId(partnerId);
        row.setBalanceAfter(getQty(productId, warehouseId));
        row.setBizDate(bizDate);
        row.setSeq(seq);
        stockLogMapper.insert(row);
    }

    /**
     * VOID 行专用：partner_id 承载被冲销的原始流水 id（判重与审计配对）。
     */
    private void writeLogWithOrigin(String bizType, String bizNo, Long bizId, Long productId, Long warehouseId,
                                    BigDecimal qty, BigDecimal unitCost, BigDecimal amount,
                                    Date bizDate, int seq, Long originLogId) {
        writeLog(bizType, bizNo, bizId, productId, warehouseId, qty, unitCost, amount,
            "NONE", originLogId, bizDate, seq);
    }

    private void ensureStockRow(Long productId, Long warehouseId) {
        Long count = stockMapper.selectCount(new LambdaQueryWrapper<ErpStock>()
            .eq(ErpStock::getProductId, productId)
            .eq(ErpStock::getWarehouseId, warehouseId));
        if (count == null || count == 0) {
            ErpStock row = new ErpStock();
            row.setProductId(productId);
            row.setWarehouseId(warehouseId);
            row.setQty(BigDecimal.ZERO);
            row.setAvgCost(BigDecimal.ZERO);
            try {
                stockMapper.insert(row);
            } catch (Exception e) {
                // 并发下唯一键冲突视为已存在
                log.debug("stock row already exists: product={} warehouse={}", productId, warehouseId);
            }
        }
    }

    @Override
    public BigDecimal getQty(Long productId, Long warehouseId) {
        ErpStock row = stockMapper.selectOne(new LambdaQueryWrapper<ErpStock>()
            .eq(ErpStock::getProductId, productId)
            .eq(ErpStock::getWarehouseId, warehouseId));
        return row == null ? BigDecimal.ZERO : row.getQty();
    }

    @Override
    public BigDecimal getAvgCost(Long productId, Long warehouseId) {
        ErpStock row = stockMapper.selectOne(new LambdaQueryWrapper<ErpStock>()
            .eq(ErpStock::getProductId, productId)
            .eq(ErpStock::getWarehouseId, warehouseId));
        return row == null ? BigDecimal.ZERO : row.getAvgCost();
    }

    @Override
    public List<ErpStockVo> listStock(String keyword, String type) {
        List<ErpStock> rows = stockMapper.selectList(new LambdaQueryWrapper<ErpStock>()
            .orderByAsc(ErpStock::getProductId));
        Map<Long, ErpProduct> products = loadProducts(
            rows.stream().map(ErpStock::getProductId).filter(Objects::nonNull).distinct().toList());
        // 按产品类型与关键字过滤：type 精确匹配，keyword 命中编码/名称/规格
        boolean filterType = type != null && !type.isBlank();
        boolean filterKeyword = keyword != null && !keyword.isBlank();
        return rows.stream().map(r -> {
            ErpStockVo vo = new ErpStockVo();
            vo.setProductId(r.getProductId());
            vo.setWarehouseId(r.getWarehouseId());
            vo.setQty(r.getQty());
            vo.setAvgCost(r.getAvgCost());
            vo.setStockAmount(r.getQty().multiply(r.getAvgCost()).setScale(2, RoundingMode.HALF_UP));
            // 有库存但均价为 0：出库成本会按 0 计，标记「成本缺失」（DOC/26 §2.4 防线③）
            vo.setCostMissing(r.getQty().signum() > 0 && r.getAvgCost().signum() <= 0);
            ErpProduct p = products.get(r.getProductId());
            if (p != null) {
                vo.setProductCode(p.getCode());
                vo.setProductName(p.getName());
                vo.setSpec(p.getSpec());
                vo.setUnit(p.getUnit());
                vo.setType(p.getType());
                // 数量精度与上下限随行下发，移动端无需再按小数位猜测，也无需二次请求预警接口
                vo.setQtyScale(p.getQtyScale());
                vo.setMinStock(p.getMinStock());
                vo.setMaxStock(p.getMaxStock());
            }
            return vo;
        })
            .filter(vo -> !filterType || type.equals(vo.getType()))
            .filter(vo -> !filterKeyword || matchKeyword(vo, keyword))
            .toList();
    }

    /**
     * 关键字匹配：编码、名称、规格，忽略大小写。
     */
    private boolean matchKeyword(ErpStockVo vo, String keyword) {
        String kw = keyword.trim().toLowerCase();
        return contains(vo.getProductCode(), kw) || contains(vo.getProductName(), kw)
            || contains(vo.getSpec(), kw);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword);
    }

    @Override
    public ErpStockVo snapshot(Long productId) {
        // 单产品库存快照：主仓（最小 warehouseId）口径，与 snapshotCost 一致
        List<ErpStock> rows = stockMapper.selectList(new LambdaQueryWrapper<ErpStock>()
            .eq(ErpStock::getProductId, productId)
            .orderByAsc(ErpStock::getWarehouseId));
        ErpStockVo vo = new ErpStockVo();
        vo.setProductId(productId);
        if (!rows.isEmpty()) {
            ErpStock row = rows.get(0);
            vo.setWarehouseId(row.getWarehouseId());
            vo.setQty(row.getQty());
            vo.setAvgCost(row.getAvgCost());
            vo.setStockAmount(row.getQty().multiply(row.getAvgCost()).setScale(2, RoundingMode.HALF_UP));
            vo.setCostMissing(row.getQty().signum() > 0 && row.getAvgCost().signum() <= 0);
        } else {
            vo.setQty(BigDecimal.ZERO);
            vo.setAvgCost(BigDecimal.ZERO);
            vo.setStockAmount(BigDecimal.ZERO);
            vo.setCostMissing(false);
        }
        ErpProduct p = productMapper.selectById(productId);
        if (p != null) {
            vo.setProductCode(p.getCode());
            vo.setProductName(p.getName());
            vo.setSpec(p.getSpec());
            vo.setUnit(p.getUnit());
            vo.setType(p.getType());
        }
        return vo;
    }

    private Map<Long, ErpProduct> loadProducts(List<Long> productIds) {
        Map<Long, ErpProduct> products = new HashMap<>();
        if (productIds.isEmpty()) {
            return products;
        }
        for (ErpProduct p : productMapper.selectByIds(productIds)) {
            products.put(p.getId(), p);
        }
        return products;
    }

    @Override
    public List<ErpStockLogVo> listLogs(Long productId) {
        List<ErpStockLog> rows = stockLogMapper.selectList(new LambdaQueryWrapper<ErpStockLog>()
            .eq(ErpStockLog::getProductId, productId)
            .orderByDesc(ErpStockLog::getBizDate)
            .orderByDesc(ErpStockLog::getId));
        // 往来单位名称批量查，避免 N+1
        List<Long> supplierIds = rows.stream()
            .filter(r -> "SUPPLIER".equals(r.getPartnerType()))
            .map(ErpStockLog::getPartnerId).filter(Objects::nonNull).distinct().toList();
        List<Long> customerIds = rows.stream()
            .filter(r -> "CUSTOMER".equals(r.getPartnerType()))
            .map(ErpStockLog::getPartnerId).filter(Objects::nonNull).distinct().toList();
        Map<Long, String> supplierNames = new HashMap<>();
        if (!supplierIds.isEmpty()) {
            for (ErpSupplier s : supplierMapper.selectByIds(supplierIds)) {
                supplierNames.put(s.getId(), s.getName());
            }
        }
        Map<Long, String> customerNames = new HashMap<>();
        if (!customerIds.isEmpty()) {
            for (ErpCustomer c : customerMapper.selectByIds(customerIds)) {
                customerNames.put(c.getId(), c.getName());
            }
        }
        return rows.stream().map(r -> {
            ErpStockLogVo vo = new ErpStockLogVo();
            vo.setId(r.getId());
            vo.setBizType(r.getBizType());
            vo.setBizNo(r.getBizNo());
            vo.setBizId(r.getBizId());
            vo.setProductId(r.getProductId());
            vo.setWarehouseId(r.getWarehouseId());
            vo.setQty(r.getQty());
            vo.setUnitCost(r.getUnitCost());
            vo.setAmount(r.getAmount());
            vo.setPartnerType(r.getPartnerType());
            vo.setPartnerId(r.getPartnerId());
            vo.setPartnerName(resolvePartnerName(r.getPartnerType(), r.getPartnerId(), supplierNames, customerNames));
            vo.setBalanceAfter(r.getBalanceAfter());
            vo.setBizDate(r.getBizDate());
            vo.setSeq(r.getSeq());
            return vo;
        }).toList();
    }

    private String resolvePartnerName(String partnerType, Long partnerId,
                                      Map<Long, String> supplierNames, Map<Long, String> customerNames) {
        if (partnerId == null || partnerType == null) {
            return null;
        }
        if ("SUPPLIER".equals(partnerType)) {
            return supplierNames.get(partnerId);
        }
        if ("CUSTOMER".equals(partnerType)) {
            return customerNames.get(partnerId);
        }
        // NONE / 其他类型无往来单位
        return null;
    }

    @Override
    public BigDecimal snapshotCost(Long productId) {
        // 默认主仓：取最小 warehouseId 的 avg_cost；v1 单仓
        List<ErpStock> rows = stockMapper.selectList(new LambdaQueryWrapper<ErpStock>()
            .eq(ErpStock::getProductId, productId)
            .orderByAsc(ErpStock::getWarehouseId));
        return rows.isEmpty() ? BigDecimal.ZERO : rows.get(0).getAvgCost();
    }

    @Override
    public Map<Long, BigDecimal> snapshotCostBatch(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Map.of();
        }
        // 全量捞出后按 warehouseId 升序取每个产品第一条（主仓口径，与 snapshotCost 一致），避免逐个查询
        List<ErpStock> rows = stockMapper.selectList(new LambdaQueryWrapper<ErpStock>()
            .in(ErpStock::getProductId, productIds)
            .orderByAsc(ErpStock::getWarehouseId));
        Map<Long, BigDecimal> result = new HashMap<>();
        for (ErpStock row : rows) {
            result.putIfAbsent(row.getProductId(), row.getAvgCost() == null ? BigDecimal.ZERO : row.getAvgCost());
        }
        return result;
    }

    @Override
    public BigDecimal totalStockAmount() {
        // 库存资产口径：全仓 Σ 数量 × 移动加权均价，与 listStock 的行金额一致
        return stockMapper.selectList(new LambdaQueryWrapper<ErpStock>()
                .select(ErpStock::getQty, ErpStock::getAvgCost))
            .stream()
            .map(r -> (r.getQty() == null ? BigDecimal.ZERO : r.getQty())
                .multiply(r.getAvgCost() == null ? BigDecimal.ZERO : r.getAvgCost()))
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 数量精度校验（DOC/26 §10.5）：按商品数量精度判断有效小数位，超精度拒绝写入。
     * 仅拦截新增写入的数量；撤回 reverse 与重放 replayAvgCost 不经过此方法，历史数据不受影响。
     *
     * @param productId 商品ID（查不到时跳过校验，由调用方保证商品存在）
     * @param qty       待写入数量，正负均可，按绝对值判断（盘点差异为负）
     */
    private void checkQtyScale(Long productId, BigDecimal qty) {
        ErpProduct product = productMapper.selectById(productId);
        if (product == null) {
            return;
        }
        int allowed = product.getQtyScale() == null ? 0 : product.getQtyScale();
        // 去掉末尾 0 后的有效小数位，避免 1.500 被误判成 3 位
        int scale = Math.max(0, qty.stripTrailingZeros().scale());
        if (scale > allowed) {
            throw new ServiceException("商品「" + product.getName() + "」数量最多支持 " + allowed + " 位小数");
        }
    }

    /**
     * 按 product_id 升序一次性锁齐本单涉及的全部库存行（DOC/31 §6.8.1）。
     * 去重、排序在这里完成，调用方明细顺序任意；加锁顺序固定后，与 inbound/outbound 的逐行补锁不会成环。
     *
     * @param productIds 本单涉及的商品ID集合，可空（空则不加锁）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void lockStockRows(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return;
        }
        List<Long> ordered = productIds.stream().filter(Objects::nonNull).distinct().sorted().toList();
        if (ordered.isEmpty()) {
            return;
        }
        // 库存行可能尚不存在（首次入库），此时无锁可拿，由后续 inbound 的 ensureStockRow 兜底
        stockMapper.selectByProductIdsForUpdate(ordered);
    }
}
