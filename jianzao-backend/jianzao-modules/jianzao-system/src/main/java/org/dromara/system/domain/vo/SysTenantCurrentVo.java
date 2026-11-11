package org.dromara.system.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 当前生效企业视图对象
 * <p>
 * 只暴露顶栏展示所需的企业编号与名称，避免把套餐、有效期、联系人等管理字段下发给普通企业用户。
 *
 * @author Fawkes
 */
@Data
public class SysTenantCurrentVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 当前生效的企业ID（平台超管进入企业后为动态租户）
     */
    private String tenantId;

    /**
     * 企业名称
     */
    private String companyName;

}
