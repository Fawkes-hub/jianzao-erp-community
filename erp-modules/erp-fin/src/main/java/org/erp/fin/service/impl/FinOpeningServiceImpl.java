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
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 期初往来服务实现
 *
 * 规则（DOC/17 §3、DOC/02 D1）：
 * - 录入即 upsert（party_type+party_id 唯一键），CONFIRMED 行拒改（40902）；
 * - 期初是「某时点余额」：opening_date 表示该日期之前的账面余额，确认时落台账 biz_date；
 * - confirm：先校验截止日之前无已确认业务单据（防重复计算），再 createOpening 落 fin_ar_ap(OPENING)；
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
        vo.setOpeningDate(row.getOpeningDate());
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
        // Excel 导入等入口不经过 Controller 的 @Valid，这里统一兜底
        if (bo.getOpeningDate() != null && bo.getOpeningDate().isAfter(LocalDate.now())) {
            throw new ServiceException("期初截止日不能晚于今天");
        }
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
        row.setOpeningDate(bo.getOpeningDate());
        row.setRemark(bo.getRemark());
        row.setStatus("DRAFT");
        if (exist == null) {
            try {
                openingMapper.insert(row);
            } catch (DuplicateKeyException e) {
                throw new ServiceException("该单位已有期初记录，请刷新后重试");
            }
        } else {
            openingMapper.updateById(row);
        }
        FinOpeningVo vo = new FinOpeningVo();
        vo.setId(row.getId());
        vo.setPartyType(row.getPartyType());
        vo.setPartyId(row.getPartyId());
        vo.setAmount(row.getAmount());
        vo.setOpeningDate(row.getOpeningDate());
        vo.setStatus(row.getStatus());
        vo.setStatusText(StatusText.bill(row.getStatus()));
        // 前端据此提示"已覆盖原有草稿期初"，避免新增入口静默改写已有记录
        vo.setOverwritten(exist != null);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long id) {
        FinOpening row = openingMapper.selectByIdForUpdate(id);
        if (row == null) {
            throw new ServiceException("期初记录不存在");
        }
        if (!"DRAFT".equals(row.getStatus())) {
            throw new ServiceException("期初已确认", 40902);
        }
        LocalDate openingDate = row.getOpeningDate();
        if (openingDate == null) {
            throw new ServiceException("期初截止日为空，请先编辑补录截止日");
        }
        // 期初金额=截止日前的账面余额；该区间若已有业务单据，对账单会与期初重复累加，必须拦截
        Date boundary = Date.from(openingDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        long overlaps = arApService.countBusinessBefore(row.getPartyType(), row.getPartyId(), boundary);
        if (overlaps > 0) {
            throw new ServiceException(String.format(
                "期初截止日 %s 及之前已有 %d 笔已确认业务单据，期初金额会与这些单据重复计算；请把截止日调整到第一笔单据之前", openingDate, overlaps));
        }
        // D1：期初唯一账面来源是 fin_ar_ap(source_type=OPENING)；重录时 createOpening 先冲旧
        Date bizDate = Date.from(openingDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        arApService.createOpening(row.getPartyType(), row.getPartyId(), row.getAmount(), row.getId(), bizDate);
        row.setStatus("CONFIRMED");
        openingMapper.updateById(row);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unconfirm(Long id) {
        FinOpening row = openingMapper.selectByIdForUpdate(id);
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
