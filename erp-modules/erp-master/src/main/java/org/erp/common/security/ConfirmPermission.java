package org.erp.common.security;

import cn.dev33.satoken.stp.StpUtil;

/**
 * 单据「保存并确认」权限校验
 * <p>
 * 新增/编辑接口若携带 confirm=true，会在同一事务内直接确认并产生库存、往来等副作用，
 * 必须同时具备对应单据的审核权限，避免仅有新增/编辑权限的角色绕过审核直接动账。
 *
 * @author fawkes
 */
public final class ConfirmPermission {

    private ConfirmPermission() {
    }

    /**
     * 校验直接确认权限
     *
     * @param auditPermission 单据审核权限码
     * @param confirm         是否保存后立即确认
     */
    public static void check(String auditPermission, Boolean confirm) {
        if (Boolean.TRUE.equals(confirm)) {
            StpUtil.checkPermission(auditPermission);
        }
    }
}
