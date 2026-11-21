package org.erp.fin.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.erp.fin.domain.FinOpening;
import org.erp.fin.domain.vo.FinOpeningVo;

/**
 * 期初往来 Mapper
 */
public interface FinOpeningMapper extends BaseMapperPlus<FinOpening, FinOpeningVo> {

    /**
     * 锁定期初行（事务内 SELECT ... FOR UPDATE），确认/反确认前调用，
     * 防并发保存把已确认行改回草稿（与台账 selectByIdForUpdate 同一先例）。
     */
    @Select("""
        select * from fin_opening
        where id = #{id}
          and del_flag = '0'
        for update
        """)
    FinOpening selectByIdForUpdate(@Param("id") Long id);
}
