package org.erp.master.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.erp.master.domain.bo.ErpCustomerBo;
import org.erp.master.domain.vo.ErpCustomerVo;

import java.util.List;

/**
 * 客户服务接口
 */
public interface IErpCustomerService {

    TableDataInfo<ErpCustomerVo> selectPage(String keyword, Integer status, PageQuery pageQuery);

    List<ErpCustomerVo> selectList(String keyword);

    ErpCustomerVo selectById(Long id);

    void insert(ErpCustomerBo bo);

    void update(ErpCustomerBo bo);

    void deleteByIds(List<Long> ids);
}
