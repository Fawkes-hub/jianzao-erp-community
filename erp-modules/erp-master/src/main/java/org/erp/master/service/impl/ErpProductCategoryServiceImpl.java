package org.erp.master.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.erp.master.domain.ErpProductCategory;
import org.erp.master.domain.bo.ErpProductCategoryBo;
import org.erp.master.domain.vo.ErpProductCategoryVo;
import org.erp.master.mapper.ErpProductCategoryMapper;
import org.erp.master.service.IErpProductCategoryService;
import org.erp.master.util.CodeGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 商品分类服务实现
 */
@RequiredArgsConstructor
@Service
public class ErpProductCategoryServiceImpl implements IErpProductCategoryService {

    private final ErpProductCategoryMapper baseMapper;

    @Override
    public TableDataInfo<ErpProductCategoryVo> selectPage(String keyword, Integer status, PageQuery pageQuery) {
        Page<ErpProductCategoryVo> page = baseMapper.selectVoPage(pageQuery.build(), buildQuery(keyword, status));
        return TableDataInfo.build(page);
    }

    @Override
    public List<ErpProductCategoryVo> selectList(String keyword) {
        return baseMapper.selectVoList(buildQuery(keyword, null));
    }

    /**
     * 分类查询条件：排序优先，其次按 id 保证顺序稳定（折叠分组顺序依赖该顺序）。
     *
     * @param keyword 编码/名称模糊匹配
     * @param status  状态过滤，null=不过滤
     * @return 查询条件
     */
    private LambdaQueryWrapper<ErpProductCategory> buildQuery(String keyword, Integer status) {
        LambdaQueryWrapper<ErpProductCategory> lqw = new LambdaQueryWrapper<>();
        lqw.eq(status != null, ErpProductCategory::getStatus, status);
        if (StringUtils.isNotBlank(keyword)) {
            lqw.and(w -> w.like(ErpProductCategory::getName, keyword)
                .or().like(ErpProductCategory::getCode, keyword));
        }
        lqw.orderByAsc(ErpProductCategory::getSort).orderByAsc(ErpProductCategory::getId);
        return lqw;
    }

    @Override
    public ErpProductCategoryVo selectById(Long id) {
        ErpProductCategoryVo vo = baseMapper.selectVoById(id);
        if (vo == null) {
            throw new ServiceException("分类不存在");
        }
        return vo;
    }

    @Override
    public void insert(ErpProductCategoryBo bo) {
        if (StringUtils.isBlank(bo.getCode())) {
            bo.setCode(CodeGenerator.generate(bo.getName(), "FL", this::codeOccupied));
        }
        checkCodeAvailable(bo.getId(), bo.getCode());
        baseMapper.insert(MapstructUtils.convert(bo, ErpProductCategory.class));
    }

    @Override
    public void update(ErpProductCategoryBo bo) {
        if (bo.getId() == null) {
            throw new ServiceException("分类ID不能为空");
        }
        if (StringUtils.isBlank(bo.getCode())) {
            ErpProductCategory existing = baseMapper.selectById(bo.getId());
            if (existing == null) {
                throw new ServiceException("分类不存在");
            }
            bo.setCode(existing.getCode());
        }
        checkCodeAvailable(bo.getId(), bo.getCode());
        baseMapper.updateById(MapstructUtils.convert(bo, ErpProductCategory.class));
    }

    private boolean codeExists(Long id, String code) {
        Long count = baseMapper.selectCount(new LambdaQueryWrapper<ErpProductCategory>()
            .eq(ErpProductCategory::getCode, code)
            .ne(id != null, ErpProductCategory::getId, id));
        return count != null && count > 0;
    }

    /**
     * 编码占用判定（编码自动生成用）：逻辑删除的记录仍占用唯一键 (tenant_id, code)，需一并避开。
     *
     * @param code 编码
     * @return true=已占用
     */
    private boolean codeOccupied(String code) {
        return codeExists(null, code) || baseMapper.selectDeletedIdByCode(code) != null;
    }

    /**
     * 校验编码可用：已删除记录占用时给出明确提示，避免落库时唯一键冲突报 500。
     *
     * @param id   当前记录 ID（新增为 null）
     * @param code 编码
     */
    private void checkCodeAvailable(Long id, String code) {
        if (codeExists(id, code)) {
            throw new ServiceException("编码已存在：" + code, 40903);
        }
        if (baseMapper.selectDeletedIdByCode(code) != null) {
            throw new ServiceException("编码已被已删除的分类占用，请改用其他编码：" + code, 40903);
        }
    }

    /**
     * 逻辑删除：商品引用该分类时 category_id 悬空，列表归「未分类」（DOC/26 §5.4），不阻断删除。
     *
     * @param ids 分类 ID 集合
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByIds(List<Long> ids) {
        // 删前校验每个 id 在当前租户下确实存在：跨租户 id 会被租户条件静默跳过，
        // 若不校验则接口返回「操作成功」但实际未删除，用户会误判
        for (Long id : ids) {
            if (baseMapper.selectById(id) == null) {
                throw new ServiceException("商品分类不存在或不属于当前租户：" + id, 40905);
            }
        }
        baseMapper.deleteByIds(ids);
    }
}
