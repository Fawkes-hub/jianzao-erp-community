package org.erp.fin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.erp.common.enums.StatusText;
import org.erp.fin.domain.FinOpening;
import org.erp.fin.domain.bo.FinOpeningBo;
import org.erp.fin.domain.vo.FinOpeningVo;
import org.erp.fin.mapper.FinOpeningMapper;
import org.erp.fin.service.IFinArApService;
import org.erp.fin.service.IFinOpeningService;
import org.erp.master.domain.ErpCustomer;
import org.erp.master.domain.ErpSupplier;
import org.erp.master.mapper.ErpCustomerMapper;
import org.erp.master.mapper.ErpSupplierMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 期初往来服务实现
 *
 * 规则（DOC/17 §3、DOC/02 D1）：
 * - 录入即 upsert（party_type+party_id 唯一键），CONFIRMED 行拒改（40902）；
 * - confirm：createOpening 落 fin_ar_ap(OPENING)；重录即先冲旧（createOpening 内置）；
 * - unconfirm：先冲销期初台账（settled>0 拒 40904），行回 DRAFT。
 */
@RequiredArgsConstructor
@Service
public class FinOpeningServiceImpl implements IFinOpeningService {

    private final FinOpeningMapper openingMapper;
    private final ErpCustomerMapper customerMapper;
    private final ErpSupplierMapper supplierMapper;
    private final IFinArApService arApService;

    @Override
    public List<FinOpeningVo> listAll(String partyType) {
        List<FinOpening> rows = openingMapper.selectList(new LambdaQueryWrapper<FinOpening>()
            .eq(partyType != null && !partyType.isBlank(), FinOpening::getPartyType, partyType)
            .orderByAsc(FinOpening::getId));
        List<FinOpeningVo> result = new ArrayList<>();
        for (FinOpening row : rows) {
            result.add(toVo(row));
        }
        return result;
    }

    private FinOpeningVo toVo(FinOpening row) {
        FinOpeningVo vo = new FinOpeningVo();
        vo.setId(row.getId());
        vo.setPartyType(row.getPartyType());
        vo.setPartyId(row.getPartyId());
        vo.setPartyName(resolvePartyName(row.getPartyType(), row.getPartyId()));
        vo.setAmount(row.getAmount());
        vo.setStatus(row.getStatus());
        vo.setStatusText(StatusText.bill(row.getStatus()));
        vo.setRemark(row.getRemark());
        vo.setCreateTime(row.getCreateTime());
        return vo;
    }

    private String resolvePartyName(String partyType, Long partyId) {
        if ("AR".equals(partyType)) {
            ErpCustomer c = customerMapper.selectById(partyId);
            return c == null ? null : c.getName();
        }
        ErpSupplier s = supplierMapper.selectById(partyId);
        return s == null ? null : s.getName();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinOpeningVo save(FinOpeningBo bo) {
        FinOpening exist = openingMapper.selectOne(new LambdaQueryWrapper<FinOpening>()
            .eq(FinOpening::getPartyType, bo.getPartyType())
            .eq(FinOpening::getPartyId, bo.getPartyId()));
        if (exist != null && "CONFIRMED".equals(exist.getStatus())) {
            throw new ServiceException("期初已确认锁定，请先反确认", 40902);
        }
        FinOpening row = exist == null ? new FinOpening() : exist;
        row.setPartyType(bo.getPartyType());
        row.setPartyId(bo.getPartyId());
        row.setAmount(bo.getAmount().setScale(2, RoundingMode.HALF_UP));
        row.setRemark(bo.getRemark());
        row.setStatus("DRAFT");
        if (exist == null) {
            openingMapper.insert(row);
        } else {
            openingMapper.updateById(row);
        }
        FinOpeningVo vo = new FinOpeningVo();
        vo.setId(row.getId());
        vo.setPartyType(row.getPartyType());
        vo.setPartyId(row.getPartyId());
        vo.setAmount(row.getAmount());
        vo.setStatus(row.getStatus());
        vo.setStatusText(StatusText.bill(row.getStatus()));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long id) {
        FinOpening row = openingMapper.selectById(id);
        if (row == null) {
            throw new ServiceException("期初记录不存在");
        }
        if (!"DRAFT".equals(row.getStatus())) {
            throw new ServiceException("期初已确认", 40902);
        }
        // D1：期初唯一账面来源是 fin_ar_ap(source_type=OPENING)；重录时 createOpening 先冲旧
        arApService.createOpening(row.getPartyType(), row.getPartyId(), row.getAmount(), row.getId(), new Date());
        row.setStatus("CONFIRMED");
        openingMapper.updateById(row);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unconfirm(Long id) {
        FinOpening row = openingMapper.selectById(id);
        if (row == null) {
            throw new ServiceException("期初记录不存在");
        }
        if (!"CONFIRMED".equals(row.getStatus())) {
            throw new ServiceException("仅已确认期初可反确认", 40902);
        }
        // 期初台账已核销时 voidBySource 抛 40904
        arApService.voidBySource("OPENING", row.getId());
        row.setStatus("DRAFT");
        openingMapper.updateById(row);
    }
}
