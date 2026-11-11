package org.dromara.system.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.system.domain.bo.SysTenantPackageBo;
import org.dromara.system.domain.vo.SysTenantPackageVo;

import java.util.Collection;
import java.util.List;

/**
 * 租户套餐Service接口
 *
 * @author Michelle.Chung
 */
public interface ISysTenantPackageService {

    /**
     * 查询租户套餐
     */
    SysTenantPackageVo queryById(Long packageId);

    /**
     * 查询租户套餐列表
     */
    TableDataInfo<SysTenantPackageVo> queryPageList(SysTenantPackageBo bo, PageQuery pageQuery);

    /**
     * 查询租户套餐已启用列表
     */
    List<SysTenantPackageVo> selectList();

    /**
     * 查询租户套餐列表
     */
    List<SysTenantPackageVo> queryList(SysTenantPackageBo bo);

    /**
     * 新增租户套餐
     */
    Boolean insertByBo(SysTenantPackageBo bo);

    /**
     * 修改租户套餐
     */
    Boolean updateByBo(SysTenantPackageBo bo);

    /**
     * 校验套餐名称是否唯一
     */
    boolean checkPackageNameUnique(SysTenantPackageBo bo);

    /**
     * 修改套餐状态
     */
    int updatePackageStatus(SysTenantPackageBo bo);

    /**
     * 校验并批量删除租户套餐信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 校验菜单是否都包含在套餐范围内
     * <p>
     * 租户管理员保存角色时调用，防止通过接口给企业角色分配套餐外菜单。
     *
     * @param packageId 套餐ID
     * @param menuIds   待校验的菜单ID
     */
    void checkMenuIdsWithinPackage(Long packageId, Collection<Long> menuIds);
}
