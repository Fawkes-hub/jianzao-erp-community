package org.dromara.common.core.constant;

/**
 * 租户常量信息
 *
 * @author Lion Li
 */
public interface TenantConstants {

    /**
     * 超级管理员ID
     */
    Long SUPER_ADMIN_ID = 1L;

    /**
     * 超级管理员角色 roleKey
     */
    String SUPER_ADMIN_ROLE_KEY = "superadmin";

    /**
     * 租户管理员角色 roleKey
     */
    String TENANT_ADMIN_ROLE_KEY = "admin";

    /**
     * 租户管理员角色名称
     */
    String TENANT_ADMIN_ROLE_NAME = "管理员";

    /**
     * 租户员工默认角色 roleKey
     */
    String TENANT_STAFF_ROLE_KEY = "staff";

    /**
     * 租户员工默认角色名称
     */
    String TENANT_STAFF_ROLE_NAME = "员工";

    /**
     * 仅本人数据权限
     */
    String DATA_SCOPE_SELF = "5";

    /**
     * 默认租户ID
     */
    String DEFAULT_TENANT_ID = "000000";

}
