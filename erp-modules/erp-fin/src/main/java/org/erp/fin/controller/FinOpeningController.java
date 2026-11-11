package org.erp.fin.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.web.core.BaseController;
import org.erp.common.excel.ImportResult;
import org.erp.fin.domain.bo.FinOpeningBo;
import org.erp.fin.domain.vo.FinOpeningVo;
import org.erp.fin.excel.FinOpeningExcelService;
import org.erp.fin.service.IFinOpeningService;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 期初往来
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/erp/finance/openings")
public class FinOpeningController extends BaseController {

    private final IFinOpeningService openingService;
    private final FinOpeningExcelService excelService;

    @SaCheckPermission("erp:finance:list")
    @GetMapping("/list")
    public R<List<FinOpeningVo>> list(@RequestParam(required = false) String partyType) {
        return R.ok(openingService.listAll(partyType));
    }

    @SaCheckPermission("erp:finance:opening")
    @Log(title = "期初往来录入", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping
    public R<FinOpeningVo> save(@Valid @RequestBody FinOpeningBo bo) {
        return R.ok(openingService.save(bo));
    }

    @SaCheckPermission("erp:finance:opening")
    @Log(title = "期初往来确认", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/{id}/confirm")
    public R<Void> confirm(@PathVariable Long id) {
        openingService.confirm(id);
        return R.ok();
    }

    @SaCheckPermission("erp:finance:opening")
    @Log(title = "期初往来反确认", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/{id}/unconfirm")
    public R<Void> unconfirm(@PathVariable Long id) {
        openingService.unconfirm(id);
        return R.ok();
    }

    /**
     * 导出期初往来（全部，不分页）
     */
    @SaCheckPermission("erp:finance:opening:export")
    @Log(title = "期初往来导出", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(@RequestParam(required = false) String partyType, HttpServletResponse response) {
        excelService.export(partyType, response);
    }

    /**
     * 导入期初往来；仅保存 DRAFT，不自动确认
     */
    @SaCheckPermission("erp:finance:opening:import")
    @Log(title = "期初往来", businessType = BusinessType.IMPORT)
    @PostMapping(value = "/importData", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<ImportResult> importData(@RequestPart("file") MultipartFile file) throws IOException {
        try {
            return R.ok(excelService.importData(file));
        } catch (FinOpeningExcelService.ImportRollbackException e) {
            return R.ok(e.getResult());
        }
    }

    /**
     * 下载期初往来导入模板
     */
    @SaCheckPermission("erp:finance:opening:import")
    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response) {
        excelService.importTemplate(response);
    }
}
