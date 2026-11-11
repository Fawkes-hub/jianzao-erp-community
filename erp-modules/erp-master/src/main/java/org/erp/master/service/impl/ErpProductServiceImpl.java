package org.erp.master.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.erp.master.domain.ErpProduct;
import org.erp.master.domain.ErpProductRecipe;
import org.erp.master.domain.bo.ErpProductBo;
import org.erp.master.domain.bo.ErpRecipeItemBo;
import org.erp.master.domain.vo.ErpProductVo;
import org.erp.master.mapper.ErpProductMapper;
import org.erp.master.mapper.ErpProductRecipeMapper;
import org.erp.master.service.IErpProductService;
import org.erp.master.util.CodeGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * 产品/配比 服务实现
 */
@RequiredArgsConstructor
@Service
public class ErpProductServiceImpl implements IErpProductService {

    private final ErpProductMapper productMapper;
    private final ErpProductRecipeMapper recipeMapper;

    @Override
    public TableDataInfo<ErpProductVo> selectPage(String keyword, String type, Integer status, Long categoryId,
                                                  Long supplierId, PageQuery pageQuery) {
        Page<ErpProductVo> page = productMapper.selectVoPageDetail(pageQuery.build(), keyword, type, status, categoryId, supplierId);
        return TableDataInfo.build(page);
    }

    @Override
    public List<ErpProductVo> selectList(String keyword, String type, Long categoryId) {
        return productMapper.selectVoListDetail(keyword, type, null, categoryId, null);
    }

    @Override
    public ErpProductVo selectById(Long id) {
        ErpProductVo vo = productMapper.selectVoById(id);
        if (vo == null) {
            throw new ServiceException("产品不存在");
        }
        return vo;
    }

    @Override
    public Long insert(ErpProductBo bo) {
        checkStockLimit(bo);
        if (StringUtils.isBlank(bo.getCode())) {
            bo.setCode(CodeGenerator.generate(bo.getName(), "CP", code -> codeExists(bo.getId(), code)));
        }
        checkCodeUnique(bo.getId(), bo.getCode());
        ErpProduct entity = MapstructUtils.convert(bo, ErpProduct.class);
        // 成本单价为系统回写的「最近入库成本」，不接受前台写入（DOC/26 §9.4 / Q11）
        entity.setCostPrice(BigDecimal.ZERO);
        productMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public void update(ErpProductBo bo) {
        checkStockLimit(bo);
        if (bo.getId() == null) {
            throw new ServiceException("产品ID不能为空");
        }
        if (StringUtils.isBlank(bo.getCode())) {
            ErpProduct existing = productMapper.selectById(bo.getId());
            if (existing == null) {
                throw new ServiceException("产品不存在");
            }
            bo.setCode(existing.getCode());
        }
        checkCodeUnique(bo.getId(), bo.getCode());
        ErpProduct entity = MapstructUtils.convert(bo, ErpProduct.class);
        // 成本单价只由入库回写，编辑时置空让 MyBatis-Plus 跳过该列（DOC/26 §9.4 / Q11）
        entity.setCostPrice(null);
        // 分类/供应商允许清空：MyBatis-Plus 默认跳过 null 字段，此处显式置空，否则"取消分类"保存无效
        LambdaUpdateWrapper<ErpProduct> uw = new LambdaUpdateWrapper<ErpProduct>().eq(ErpProduct::getId, bo.getId());
        if (bo.getCategoryId() == null) {
            uw.set(ErpProduct::getCategoryId, null);
        }
        if (bo.getSupplierId() == null) {
            uw.set(ErpProduct::getSupplierId, null);
        }
        productMapper.update(entity, uw);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByIds(List<Long> ids) {
        for (Long id : ids) {
            // 删前校验 id 在当前租户下确实存在：跨租户 id 会被租户条件静默跳过，
            // 若不校验则接口返回「操作成功」但实际未删除，用户会误判
            if (productMapper.selectById(id) == null) {
                throw new ServiceException("产品不存在或不属于当前租户：" + id, 40905);
            }
            Long count = recipeMapper.selectCount(new LambdaQueryWrapper<ErpProductRecipe>()
                .eq(ErpProductRecipe::getProductId, id)
                .or().eq(ErpProductRecipe::getMaterialId, id));
            if (count != null && count > 0) {
                throw new ServiceException("产品存在配比引用，请先删除配比");
            }
        }
        productMapper.deleteByIds(ids);
    }

    /**
     * 上下限同时有值时校验 min ≤ max（DOC/25 §3.1，可空各自独立启用）。
     */
    private void checkStockLimit(ErpProductBo bo) {
        if (bo.getMinStock() != null && bo.getMaxStock() != null
            && bo.getMinStock().compareTo(bo.getMaxStock()) > 0) {
            throw new ServiceException("库存下限不能大于上限");
        }
    }

    private boolean codeExists(Long id, String code) {
        Long count = productMapper.selectCount(new LambdaQueryWrapper<ErpProduct>()
            .eq(ErpProduct::getCode, code)
            .ne(id != null, ErpProduct::getId, id));
        return count != null && count > 0;
    }

    private void checkCodeUnique(Long id, String code) {
        if (codeExists(id, code)) {
            throw new ServiceException("编码已存在：" + code, 40903);
        }
    }

    @Override
    public List<ErpRecipeItemBo> getRecipe(Long productId) {
        List<ErpProductRecipe> rows = recipeMapper.selectList(new LambdaQueryWrapper<ErpProductRecipe>()
            .eq(ErpProductRecipe::getProductId, productId)
            .orderByAsc(ErpProductRecipe::getId));
        return rows.stream().map(r -> {
            ErpRecipeItemBo bo = new ErpRecipeItemBo();
            bo.setMaterialId(r.getMaterialId());
            bo.setQtyPerUnit(r.getQtyPerUnit());
            return bo;
        }).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveRecipe(Long productId, List<ErpRecipeItemBo> items) {
        ErpProduct product = productMapper.selectById(productId);
        if (product == null) {
            throw new ServiceException("产品不存在");
        }
        if (CollUtil.isNotEmpty(items)) {
            LinkedHashSet<Long> seen = new LinkedHashSet<>();
            for (ErpRecipeItemBo item : items) {
                if (!seen.add(item.getMaterialId())) {
                    throw new ServiceException("配比中存在重复原料");
                }
                if (item.getMaterialId().equals(productId)) {
                    throw new ServiceException("成品不能作为自己的原料");
                }
                if (item.getQtyPerUnit() == null || item.getQtyPerUnit().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new ServiceException("用量必须大于0");
                }
            }
        }
        recipeMapper.delete(new LambdaQueryWrapper<ErpProductRecipe>()
            .eq(ErpProductRecipe::getProductId, productId));
        if (CollUtil.isNotEmpty(items)) {
            for (ErpRecipeItemBo item : items) {
                ErpProductRecipe row = new ErpProductRecipe();
                row.setProductId(productId);
                row.setMaterialId(item.getMaterialId());
                row.setQtyPerUnit(item.getQtyPerUnit());
                recipeMapper.insert(row);
            }
        }
    }

    /**
     * 回写「最近入库成本」：采购/生产/期初/其他入库确认时调用，产品资料只读展示（DOC/26 §9.4）。
     *
     * @param productId 产品 ID
     * @param costPrice 最近一次入库的核算成本
     */
    @Override
    public void updateCostPrice(Long productId, BigDecimal costPrice) {
        ErpProduct row = new ErpProduct();
        row.setId(productId);
        row.setCostPrice(costPrice);
        productMapper.updateById(row);
    }
}
