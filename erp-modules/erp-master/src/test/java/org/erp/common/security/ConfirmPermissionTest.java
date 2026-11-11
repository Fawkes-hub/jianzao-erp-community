package org.erp.common.security;

import cn.dev33.satoken.stp.StpUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.mockito.Mockito.mockStatic;

/**
 * 单据「保存并确认」权限校验测试
 */
@Tag("dev")
class ConfirmPermissionTest {

    @Test
    @DisplayName("confirm=false 不校验审核权限")
    void skipsWhenConfirmFalse() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            ConfirmPermission.check("erp:sale:audit", false);
            stp.verifyNoInteractions();
        }
    }

    @Test
    @DisplayName("confirm=true 校验审核权限")
    void checksWhenConfirmTrue() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            ConfirmPermission.check("erp:sale:audit", true);
            stp.verify(() -> StpUtil.checkPermission("erp:sale:audit"));
        }
    }
}
