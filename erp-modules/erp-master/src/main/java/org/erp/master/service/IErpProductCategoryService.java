package org.erp.master.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.erp.master.domain.bo.ErpProductCategoryBo;
import org.erp.master.domain.vo.ErpProductCategoryVo;

import java.util.List;

/**
 * 商品分类服务接口
 */
public interface IErpProductCategoryService {

    TableDataInfo<ErpProductCategoryVo> selectPage(String keyword, Integer status, PageQuery pageQuery);

    /**
     * 分类下拉/分组用列表：按 sort、id 升序，不按状态过滤（停用分类仍需展示存量商品分组）。
     *
     * @param keyword 编码/名称模糊匹配
     * @return 分类列表
     */
    List<ErpProductCategoryVo> selectList(String keyword);

    ErpProductCategoryVo selectById(Long id);

    void insert(ErpProductCategoryBo bo);

    void update(ErpProductCategoryBo bo);

    void deleteByIds(List<Long> ids);
}
