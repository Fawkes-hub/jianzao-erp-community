package org.erp.fin.service;

import org.erp.fin.domain.bo.FinOpeningBo;
import org.erp.fin.domain.vo.FinOpeningVo;

import java.util.List;

/**
 * 期初往来服务
 *
 * 确认后落 fin_ar_ap(source_type=OPENING)（D1），参与应收应付汇总；
 * 确认后锁定，反确认需先冲销期初台账（已核销拒 40904）。
 */
public interface IFinOpeningService {

    List<FinOpeningVo> listAll(String partyType);

    FinOpeningVo save(FinOpeningBo bo);

    void confirm(Long id);

    void unconfirm(Long id);
}
