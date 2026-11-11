package org.erp.master.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.erp.master.domain.ErpSupplier;
import org.erp.master.domain.vo.ErpSupplierVo;

/**
 * 供应商 Mapper
 */
public interface ErpSupplierMapper extends BaseMapperPlus<ErpSupplier, ErpSupplierVo> {

    /**
     * 按编码查询已逻辑删除的记录 ID（唯一键 tenant_id + code 不区分 del_flag）。
     *
     * @param code 供应商编码
     * @return 已删除记录 ID，不存在时返回 null
     */
    @Select("select id from md_supplier where code = #{code} and del_flag = '1' limit 1")
    Long selectDeletedIdByCode(@Param("code") String code);
}
