package org.dromara.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.constant.SystemConstants;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.system.domain.SysTenant;
import org.dromara.system.domain.SysTenantPackage;
import org.dromara.system.domain.bo.SysTenantPackageBo;
import org.dromara.system.domain.vo.SysTenantPackageVo;
import org.dromara.system.mapper.SysTenantMapper;
import org.dromara.system.mapper.SysTenantPackageMapper;
import org.dromara.system.service.ISysTenantPackageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 租户套餐Service业务层处理
 *
 * @author Michelle.Chung
 */
@RequiredArgsConstructor
@Service
public class SysTenantPackageServiceImpl implements ISysTenantPackageService {

    private final SysTenantPackageMapper baseMapper;
    private final SysTenantMapper tenantMapper;

    /**
     * 查询租户套餐
     */
    @Override
    public SysTenantPackageVo queryById(Long packageId){
        return baseMapper.selectVoById(packageId);
    }

    /**
     * 查询租户套餐列表
     */
    @Override
    public TableDataInfo<SysTenantPackageVo> queryPageList(SysTenantPackageBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<SysTenantPackage> lqw = buildQueryWrapper(bo);
        Page<SysTenantPackageVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    @Override
    public List<SysTenantPackageVo> selectList() {
        return baseMapper.selectVoList(new LambdaQueryWrapper<SysTenantPackage>()
                .eq(SysTenantPackage::getStatus, SystemConstants.NORMAL));
    }

    /**
     * 查询租户套餐列表
     */
    @Override
    public List<SysTenantPackageVo> queryList(SysTenantPackageBo bo) {
        LambdaQueryWrapper<SysTenantPackage> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<SysTenantPackage> buildQueryWrapper(SysTenantPackageBo bo) {
        LambdaQueryWrapper<SysTenantPackage> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(bo.getPackageName()), SysTenantPackage::getPackageName, bo.getPackageName());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), SysTenantPackage::getStatus, bo.getStatus());
        lqw.orderByAsc(SysTenantPackage::getPackageId);
        return lqw;
    }

    /**
     * 新增租户套餐
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(SysTenantPackageBo bo) {
        // 员工默认菜单必须落在管理员可用菜单内
        checkEmployeeMenuSubset(bo.getMenuIds(), bo.getEmployeeMenuIds());
        SysTenantPackage add = MapstructUtils.convert(bo, SysTenantPackage.class);
        // 保存菜单id
        List<Long> menuIds = ArrayUtil.isEmpty(bo.getMenuIds()) ? List.of() : Arrays.asList(bo.getMenuIds());
        add.setMenuIds(StringUtils.joinComma(menuIds));
        add.setEmployeeMenuIds(ArrayUtil.isEmpty(bo.getEmployeeMenuIds()) ? "" : StringUtils.joinComma(bo.getEmployeeMenuIds()));
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setPackageId(add.getPackageId());
        }
        return flag;
    }

    /**
     * 修改租户套餐
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(SysTenantPackageBo bo) {
        SysTenantPackage update = MapstructUtils.convert(bo, SysTenantPackage.class);
        // 部分更新：未提供的菜单字段保持数据库原值，避免旧表单/缺字段请求把套餐权限静默清空
        boolean menuProvided = ObjectUtil.isNotNull(bo.getMenuIds());
        boolean employeeProvided = ObjectUtil.isNotNull(bo.getEmployeeMenuIds());
        if (menuProvided || employeeProvided) {
            SysTenantPackage exist = baseMapper.selectById(bo.getPackageId());
            if (ObjectUtil.isNull(exist)) {
                throw new ServiceException("租户套餐不存在，无法修改菜单");
            }
            // 用“请求值 + 数据库存量”合并出最终菜单，再校验员工菜单仍是套餐菜单子集
            Long[] effectiveMenuIds = menuProvided ? bo.getMenuIds() : splitMenuIds(exist.getMenuIds());
            Long[] effectiveEmployeeIds = employeeProvided ? bo.getEmployeeMenuIds() : splitMenuIds(exist.getEmployeeMenuIds());
            checkEmployeeMenuSubset(effectiveMenuIds, effectiveEmployeeIds);
            if (menuProvided) {
                update.setMenuIds(StringUtils.joinComma(Arrays.asList(bo.getMenuIds())));
            }
            if (employeeProvided) {
                update.setEmployeeMenuIds(StringUtils.joinComma(Arrays.asList(bo.getEmployeeMenuIds())));
            }
        }
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 把逗号分隔的菜单id串还原为数组
     *
     * @param menuIds 菜单id串，可空
     * @return 菜单id数组
     */
    private Long[] splitMenuIds(String menuIds) {
        return StringUtils.splitTo(menuIds, Convert::toLong).toArray(new Long[0]);
    }

    /**
     * 校验套餐名称是否唯一
     */
    @Override
    public boolean checkPackageNameUnique(SysTenantPackageBo bo) {
        boolean exist = baseMapper.exists(new LambdaQueryWrapper<SysTenantPackage>()
            .eq(SysTenantPackage::getPackageName, bo.getPackageName())
            .ne(ObjectUtil.isNotNull(bo.getPackageId()), SysTenantPackage::getPackageId, bo.getPackageId()));
        return !exist;
    }

    /**
     * 修改套餐状态
     *
     * @param bo 套餐信息
     * @return 结果
     */
    @Override
    public int updatePackageStatus(SysTenantPackageBo bo) {
        SysTenantPackage tenantPackage = MapstructUtils.convert(bo, SysTenantPackage.class);
        return baseMapper.updateById(tenantPackage);
    }

    /**
     * 批量删除租户套餐
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if(isValid){
            boolean exists = tenantMapper.exists(new LambdaQueryWrapper<SysTenant>().in(SysTenant::getPackageId, ids));
            if (exists) {
                throw new ServiceException("租户套餐已被使用");
            }
        }
        return baseMapper.deleteByIds(ids) > 0;
    }

    /**
     * 校验员工默认菜单是套餐菜单的子集
     *
     * @param menuIds         套餐管理员菜单
     * @param employeeMenuIds 员工默认菜单
     */
    public void checkEmployeeMenuSubset(Long[] menuIds, Long[] employeeMenuIds) {
        if (ArrayUtil.isEmpty(employeeMenuIds)) {
            return;
        }
        Set<Long> allowed = ArrayUtil.isEmpty(menuIds) ? Set.of() : new HashSet<>(Arrays.asList(menuIds));
        for (Long employeeMenuId : employeeMenuIds) {
            if (!allowed.contains(employeeMenuId)) {
                throw new ServiceException("员工默认菜单不能超出套餐菜单范围");
            }
        }
    }

    /**
     * 校验菜单是否都包含在套餐范围内
     *
     * @param packageId 套餐ID
     * @param menuIds   待校验的菜单ID
     */
    @Override
    public void checkMenuIdsWithinPackage(Long packageId, Collection<Long> menuIds) {
        if (CollUtil.isEmpty(menuIds)) {
            return;
        }
        SysTenantPackage tenantPackage = baseMapper.selectById(packageId);
        if (ObjectUtil.isNull(tenantPackage)) {
            throw new ServiceException("租户套餐不存在，无法分配菜单");
        }
        Set<Long> allowed = new HashSet<>(StringUtils.splitTo(tenantPackage.getMenuIds(), Convert::toLong));
        for (Long menuId : menuIds) {
            if (!allowed.contains(menuId)) {
                throw new ServiceException("菜单超出套餐范围，无权分配");
            }
        }
    }
}
