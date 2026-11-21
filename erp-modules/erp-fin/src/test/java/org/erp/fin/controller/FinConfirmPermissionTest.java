package org.erp.fin.controller;

import cn.dev33.satoken.stp.StpUtil;
import org.erp.fin.domain.bo.FinOtherFlowBo;
import org.erp.fin.domain.bo.FinPaymentBo;
import org.erp.fin.domain.bo.FinReceiptBo;
import org.erp.fin.service.IFinOtherFlowService;
import org.erp.fin.service.IFinPaymentService;
import org.erp.fin.service.IFinReceiptService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

/**
 * 财务单据「保存并确认」权限校验测试
 * <p>
 * 新增/编辑接口携带 confirm=true 时会直接核销往来，必须额外校验确认权限，
 * 防止仅有 add/edit 权限的角色绕过审核直接动账。
 */
@Tag("dev")
class FinConfirmPermissionTest {

    @Test
    void 收款单新增与编辑携带确认时校验确认权限() {
        FinReceiptController controller = new FinReceiptController(mock(IFinReceiptService.class));
        FinReceiptBo bo = new FinReceiptBo();
        bo.setConfirm(true);

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            controller.add(bo);
            controller.edit(1L, bo);
            stp.verify(() -> StpUtil.checkPermission("erp:finance:receipt:confirm"), times(2));
        }
    }

    @Test
    void 付款单新增与编辑携带确认时校验确认权限() {
        FinPaymentController controller = new FinPaymentController(mock(IFinPaymentService.class));
        FinPaymentBo bo = new FinPaymentBo();
        bo.setConfirm(true);

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            controller.add(bo);
            controller.edit(1L, bo);
            stp.verify(() -> StpUtil.checkPermission("erp:finance:payment:confirm"), times(2));
        }
    }

    @Test
    void 其他收支新增与编辑携带确认时校验确认权限() {
        FinOtherFlowController controller = new FinOtherFlowController(mock(IFinOtherFlowService.class));
        FinOtherFlowBo bo = new FinOtherFlowBo();
        bo.setConfirm(true);

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            controller.add(bo);
            controller.edit(1L, bo);
            stp.verify(() -> StpUtil.checkPermission("erp:finance:other-flow:confirm"), times(2));
        }
    }

    @Test
    void 仅保存草稿不校验确认权限() {
        FinReceiptController controller = new FinReceiptController(mock(IFinReceiptService.class));
        FinReceiptBo bo = new FinReceiptBo();
        bo.setConfirm(false);

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            controller.add(bo);
            controller.edit(1L, bo);
            stp.verifyNoInteractions();
        }
    }
}
