package org.dromara.system.service.impl;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.domain.SysRole;
import org.dromara.system.domain.SysTenant;
import org.dromara.system.domain.SysTenantPackage;
import org.dromara.system.domain.bo.SysRoleBo;
import org.dromara.system.mapper.SysRoleDeptMapper;
import org.dromara.system.mapper.SysRoleMapper;
import org.dromara.system.mapper.SysRoleMenuMapper;
import org.dromara.system.mapper.SysTenantMapper;
import org.dromara.system.mapper.SysTenantPackageMapper;
import org.dromara.system.mapper.SysUserRoleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SysRoleServiceImpl 单元测试
 * <p>
 * 覆盖企业管理员保存角色时的套餐菜单上限校验（角色菜单不得超出套餐 menu_ids）。
 *
 * @author Fawkes
 */
@Tag("dev")
class SysRoleServiceImplTest {

    private SysTenantMapper tenantMapper;
    private SysTenantPackageMapper packageMapper;
    private SysRoleMapper roleMapper;
    private SysRoleServiceImpl roleService;

    @BeforeEach
    void setUp() {
        tenantMapper = mock(SysTenantMapper.class);
        packageMapper = mock(SysTenantPackageMapper.class);
        roleMapper = mock(SysRoleMapper.class);
        SysTenantPackageServiceImpl packageService = new SysTenantPackageServiceImpl(packageMapper, tenantMapper);
        roleService = new SysRoleServiceImpl(
            roleMapper,
            mock(SysRoleMenuMapper.class),
            mock(SysUserRoleMapper.class),
            mock(SysRoleDeptMapper.class),
            tenantMapper,
            packageService);
    }

    @Test
    void roleMenusInsidePackagePass() {
        when(tenantMapper.selectOne(any())).thenReturn(tenant(100L));
        when(packageMapper.selectById(100L)).thenReturn(tenantPackage("1,2,3"));

        assertDoesNotThrow(() -> roleService.checkRoleMenuScope(new Long[]{1L, 3L}, "100001"));
    }

    @Test
    void roleMenusOutsidePackageAreRejected() {
        when(tenantMapper.selectOne(any())).thenReturn(tenant(100L));
        when(packageMapper.selectById(100L)).thenReturn(tenantPackage("1,2,3"));

        ServiceException exception = assertThrows(ServiceException.class,
            () -> roleService.checkRoleMenuScope(new Long[]{1L, 9L}, "100001"));
        assertEquals("菜单超出套餐范围，无权分配", exception.getMessage());
    }

    @Test
    void unknownTenantIsRejected() {
        when(tenantMapper.selectOne(any())).thenReturn(null);

        ServiceException exception = assertThrows(ServiceException.class,
            () -> roleService.checkRoleMenuScope(new Long[]{1L}, "100001"));
        assertEquals("当前企业不存在，不允许分配菜单", exception.getMessage());
    }

    @Test
    void blankTenantIsRejectedButEmptyMenusSkipCheck() {
        ServiceException exception = assertThrows(ServiceException.class,
            () -> roleService.checkRoleMenuScope(new Long[]{1L}, null));
        assertEquals("无法确定当前企业，不允许分配菜单", exception.getMessage());

        // 不分配菜单时无需校验套餐
        assertDoesNotThrow(() -> roleService.checkRoleMenuScope(null, null));
        verify(tenantMapper, never()).selectOne(any());
    }

    @Test
    void insertRoleRejectsMenusOutsidePackageBeforePersisting() {
        when(tenantMapper.selectOne(any())).thenReturn(tenant(100L));
        when(packageMapper.selectById(100L)).thenReturn(tenantPackage("1,2,3"));
        SysRoleBo bo = new SysRoleBo();
        bo.setMenuIds(new Long[]{1L, 9L});

        try (MockedStatic<LoginHelper> helper = mockStatic(LoginHelper.class)) {
            helper.when(LoginHelper::getTenantId).thenReturn("100001");
            helper.when(LoginHelper::isSuperAdmin).thenReturn(false);

            ServiceException exception = assertThrows(ServiceException.class, () -> roleService.insertRole(bo));
            assertEquals("菜单超出套餐范围，无权分配", exception.getMessage());
        }
        // 越界请求不得落库
        verify(roleMapper, never()).insert(any(SysRole.class));
    }

    @Test
    void updateRoleRejectsMenusOutsidePackageBeforePersisting() {
        when(tenantMapper.selectOne(any())).thenReturn(tenant(100L));
        when(packageMapper.selectById(100L)).thenReturn(tenantPackage("1,2,3"));
        SysRoleBo bo = new SysRoleBo();
        bo.setRoleId(5L);
        bo.setMenuIds(new Long[]{9L});

        try (MockedStatic<LoginHelper> helper = mockStatic(LoginHelper.class)) {
            helper.when(LoginHelper::getTenantId).thenReturn("100001");
            helper.when(LoginHelper::isSuperAdmin).thenReturn(false);

            ServiceException exception = assertThrows(ServiceException.class, () -> roleService.updateRole(bo));
            assertEquals("菜单超出套餐范围，无权分配", exception.getMessage());
        }
        verify(roleMapper, never()).updateById(any(SysRole.class));
    }

    /**
     * 构造租户
     */
    private SysTenant tenant(Long packageId) {
        SysTenant tenant = new SysTenant();
        tenant.setTenantId("100001");
        tenant.setPackageId(packageId);
        return tenant;
    }

    /**
     * 构造套餐
     */
    private SysTenantPackage tenantPackage(String menuIds) {
        SysTenantPackage tenantPackage = new SysTenantPackage();
        tenantPackage.setPackageId(100L);
        tenantPackage.setMenuIds(menuIds);
        return tenantPackage;
    }

}
