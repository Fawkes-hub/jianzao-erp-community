package org.erp.print.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.erp.print.domain.bo.PrintTemplateBo;
import org.erp.print.domain.vo.PrintTemplateVo;
import org.erp.print.service.IPrintService;
import org.erp.print.service.IPrintTemplateService;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 打印模板管理 + 渲染（DOC/12 §9）
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/erp/print")
public class PrintController extends BaseController {

    private final IPrintTemplateService templateService;
    private final IPrintService printService;

    /**
     * 模板列表
     */
    @SaCheckPermission("erp:print:template:list")
    @GetMapping("/templates")
    public TableDataInfo<PrintTemplateVo> templates(@RequestParam(required = false) String scene,
                                                    @RequestParam(required = false) String keyword,
                                                    PageQuery pageQuery) {
        return templateService.selectPage(scene, keyword, pageQuery);
    }

    /**
     * 模板详情
     */
    @SaCheckPermission("erp:print:template:list")
    @GetMapping("/templates/{id}")
    public R<PrintTemplateVo> template(@PathVariable Long id) {
        return R.ok(templateService.selectById(id));
    }

    /**
     * 新增模板
     */
    @SaCheckPermission("erp:print:template:edit")
    @PostMapping("/templates")
    public R<PrintTemplateVo> add(@Valid @RequestBody PrintTemplateBo bo) {
        return R.ok(templateService.insert(bo));
    }

    /**
     * 编辑模板（version+1）
     */
    @SaCheckPermission("erp:print:template:edit")
    @PutMapping("/templates/{id}")
    public R<PrintTemplateVo> edit(@PathVariable Long id, @Valid @RequestBody PrintTemplateBo bo) {
        return R.ok(templateService.update(id, bo));
    }

    /**
     * 停用模板（软删除保留审计）
     */
    @SaCheckPermission("erp:print:template:edit")
    @DeleteMapping("/templates/{id}")
    public R<Void> disable(@PathVariable Long id) {
        templateService.disable(id);
        return R.ok();
    }

    /**
     * 设为场景默认
     */
    @SaCheckPermission("erp:print:template:edit")
    @PostMapping("/templates/{id}/default")
    public R<Void> setDefault(@PathVariable Long id) {
        templateService.setDefault(id);
        return R.ok();
    }

    /**
     * 预览：用示例数据渲染编辑中的模板内容，直接返回 HTML（与渲染接口一致，前端按纯文本透传）
     */
    @SaCheckPermission("erp:print:template:preview")
    @PostMapping(value = "/preview", produces = MediaType.TEXT_HTML_VALUE)
    public String preview(@RequestParam String scene,
                          @RequestBody String content,
                          @RequestParam(required = false) Long customerId,
                          @RequestParam(defaultValue = "false") boolean showProfit) {
        return printService.preview(scene, content, customerId, showProfit);
    }

    /**
     * 送货单渲染（copies 为打印份数/联单数，默认 1）
     */
    @SaCheckPermission("erp:print:render")
    @GetMapping(value = "/SALE_DELIVERY/{saleId}", produces = MediaType.TEXT_HTML_VALUE)
    public String saleDelivery(@PathVariable Long saleId,
                               @RequestParam(required = false) Long templateId,
                               @RequestParam(defaultValue = "false") boolean showProfit,
                               @RequestParam(defaultValue = "1") int copies) {
        return printService.renderSaleDelivery(saleId, templateId, showProfit, copies);
    }

    /**
     * 对账单渲染
     */
    @SaCheckPermission("erp:print:render")
    @GetMapping(value = "/STATEMENT", produces = MediaType.TEXT_HTML_VALUE)
    public String statement(@RequestParam Long customerId,
                            @RequestParam(required = false) String month,
                            @RequestParam(required = false) Long templateId) {
        return printService.renderStatement(customerId, month, templateId);
    }

    /**
     * 收款单渲染
     */
    @SaCheckPermission("erp:print:render")
    @GetMapping(value = "/RECEIPT/{receiptId}", produces = MediaType.TEXT_HTML_VALUE)
    public String receipt(@PathVariable Long receiptId,
                          @RequestParam(required = false) Long templateId) {
        return printService.renderReceipt(receiptId, templateId);
    }

    /**
     * 付款单渲染
     */
    @SaCheckPermission("erp:print:render")
    @GetMapping(value = "/PAYMENT/{paymentId}", produces = MediaType.TEXT_HTML_VALUE)
    public String payment(@PathVariable Long paymentId,
                          @RequestParam(required = false) Long templateId) {
        return printService.renderPayment(paymentId, templateId);
    }
}
