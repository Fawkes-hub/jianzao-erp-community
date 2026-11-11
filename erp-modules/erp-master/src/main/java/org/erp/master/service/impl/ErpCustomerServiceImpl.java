package org.erp.master.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.erp.master.domain.ErpCustomer;
import org.erp.master.domain.bo.ErpCustomerBo;
import org.erp.master.domain.vo.ErpCustomerVo;
import org.erp.master.mapper.ErpCustomerMapper;
import org.erp.master.service.IErpCustomerService;
import org.erp.master.util.CodeGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 客户服务实现
 */
@RequiredArgsConstructor
@Service
public class ErpCustomerServiceImpl implements IErpCustomerService {

    private final ErpCustomerMapper baseMapper;

    @Override
    public TableDataInfo<ErpCustomerVo> selectPage(String keyword, Integer status, PageQuery pageQuery) {
        LambdaQueryWrapper<ErpCustomer> lqw = buildQuery(keyword, status);
        Page<ErpCustomerVo> page = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(page);
    }

    @Override
    public List<ErpCustomerVo> selectList(String keyword) {
        return baseMapper.selectVoList(buildQuery(keyword, null));
    }

    private LambdaQueryWrapper<ErpCustomer> buildQuery(String keyword, Integer status) {
        LambdaQueryWrapper<ErpCustomer> lqw = new LambdaQueryWrapper<>();
        lqw.eq(status != null, ErpCustomer::getStatus, status);
        if (StringUtils.isNotBlank(keyword)) {
            lqw.and(w -> w.like(ErpCustomer::getName, keyword)
                .or().like(ErpCustomer::getCode, keyword)
                .or().like(ErpCustomer::getPhone, keyword));
        }
        lqw.orderByDesc(ErpCustomer::getId);
        return lqw;
    }

    @Override
    public ErpCustomerVo selectById(Long id) {
        ErpCustomerVo vo = baseMapper.selectVoById(id);
        if (vo == null) {
            throw new ServiceException("客户不存在");
        }
        return vo;
    }

    @Override
    public void insert(ErpCustomerBo bo) {
        if (StringUtils.isBlank(bo.getCode())) {
            bo.setCode(CodeGenerator.generate(bo.getName(), "KH", code -> codeExists(bo.getId(), code)));
        }
        checkCodeUnique(bo.getId(), bo.getCode());
        baseMapper.insert(MapstructUtils.convert(bo, ErpCustomer.class));
    }

    @Override
    public void update(ErpCustomerBo bo) {
        if (bo.getId() == null) {
            throw new ServiceException("客户ID不能为空");
        }
        if (StringUtils.isBlank(bo.getCode())) {
            ErpCustomer existing = baseMapper.selectById(bo.getId());
            if (existing == null) {
                throw new ServiceException("客户不存在");
            }
            bo.setCode(existing.getCode());
        }
        checkCodeUnique(bo.getId(), bo.getCode());
        baseMapper.updateById(MapstructUtils.convert(bo, ErpCustomer.class));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByIds(List<Long> ids) {
        // 删前校验每个 id 在当前租户下确实存在：跨租户 id 会被租户条件静默跳过，
        // 若不校验则接口返回「操作成功」但实际未删除，用户会误判
        for (Long id : ids) {
            if (baseMapper.selectById(id) == null) {
                throw new ServiceException("客户不存在或不属于当前租户：" + id, 40905);
            }
        }
        baseMapper.deleteByIds(ids);
    }

    private boolean codeExists(Long id, String code) {
        Long count = baseMapper.selectCount(new LambdaQueryWrapper<ErpCustomer>()
            .eq(ErpCustomer::getCode, code)
            .ne(id != null, ErpCustomer::getId, id));
        return count != null && count > 0;
    }

    private void checkCodeUnique(Long id, String code) {
        if (codeExists(id, code)) {
            throw new ServiceException("编码已存在：" + code, 40903);
        }
    }
}
