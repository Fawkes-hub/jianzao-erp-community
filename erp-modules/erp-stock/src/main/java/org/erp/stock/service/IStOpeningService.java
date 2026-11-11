package org.erp.stock.service;

import org.erp.stock.domain.bo.StOpeningBo;
import org.erp.stock.domain.vo.StOpeningVo;

import java.util.List;

/**
 * 期初库存服务
 *
 * 只录上线盘点数，不生成采购单、不形成应付（DOC/17 §2）；
 * 确认写 OPENING 流水初始化 st_stock 并锁定；反确认走 reverse。
 */
public interface IStOpeningService {

    List<StOpeningVo> listAll(Long warehouseId);

    StOpeningVo save(StOpeningBo bo);

    void confirm(Long id);

    void unconfirm(Long id);

    /**
     * 删除草稿期初库存：仅 DRAFT 可删，逻辑删除（del_flag=1），不写库存流水。
     *
     * @param id 期初行ID
     */
    void deleteDraft(Long id);
}
