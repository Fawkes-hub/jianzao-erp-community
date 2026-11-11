package org.erp.master.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.erp.master.domain.ErpWarehouse;
import org.erp.master.domain.vo.ErpWarehouseVo;

/**
 * 仓库 Mapper
 */
public interface ErpWarehouseMapper extends BaseMapperPlus<ErpWarehouse, ErpWarehouseVo> {

    /**
     * 按编码查询已逻辑删除的记录 ID（唯一键 tenant_id + code 不区分 del_flag）。
     *
     * @param code 仓库编码
     * @return 已删除记录 ID，不存在时返回 null
     */
    @Select("select id from md_warehouse where code = #{code} and del_flag = '1' limit 1")
    Long selectDeletedIdByCode(@Param("code") String code);
}
