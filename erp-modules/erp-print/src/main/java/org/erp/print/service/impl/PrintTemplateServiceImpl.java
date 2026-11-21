package org.erp.print.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.erp.master.domain.ErpCustomer;
import org.erp.master.mapper.ErpCustomerMapper;
import org.erp.print.domain.PrintTemplate;
import org.erp.print.domain.bo.PrintTemplateBo;
import org.erp.print.domain.vo.PrintTemplateVo;
import org.erp.print.mapper.PrintTemplateMapper;
import org.erp.print.service.IPrintTemplateService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 打印模板服务实现
 *
 * 规则（DOC/14）：code 租户内唯一；保存清洗 + version+1；停用不删除；
 * 渲染解析顺序 客户专属 → 租户默认 → null（内置兜底）。
 */
@RequiredArgsConstructor
@Service
public class PrintTemplateServiceImpl implements IPrintTemplateService {

    private static final String ENGINE_THYMELEAF = "THYMELEAF";

    private final PrintTemplateMapper templateMapper;
    private final ErpCustomerMapper customerMapper;
    private final TemplateSanitizer sanitizer;

    @Override
    public TableDataInfo<PrintTemplateVo> selectPage(String scene, String keyword, PageQuery pageQuery) {
        LambdaQueryWrapper<PrintTemplate> lqw = new LambdaQueryWrapper<>();
        lqw.eq(scene != null && !scene.isBlank(), PrintTemplate::getScene, scene);
        lqw.like(keyword != null && !keyword.isBlank(), PrintTemplate::getName, keyword);
        lqw.or(keyword != null && !keyword.isBlank(), w -> w.like(PrintTemplate::getCode, keyword));
        lqw.orderByDesc(PrintTemplate::getId);
        Page<PrintTemplate> page = templateMapper.selectPage(pageQuery.build(), lqw);
        Page<PrintTemplateVo> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::toVo).toList());
        return TableDataInfo.build(voPage);
    }

    @Override
    public PrintTemplateVo selectById(Long id) {
        PrintTemplate row = templateMapper.selectById(id);
        if (row == null) {
            throw new ServiceException("打印模板不存在：" + id);
        }
        return toVo(row);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PrintTemplateVo insert(PrintTemplateBo bo) {
        String content = sanitizer.clean(bo.getContent());
        validateScene(bo.getScene());
        Long exists = templateMapper.selectCount(new LambdaQueryWrapper<PrintTemplate>()
            .eq(PrintTemplate::getCode, bo.getCode()));
        if (exists != null && exists > 0) {
            throw new ServiceException("模板编码已存在：" + bo.getCode(), 40902);
        }
        PrintTemplate row = new PrintTemplate();
        applyBo(row, bo);
        row.setContent(content);
        row.setEngine(ENGINE_THYMELEAF);
        row.setVersion(1);
        if (row.getIsDefault() != null && row.getIsDefault() == 1) {
            clearDefault(bo.getScene());
        }
        templateMapper.insert(row);
        return toVo(row);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PrintTemplateVo update(Long id, PrintTemplateBo bo) {
        PrintTemplate row = templateMapper.selectById(id);
        if (row == null) {
            throw new ServiceException("打印模板不存在：" + id);
        }
        String content = sanitizer.clean(bo.getContent());
        validateScene(bo.getScene());
        Long exists = templateMapper.selectCount(new LambdaQueryWrapper<PrintTemplate>()
            .eq(PrintTemplate::getCode, bo.getCode())
            .ne(PrintTemplate::getId, id));
        if (exists != null && exists > 0) {
            throw new ServiceException("模板编码已存在：" + bo.getCode(), 40902);
        }
        applyBo(row, bo);
        row.setContent(content);
        row.setVersion(row.getVersion() == null ? 1 : row.getVersion() + 1);
        if (row.getIsDefault() != null && row.getIsDefault() == 1) {
            clearDefault(bo.getScene());
        }
        templateMapper.updateById(row);
        return toVo(row);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disable(Long id) {
        PrintTemplate row = templateMapper.selectById(id);
        if (row == null) {
            throw new ServiceException("打印模板不存在：" + id);
        }
        row.setStatus(0);
        templateMapper.updateById(row);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long id) {
        PrintTemplate row = templateMapper.selectById(id);
        if (row == null) {
            throw new ServiceException("打印模板不存在：" + id);
        }
        row.setIsDefault(1);
        row.setStatus(1);
        clearDefault(row.getScene());
        templateMapper.updateById(row);
    }

    @Override
    public PrintTemplate resolve(Long templateId, String scene, Long customerId) {
        if (templateId != null) {
            PrintTemplate row = templateMapper.selectById(templateId);
            if (row == null || row.getStatus() == null || row.getStatus() != 1) {
                throw new ServiceException("指定打印模板不存在或已停用：" + templateId);
            }
            return row;
        }
        if (customerId != null) {
            List<PrintTemplate> rows = templateMapper.selectList(new LambdaQueryWrapper<PrintTemplate>()
                .eq(PrintTemplate::getScene, scene)
                .eq(PrintTemplate::getCustomerId, customerId)
                .eq(PrintTemplate::getStatus, 1)
                .orderByDesc(PrintTemplate::getId)
                .last("limit 1"));
            if (!rows.isEmpty()) {
                return rows.get(0);
            }
        }
        List<PrintTemplate> defaults = templateMapper.selectList(new LambdaQueryWrapper<PrintTemplate>()
            .eq(PrintTemplate::getScene, scene)
            .eq(PrintTemplate::getIsDefault, 1)
            .eq(PrintTemplate::getStatus, 1)
            .orderByDesc(PrintTemplate::getId)
            .last("limit 1"));
        return defaults.isEmpty() ? null : defaults.get(0);
    }

    @Override
    public List<PrintTemplate> listEnabled(String scene) {
        return templateMapper.selectList(new LambdaQueryWrapper<PrintTemplate>()
            .eq(PrintTemplate::getScene, scene)
            .eq(PrintTemplate::getStatus, 1)
            .orderByDesc(PrintTemplate::getIsDefault)
            .orderByDesc(PrintTemplate::getId));
    }

    private void applyBo(PrintTemplate row, PrintTemplateBo bo) {
        row.setCode(bo.getCode());
        row.setName(bo.getName());
        row.setScene(bo.getScene());
        row.setCustomerId(bo.getCustomerId());
        row.setVariablesJson(bo.getVariablesJson());
        row.setPaper(bo.getPaper() == null || bo.getPaper().isBlank() ? "A4" : bo.getPaper());
        row.setIsDefault(bo.getIsDefault() == null ? 0 : bo.getIsDefault());
        row.setStatus(bo.getStatus() == null ? 1 : bo.getStatus());
    }

    private void clearDefault(String scene) {
        List<PrintTemplate> defaults = templateMapper.selectList(new LambdaQueryWrapper<PrintTemplate>()
            .eq(PrintTemplate::getScene, scene)
            .eq(PrintTemplate::getIsDefault, 1));
        for (PrintTemplate d : defaults) {
            d.setIsDefault(0);
            templateMapper.updateById(d);
        }
    }

    private void validateScene(String scene) {
        List<String> allowed = List.of("SALE_DELIVERY", "STATEMENT", "STATEMENT_RANGE", "PURCHASE_IN",
            "RECEIPT", "PAYMENT");
        if (!allowed.contains(scene)) {
            throw new ServiceException("不支持的场景：" + scene);
        }
    }

    private PrintTemplateVo toVo(PrintTemplate row) {
        PrintTemplateVo vo = new PrintTemplateVo();
        vo.setId(row.getId());
        vo.setCode(row.getCode());
        vo.setName(row.getName());
        vo.setScene(row.getScene());
        vo.setCustomerId(row.getCustomerId());
        if (row.getCustomerId() != null) {
            ErpCustomer c = customerMapper.selectById(row.getCustomerId());
            vo.setCustomerName(c == null ? null : c.getName());
        }
        vo.setEngine(row.getEngine());
        vo.setVariablesJson(row.getVariablesJson());
        vo.setContent(row.getContent());
        vo.setPaper(row.getPaper());
        vo.setVersion(row.getVersion());
        vo.setIsDefault(row.getIsDefault());
        vo.setStatus(row.getStatus());
        vo.setCreateTime(row.getCreateTime());
        return vo;
    }
}
