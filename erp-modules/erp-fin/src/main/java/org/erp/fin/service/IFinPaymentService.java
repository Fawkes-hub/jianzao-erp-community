package org.erp.fin.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.erp.fin.domain.bo.FinPaymentBo;
import org.erp.fin.domain.vo.FinPaymentVo;

/**
 * 付款单服务
 *
 * 确认落核销（fin_payment_alloc + fin_ar_ap.settled_amount）；
 * 撤回先反核销；作废走撤回。
 */
public interface IFinPaymentService {

    TableDataInfo<FinPaymentVo> selectPage(Long supplierId, String status, PageQuery pageQuery);

    FinPaymentVo selectById(Long id);

    FinPaymentVo insert(FinPaymentBo bo);

    FinPaymentVo update(Long id, FinPaymentBo bo);

    void confirm(Long id);

    void withdraw(Long id);

    void voidBill(Long id);

    /**
     * 删除草稿付款单（逻辑删除，仅 DRAFT 可删）
     *
     * @param id 付款单ID
     */
    void deleteDraft(Long id);
}
