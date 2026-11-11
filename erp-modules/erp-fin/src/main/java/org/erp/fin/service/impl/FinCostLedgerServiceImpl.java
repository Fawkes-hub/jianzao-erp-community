package org.erp.fin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.erp.fin.domain.FinCostLedger;
import org.erp.fin.mapper.FinCostLedgerMapper;
import org.erp.fin.service.IFinCostLedgerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 成本台账服务实现
 */
@RequiredArgsConstructor
@Service
public class FinCostLedgerServiceImpl implements IFinCostLedgerService {

    private final FinCostLedgerMapper costLedgerMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void record(String costType, String sourceType, Long sourceId, String sourceNo,
                       Date bizDate, BigDecimal amount, String remark) {
        FinCostLedger row = new FinCostLedger();
        row.setCostType(costType);
        row.setSourceType(sourceType);
        row.setSourceId(sourceId);
        row.setSourceNo(sourceNo);
        row.setBizDate(bizDate);
        row.setAmount(amount);
        row.setStatus("ACTIVE");
        row.setRemark(remark);
        costLedgerMapper.insert(row);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reverseBySource(String sourceType, Long sourceId) {
        var rows = costLedgerMapper.selectList(new LambdaQueryWrapper<FinCostLedger>()
            .eq(FinCostLedger::getSourceType, sourceType)
            .eq(FinCostLedger::getSourceId, sourceId)
            .eq(FinCostLedger::getStatus, "ACTIVE"));
        for (FinCostLedger row : rows) {
            row.setStatus("REVERSED");
            costLedgerMapper.updateById(row);
        }
    }
}
