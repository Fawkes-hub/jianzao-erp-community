package org.erp.fin.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.erp.fin.domain.bo.FinAccountBo;
import org.erp.fin.domain.vo.FinAccountFlowVo;
import org.erp.fin.domain.vo.FinAccountVo;
import org.erp.fin.service.IFinAccountService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 结算账户（DOC/25 §3.4）
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/erp/finance/accounts")
public class FinAccountController extends BaseController {

    private final IFinAccountService accountService;

    @SaCheckPermission("erp:finance:account:list")
    @GetMapping("/list")
    public TableDataInfo<FinAccountVo> list(@RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String type,
                                            @RequestParam(required = false) Integer status,
                                            PageQuery pageQuery) {
        return accountService.selectPage(keyword, type, status, pageQuery);
    }

    @SaCheckPermission("erp:finance:account:query")
    @GetMapping("/{id}")
    public R<FinAccountVo> getInfo(@PathVariable Long id) {
        return R.ok(accountService.selectById(id));
    }

    @SaCheckPermission("erp:finance:account:add")
    @Log(title = "结算账户", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping
    public R<Void> add(@Valid @RequestBody FinAccountBo bo) {
        accountService.insert(bo);
        return R.ok();
    }

    @SaCheckPermission("erp:finance:account:edit")
    @Log(title = "结算账户", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping("/{id}")
    public R<Void> edit(@PathVariable Long id, @Valid @RequestBody FinAccountBo bo) {
        bo.setId(id);
        accountService.update(bo);
        return R.ok();
    }

    @SaCheckPermission("erp:finance:account:remove")
    @Log(title = "结算账户", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public R<Void> remove(@PathVariable Long id) {
        accountService.deleteById(id);
        return R.ok();
    }

    /**
     * 启用账户下拉：收付款/其他收支页面复用，权限放宽到收付单列表权限。
     */
    @SaCheckPermission("erp:finance:receipt:list")
    @GetMapping("/options")
    public R<List<FinAccountVo>> options() {
        return R.ok(accountService.options());
    }

    @SaCheckPermission("erp:finance:account:query")
    @GetMapping("/{id}/flows")
    public TableDataInfo<FinAccountFlowVo> flows(@PathVariable Long id, PageQuery pageQuery) {
        return accountService.flows(id, pageQuery);
    }
}
