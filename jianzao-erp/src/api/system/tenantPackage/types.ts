export interface TenantPkgVO extends BaseEntity {
  packageId: string | number;
  packageName: string;
  /** 企业管理员可用菜单 ID 串 */
  menuIds: string;
  /** 员工默认角色菜单 ID 串（开租户时据此生成 staff 角色） */
  employeeMenuIds: string;
  remark: string;
  menuCheckStrictly: boolean;
  employeeCheckStrictly?: boolean;
  status: string;
}

export interface TenantPkgQuery extends PageQuery {
  packageName: string;
}

export interface TenantPkgForm {
  packageId: string | number | undefined;
  packageName: string;
  menuIds: string | Array<string | number>;
  /** 员工默认权限：必须落在 menuIds 子集内，服务端保存时校验 */
  employeeMenuIds: string | Array<string | number>;
  remark: string;
  menuCheckStrictly: boolean;
  employeeCheckStrictly?: boolean;
}
