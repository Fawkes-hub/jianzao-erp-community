package org.erp.master.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.erp.master.domain.ErpCustomer;
import org.erp.master.domain.vo.ErpCustomerVo;

/**
 * 客户 Mapper
 */
public interface ErpCustomerMapper extends BaseMapperPlus<ErpCustomer, ErpCustomerVo> {

    /**
     * 按编码查询已逻辑删除的记录 ID（唯一键 tenant_id + code 不区分 del_flag）。
     *
     * @param code 客户编码
     * @return 已删除记录 ID，不存在时返回 null
     */
    @Select("select id from md_customer where code = #{code} and del_flag = '1' limit 1")
    Long selectDeletedIdByCode(@Param("code") String code);
}
