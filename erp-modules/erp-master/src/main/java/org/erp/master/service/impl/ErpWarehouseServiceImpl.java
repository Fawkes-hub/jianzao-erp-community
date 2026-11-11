package org.erp.master.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.erp.master.domain.ErpWarehouse;
import org.erp.master.domain.bo.ErpWarehouseBo;
import org.erp.master.domain.vo.ErpWarehouseVo;
import org.erp.master.mapper.ErpWarehouseMapper;
import org.erp.master.service.IErpWarehouseService;
import org.erp.master.util.CodeGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 仓库服务实现
 */
@RequiredArgsConstructor
@Service
public class ErpWarehouseServiceImpl implements IErpWarehouseService {

    private final ErpWarehouseMapper baseMapper;

    @Override
    public TableDataInfo<ErpWarehouseVo> selectPage(String keyword, Integer status, PageQuery pageQuery) {
        LambdaQueryWrapper<ErpWarehouse> lqw = buildQuery(keyword, status);
        Page<ErpWarehouseVo> page = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(page);
    }

    @Override
    public List<ErpWarehouseVo> selectList(String keyword) {
        return baseMapper.selectVoList(buildQuery(keyword, null));
    }

    private LambdaQueryWrapper<ErpWarehouse> buildQuery(String keyword, Integer status) {
        LambdaQueryWrapper<ErpWarehouse> lqw = new LambdaQueryWrapper<>();
        lqw.eq(status != null, ErpWarehouse::getStatus, status);
        if (StringUtils.isNotBlank(keyword)) {
            lqw.and(w -> w.like(ErpWarehouse::getName, keyword)
                .or().like(ErpWarehouse::getCode, keyword));
        }
        lqw.orderByAsc(ErpWarehouse::getId);
        return lqw;
    }

    @Override
    public void insert(ErpWarehouseBo bo) {
        if (StringUtils.isBlank(bo.getCode())) {
            bo.setCode(CodeGenerator.generate(bo.getName(), "CK", code -> codeExists(bo.getId(), code)));
        }
        checkCodeUnique(bo.getId(), bo.getCode());
        baseMapper.insert(MapstructUtils.convert(bo, ErpWarehouse.class));
    }

    @Override
    public void update(ErpWarehouseBo bo) {
        if (bo.getId() == null) {
            throw new ServiceException("仓库ID不能为空");
        }
        if (StringUtils.isBlank(bo.getCode())) {
            ErpWarehouse existing = baseMapper.selectById(bo.getId());
            if (existing == null) {
                throw new ServiceException("仓库不存在");
            }
            bo.setCode(existing.getCode());
        }
        checkCodeUnique(bo.getId(), bo.getCode());
        baseMapper.updateById(MapstructUtils.convert(bo, ErpWarehouse.class));
    }

    private boolean codeExists(Long id, String code) {
        Long count = baseMapper.selectCount(new LambdaQueryWrapper<ErpWarehouse>()
            .eq(ErpWarehouse::getCode, code)
            .ne(id != null, ErpWarehouse::getId, id));
        return count != null && count > 0;
    }

    private void checkCodeUnique(Long id, String code) {
        if (codeExists(id, code)) {
            throw new ServiceException("编码已存在：" + code, 40903);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByIds(List<Long> ids) {
        // 删前校验每个 id 在当前租户下确实存在：跨租户 id 会被租户条件静默跳过，
        // 若不校验则接口返回「操作成功」但实际未删除，用户会误判
        for (Long id : ids) {
            if (baseMapper.selectById(id) == null) {
                throw new ServiceException("仓库不存在或不属于当前租户：" + id, 40905);
            }
        }
        baseMapper.deleteByIds(ids);
    }

    @Override
    public Long defaultWarehouseId() {
        ErpWarehouse warehouse = baseMapper.selectOne(new LambdaQueryWrapper<ErpWarehouse>()
            .eq(ErpWarehouse::getStatus, 1)
            .orderByAsc(ErpWarehouse::getId)
            .last("limit 1"));
        if (warehouse == null) {
            throw new ServiceException("当前租户未配置启用状态的仓库，请先在「仓库资料」中维护主仓", 40901);
        }
        return warehouse.getId();
    }
}
