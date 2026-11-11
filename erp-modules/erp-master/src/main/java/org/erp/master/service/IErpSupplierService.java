package org.erp.master.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.erp.master.domain.bo.ErpSupplierBo;
import org.erp.master.domain.vo.ErpSupplierVo;

import java.util.List;

/**
 * 供应商服务接口
 */
public interface IErpSupplierService {

    TableDataInfo<ErpSupplierVo> selectPage(String keyword, Integer status, PageQuery pageQuery);

    List<ErpSupplierVo> selectList(String keyword);

    ErpSupplierVo selectById(Long id);

    void insert(ErpSupplierBo bo);

    void update(ErpSupplierBo bo);

    void deleteByIds(List<Long> ids);
}
