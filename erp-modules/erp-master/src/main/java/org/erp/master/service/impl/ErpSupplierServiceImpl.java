package org.erp.master.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.erp.master.domain.ErpSupplier;
import org.erp.master.domain.bo.ErpSupplierBo;
import org.erp.master.domain.vo.ErpSupplierVo;
import org.erp.master.mapper.ErpSupplierMapper;
import org.erp.master.service.IErpSupplierService;
import org.erp.master.util.CodeGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 供应商服务实现
 */
@RequiredArgsConstructor
@Service
public class ErpSupplierServiceImpl implements IErpSupplierService {

    private final ErpSupplierMapper baseMapper;

    @Override
    public TableDataInfo<ErpSupplierVo> selectPage(String keyword, Integer status, PageQuery pageQuery) {
        LambdaQueryWrapper<ErpSupplier> lqw = buildQuery(keyword, status);
        Page<ErpSupplierVo> page = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(page);
    }

    @Override
    public List<ErpSupplierVo> selectList(String keyword) {
        return baseMapper.selectVoList(buildQuery(keyword, null));
    }

    private LambdaQueryWrapper<ErpSupplier> buildQuery(String keyword, Integer status) {
        LambdaQueryWrapper<ErpSupplier> lqw = new LambdaQueryWrapper<>();
        lqw.eq(status != null, ErpSupplier::getStatus, status);
        if (StringUtils.isNotBlank(keyword)) {
            lqw.and(w -> w.like(ErpSupplier::getName, keyword)
                .or().like(ErpSupplier::getCode, keyword)
                .or().like(ErpSupplier::getPhone, keyword));
        }
        lqw.orderByDesc(ErpSupplier::getId);
        return lqw;
    }

    @Override
    public ErpSupplierVo selectById(Long id) {
        ErpSupplierVo vo = baseMapper.selectVoById(id);
        if (vo == null) {
            throw new ServiceException("供应商不存在");
        }
        return vo;
    }

    @Override
    public void insert(ErpSupplierBo bo) {
        if (StringUtils.isBlank(bo.getCode())) {
            bo.setCode(CodeGenerator.generate(bo.getName(), "GYS", code -> codeExists(bo.getId(), code)));
        }
        checkCodeUnique(bo.getId(), bo.getCode());
        baseMapper.insert(MapstructUtils.convert(bo, ErpSupplier.class));
    }

    @Override
    public void update(ErpSupplierBo bo) {
        if (bo.getId() == null) {
            throw new ServiceException("供应商ID不能为空");
        }
        if (StringUtils.isBlank(bo.getCode())) {
            ErpSupplier existing = baseMapper.selectById(bo.getId());
            if (existing == null) {
                throw new ServiceException("供应商不存在");
            }
            bo.setCode(existing.getCode());
        }
        checkCodeUnique(bo.getId(), bo.getCode());
        baseMapper.updateById(MapstructUtils.convert(bo, ErpSupplier.class));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByIds(List<Long> ids) {
        // 删前校验每个 id 在当前租户下确实存在：跨租户 id 会被租户条件静默跳过，
        // 若不校验则接口返回「操作成功」但实际未删除，用户会误判
        for (Long id : ids) {
            if (baseMapper.selectById(id) == null) {
                throw new ServiceException("供应商不存在或不属于当前租户：" + id, 40905);
            }
        }
        baseMapper.deleteByIds(ids);
    }

    private boolean codeExists(Long id, String code) {
        Long count = baseMapper.selectCount(new LambdaQueryWrapper<ErpSupplier>()
            .eq(ErpSupplier::getCode, code)
            .ne(id != null, ErpSupplier::getId, id));
        return count != null && count > 0;
    }

    private void checkCodeUnique(Long id, String code) {
        if (codeExists(id, code)) {
            throw new ServiceException("编码已存在：" + code, 40903);
        }
    }
}
