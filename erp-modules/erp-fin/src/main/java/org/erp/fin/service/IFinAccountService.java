package org.erp.fin.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.erp.fin.domain.bo.FinAccountBo;
import org.erp.fin.domain.vo.FinAccountFlowVo;
import org.erp.fin.domain.vo.FinAccountVo;

import java.util.List;

/**
 * 结算账户服务（DOC/25 §3.4/§3.5）
 */
public interface IFinAccountService {

    /**
     * 账户分页列表（含实时余额）。
     */
    TableDataInfo<FinAccountVo> selectPage(String keyword, String type, Integer status, PageQuery pageQuery);

    /**
     * 详情（含实时余额）。
     */
    FinAccountVo selectById(Long id);

    /**
     * 新增（可填期初余额）。同码已删档时复用该行重建。
     */
    void insert(FinAccountBo bo);

    /**
     * 修改：code 不可改；期初余额仅无动账时可改；余额≠0 拒绝停用。
     */
    void update(FinAccountBo bo);

    /**
     * 逻辑删除：名下存在任意状态（含草稿）收付款/其他收支单或余额≠0 时拒绝。
     */
    void deleteById(Long id);

    /**
     * 启用账户下拉（轻量不分页），供收付款/其他收支页面选择。
     */
    List<FinAccountVo> options();

    /**
     * 账户收支流水（收/付/其他收支 UNION，biz_date+id 倒序）。
     */
    TableDataInfo<FinAccountFlowVo> flows(Long accountId, PageQuery pageQuery);
}
