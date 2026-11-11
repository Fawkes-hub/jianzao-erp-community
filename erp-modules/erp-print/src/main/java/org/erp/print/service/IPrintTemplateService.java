package org.erp.print.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.erp.print.domain.PrintTemplate;
import org.erp.print.domain.bo.PrintTemplateBo;
import org.erp.print.domain.vo.PrintTemplateVo;

import java.util.List;

/**
 * 打印模板服务（DOC/14 §2/§12、DOC/12 §9）
 */
public interface IPrintTemplateService {

    /**
     * 模板分页。
     */
    TableDataInfo<PrintTemplateVo> selectPage(String scene, String keyword, PageQuery pageQuery);

    /**
     * 模板详情。
     */
    PrintTemplateVo selectById(Long id);

    /**
     * 新增模板：jsoup 白名单清洗；code 租户内唯一；is_default=1 时清除同场景其他默认。
     */
    PrintTemplateVo insert(PrintTemplateBo bo);

    /**
     * 编辑模板：content 清洗后 version+1；编辑同时支持停用/启用（status 字段）。
     */
    PrintTemplateVo update(Long id, PrintTemplateBo bo);

    /**
     * 停用（软删除，保留审计）。
     */
    void disable(Long id);

    /**
     * 设为该场景默认（同场景其余取消默认）。
     */
    void setDefault(Long id);

    /**
     * 渲染取模板：templateId 优先；否则 scene+customerId 专属 → scene 默认 → null（调用方用内置兜底）。
     */
    PrintTemplate resolve(Long templateId, String scene, Long customerId);

    /**
     * 同场景全部启用模板（内置兜底选择用）。
     */
    List<PrintTemplate> listEnabled(String scene);
}
