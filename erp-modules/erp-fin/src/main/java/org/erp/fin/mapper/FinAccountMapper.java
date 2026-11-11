package org.erp.fin.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.erp.fin.domain.FinAccount;
import org.erp.fin.domain.vo.FinAccountVo;

/**
 * 结算账户 Mapper
 */
public interface FinAccountMapper extends BaseMapperPlus<FinAccount, FinAccountVo> {

    /**
     * 按编码查询已逻辑删除的记录 ID（唯一键 tenant_id + code 不区分 del_flag），
     * 删档后重建同码账户时复用该行（对齐 ErpProductMapper 先例）。
     *
     * @param code 账户编码
     * @return 已删除记录 ID，不存在时返回 null
     */
    @Select("select id from fin_account where code = #{code} and del_flag = '1' limit 1")
    Long selectDeletedIdByCode(@Param("code") String code);
}
