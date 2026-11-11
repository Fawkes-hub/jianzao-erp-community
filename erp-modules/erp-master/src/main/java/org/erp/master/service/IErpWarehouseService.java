package org.erp.master.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.erp.master.domain.bo.ErpWarehouseBo;
import org.erp.master.domain.vo.ErpWarehouseVo;

import java.util.List;

/**
 * 仓库服务接口
 */
public interface IErpWarehouseService {

    TableDataInfo<ErpWarehouseVo> selectPage(String keyword, Integer status, PageQuery pageQuery);

    List<ErpWarehouseVo> selectList(String keyword);

    void insert(ErpWarehouseBo bo);

    void update(ErpWarehouseBo bo);

    void deleteByIds(List<Long> ids);

    /**
     * 取当前租户的主仓 ID。
     *
     * <p>仓库表主键为全局自增 ID，非租户内唯一，因此各业务单据不能硬编码仓库 ID：
     * 硬编码会导致非首个租户的单据落到不存在的仓库。单据统一用本方法取主仓。</p>
     *
     * @return 主仓 ID；当前租户无启用仓库时抛 40901
     */
    Long defaultWarehouseId();
}
