package org.dromara.system.service.impl;

import cn.hutool.extra.spring.SpringUtil;
import io.github.linpeilie.Converter;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.system.domain.SysTenant;
import org.dromara.system.domain.SysTenantPackage;
import org.dromara.system.domain.bo.SysTenantPackageBo;
import org.dromara.system.mapper.SysTenantMapper;
import org.dromara.system.mapper.SysTenantPackageMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SysTenantPackageServiceImpl 单元测试
 * <p>
 * 覆盖员工默认菜单为套餐菜单子集、角色菜单不超出套餐范围两类校验。
 *
 * @author Fawkes
 */
@Tag("dev")
class SysTenantPackageServiceImplTest {

    private static Converter converter;

    private SysTenantPackageMapper packageMapper;
    private SysTenantPackageServiceImpl packageService;

    /**
     * MapstructUtils 静态依赖用 mock Converter 替换，便于验证保存接线
     */
    @BeforeAll
    static void initConverter() {
        try (MockedStatic<SpringUtil> spring = mockStatic(SpringUtil.class)) {
            spring.when(() -> SpringUtil.getBean(Converter.class)).thenReturn(mock(Converter.class));
            // 触发 MapstructUtils 静态初始化
            MapstructUtils.convert((Object) null, SysTenant.class);
        }
        // 多个测试类共享 MapstructUtils 的静态 Converter，必须对同一个 mock 打桩
        converter = sharedConverter();
    }

    /**
     * 读取 MapstructUtils 已初始化的 Converter
     */
    private static Converter sharedConverter() {
        try {
            java.lang.reflect.Field field = MapstructUtils.class.getDeclaredField("CONVERTER");
            field.setAccessible(true);
            return (Converter) field.get(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("无法获取 MapstructUtils Converter", e);
        }
    }

    @BeforeEach
    void setUp() {
        packageMapper = mock(SysTenantPackageMapper.class);
        packageService = new SysTenantPackageServiceImpl(packageMapper, mock(SysTenantMapper.class));
    }

    @Test
    void employeeMenuSubsetPassesWhenAllInPackage() {
        assertDoesNotThrow(() -> packageService.checkEmployeeMenuSubset(new Long[]{1L, 2L, 3L}, new Long[]{1L, 3L}));
    }

    @Test
    void employeeMenuOutsidePackageIsRejected() {
        ServiceException exception = assertThrows(ServiceException.class,
            () -> packageService.checkEmployeeMenuSubset(new Long[]{1L, 2L}, new Long[]{1L, 9L}));
        assertEquals("员工默认菜单不能超出套餐菜单范围", exception.getMessage());
    }

    @Test
    void emptyEmployeeMenuPassesButEmptyPackageRejectsAny() {
        // 未配置员工菜单时不校验
        assertDoesNotThrow(() -> packageService.checkEmployeeMenuSubset(new Long[]{1L}, null));
        // 套餐菜单为空时，任何员工菜单都越界
        assertThrows(ServiceException.class, () -> packageService.checkEmployeeMenuSubset(new Long[0], new Long[]{1L}));
    }

    @Test
    void menuIdsWithinPackagePasses() {
        when(packageMapper.selectById(100L)).thenReturn(tenantPackage("1,2,3", "1,2"));

        assertDoesNotThrow(() -> packageService.checkMenuIdsWithinPackage(100L, List.of(1L, 3L)));
        // 空菜单无需校验套餐
        assertDoesNotThrow(() -> packageService.checkMenuIdsWithinPackage(100L, List.of()));
    }

    @Test
    void menuIdsOutsidePackageIsRejected() {
        when(packageMapper.selectById(100L)).thenReturn(tenantPackage("1,2,3", "1,2"));

        ServiceException exception = assertThrows(ServiceException.class,
            () -> packageService.checkMenuIdsWithinPackage(100L, List.of(1L, 9L)));
        assertEquals("菜单超出套餐范围，无权分配", exception.getMessage());
    }

    @Test
    void missingPackageIsRejected() {
        when(packageMapper.selectById(100L)).thenReturn(null);

        ServiceException exception = assertThrows(ServiceException.class,
            () -> packageService.checkMenuIdsWithinPackage(100L, List.of(1L)));
        assertEquals("租户套餐不存在，无法分配菜单", exception.getMessage());
    }

    @Test
    void updateWithoutMenuFieldsKeepsExistingMenus() {
        SysTenantPackage update = new SysTenantPackage();
        when(converter.convert(any(SysTenantPackageBo.class), eq(SysTenantPackage.class))).thenReturn(update);
        when(packageMapper.updateById(update)).thenReturn(1);
        SysTenantPackageBo bo = new SysTenantPackageBo();
        bo.setPackageId(100L);
        bo.setPackageName("ERP-only");

        assertTrue(packageService.updateByBo(bo));
        // 未传菜单字段时不得把数据库里的套餐权限清空
        assertNull(update.getMenuIds());
        assertNull(update.getEmployeeMenuIds());
        verify(packageMapper, never()).selectById(any());
    }

    @Test
    void updateEmployeeMenuOutsideExistingPackageIsRejected() {
        when(converter.convert(any(SysTenantPackageBo.class), eq(SysTenantPackage.class))).thenReturn(new SysTenantPackage());
        when(packageMapper.selectById(100L)).thenReturn(tenantPackage("1,2", "1"));
        SysTenantPackageBo bo = new SysTenantPackageBo();
        bo.setPackageId(100L);
        bo.setEmployeeMenuIds(new Long[]{9L});

        ServiceException exception = assertThrows(ServiceException.class, () -> packageService.updateByBo(bo));
        assertEquals("员工默认菜单不能超出套餐菜单范围", exception.getMessage());
        verify(packageMapper, never()).updateById(any(SysTenantPackage.class));
    }

    @Test
    void updateMenusRejectsExistingEmployeeMenuOutOfNewSubset() {
        when(converter.convert(any(SysTenantPackageBo.class), eq(SysTenantPackage.class))).thenReturn(new SysTenantPackage());
        when(packageMapper.selectById(100L)).thenReturn(tenantPackage("1,2,3", "3"));
        SysTenantPackageBo bo = new SysTenantPackageBo();
        bo.setPackageId(100L);
        bo.setMenuIds(new Long[]{1L, 2L});

        // 只改管理员菜单时，存量员工菜单 3 已不在新子集内，必须拒绝
        ServiceException exception = assertThrows(ServiceException.class, () -> packageService.updateByBo(bo));
        assertEquals("员工默认菜单不能超出套餐菜单范围", exception.getMessage());
        verify(packageMapper, never()).updateById(any(SysTenantPackage.class));
    }

    /**
     * 构造套餐
     */
    private SysTenantPackage tenantPackage(String menuIds, String employeeMenuIds) {
        SysTenantPackage tenantPackage = new SysTenantPackage();
        tenantPackage.setPackageId(100L);
        tenantPackage.setPackageName("ERP-only");
        tenantPackage.setMenuIds(menuIds);
        tenantPackage.setEmployeeMenuIds(employeeMenuIds);
        return tenantPackage;
    }

}
