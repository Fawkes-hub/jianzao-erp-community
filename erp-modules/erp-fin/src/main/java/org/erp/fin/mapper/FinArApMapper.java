package org.erp.fin.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.erp.fin.domain.FinArAp;
import org.erp.fin.domain.vo.FinArApVo;

/**
 * 应收应付台账 Mapper
 */
public interface FinArApMapper extends BaseMapperPlus<FinArAp, FinArApVo> {

    /**
     * 锁定台账行（事务内 SELECT ... FOR UPDATE），核销/反核销/冲销前调用，
     * 防并发读-改-写丢失更新（与库存 selectForUpdate 同一先例）。
     */
    @Select("""
        select * from fin_ar_ap
        where id = #{id}
          and del_flag = '0'
        for update
        """)
    FinArAp selectByIdForUpdate(@Param("id") Long id);
}
