<template>
  <div class="p-2">
    <transition :enter-active-class="proxy?.animate.searchAnimate.enter" :leave-active-class="proxy?.animate.searchAnimate.leave">
      <div v-show="showSearch" class="mb-[10px]">
        <t-card>
          <t-form ref="queryFormRef" :data="queryParams" class="erp-filter-row" layout="inline">
            <t-form-item label="菜单名称" name="menuName">
              <t-input v-model="queryParams.menuName" placeholder="请输入菜单名称" clearable @enter="handleQuery" />
            </t-form-item>
            <t-form-item label="状态" name="status">
              <t-select v-model="queryParams.status" placeholder="菜单状态" clearable :options="sys_normal_disable" />
            </t-form-item>
            <div class="erp-filter-actions">
              <t-button theme="primary" @click="handleQuery">
                <template #icon><SearchIcon /></template>
                搜索
              </t-button>
              <t-button theme="default" variant="outline" @click="resetQuery">
                <template #icon><RefreshIcon /></template>
                重置
              </t-button>
            </div>
          </t-form>
        </t-card>
      </div>
    </transition>

    <t-card>
      <template #header>
        <div class="menu-toolbar">
          <t-button v-hasPermi="['system:menu:add']" theme="primary" variant="outline" @click="handleAdd()">
            <template #icon><PlusIcon /></template>
            新增
          </t-button>
          <t-button v-hasPermi="['system:menu:remove']" theme="danger" variant="outline" :loading="deleteLoading" @click="handleCascadeDelete">
            <template #icon><DeleteIcon /></template>
            级联删除
          </t-button>
          <right-toolbar v-model:show-search="showSearch" @query-table="getList"></right-toolbar>
        </div>
      </template>

      <!-- 树形表格只有 EnhancedTable 消费 tree 属性（t-table/PrimaryTable 会静默忽略），故主列表单独用增强表 -->
      <EnhancedTable
        class="erp-table"
        row-key="menuId"
        :data="menuList"
        :columns="menuColumns"
        :loading="loading"
        bordered
        size="small"
        cell-empty-content="-"
        :resizable="true"
        :tree="{ childrenKey: 'children', checkStrictly: true }"
        :expanded-tree-nodes="expandedTreeKeys"
        @expanded-tree-nodes-change="(keys: Array<string | number>) => (expandedTreeKeys = keys)"
        @column-resize-change="(ctx: any) => saveColumnWidths('system/menu/list', ctx.columnsWidth)"
      >
        <template #icon="{ row }">
          <svg-icon :icon-class="row.icon" />
        </template>
        <template #status="{ row }">
          <dict-tag :options="sys_normal_disable" :value="row.status" />
        </template>
        <template #createTime="{ row }">
          <span>{{ row.createTime }}</span>
        </template>
        <template #operation="{ row }">
          <!-- 权限判断挂外层 t-tooltip：v-hasPermi 在 mounted 里 removeChild，与 tooltip 包裹结构打架（DOC/36 P1） -->
          <t-tooltip v-if="checkPermi(['system:menu:edit'])" content="修改" placement="top">
            <t-button theme="primary" variant="text" shape="square" size="small" @click="handleUpdate(row)">
              <template #icon><Edit1Icon /></template>
            </t-button>
          </t-tooltip>
          <t-tooltip v-if="checkPermi(['system:menu:add'])" content="新增" placement="top">
            <t-button theme="primary" variant="text" shape="square" size="small" @click="handleAdd(row)">
              <template #icon><PlusIcon /></template>
            </t-button>
          </t-tooltip>
          <t-tooltip v-if="checkPermi(['system:menu:remove'])" content="删除" placement="top">
            <t-button theme="primary" variant="text" shape="square" size="small" @click="handleDelete(row)">
              <template #icon><DeleteIcon /></template>
            </t-button>
          </t-tooltip>
        </template>
      </EnhancedTable>
    </t-card>

    <t-dialog v-model:visible="dialog.visible" :header="dialog.title" destroy-on-close attach="body" width="750px">
      <t-form ref="menuFormRef" :data="form" :rules="rules" label-width="100px">
        <!-- TDesign 是 12 栅格，EP 的 24 栅格 span 减半，每行合计 12 -->
        <t-row :gutter="16">
          <t-col :span="12">
            <t-form-item label="上级菜单" name="parentId">
              <!-- t-tree-select 用 keys 指定节点字段；父菜单平铺数据由 handleTree 生成 children -->
              <t-tree-select
                v-model="form.parentId"
                :data="menuOptions"
                :keys="{ value: 'menuId', label: 'menuName', children: 'children' }"
                placeholder="选择上级菜单"
                clearable
                :tree-props="{ checkStrictly: true }"
              />
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item label="菜单类型" name="menuType">
              <t-radio-group v-model="form.menuType">
                <t-radio value="M">目录</t-radio>
                <t-radio value="C">菜单</t-radio>
                <t-radio value="F">按钮</t-radio>
              </t-radio-group>
            </t-form-item>
          </t-col>
          <t-col v-if="form.menuType !== 'F'" :span="12">
            <t-form-item label="菜单图标" name="icon">
              <!-- 图标选择器 -->
              <icon-select v-model="form.icon" />
            </t-form-item>
          </t-col>
          <t-col :span="6">
            <t-form-item label="菜单名称" name="menuName">
              <t-input v-model="form.menuName" placeholder="请输入菜单名称" />
            </t-form-item>
          </t-col>
          <t-col :span="6">
            <t-form-item label="显示排序" name="orderNum">
              <t-input-number v-model="form.orderNum" theme="normal" :min="0" style="width: 100%" />
            </t-form-item>
          </t-col>
          <t-col v-if="form.menuType !== 'F'" :span="6">
            <t-form-item>
              <template #label>
                <span>
                  <t-tooltip content="选择是外链则路由地址需要以`http(s)://`开头" placement="top">
                    <HelpCircleIcon />
                  </t-tooltip>
                  是否外链
                </span>
              </template>
              <t-radio-group v-model="form.isFrame">
                <t-radio value="0">是</t-radio>
                <t-radio value="1">否</t-radio>
              </t-radio-group>
            </t-form-item>
          </t-col>
          <t-col v-if="form.menuType !== 'F'" :span="6">
            <t-form-item name="path">
              <template #label>
                <span>
                  <t-tooltip content="访问的路由地址，如：`user`，如外网地址需内链访问则以`http(s)://`开头" placement="top">
                    <HelpCircleIcon />
                  </t-tooltip>
                  路由地址
                </span>
              </template>
              <t-input v-model="form.path" placeholder="请输入路由地址" />
            </t-form-item>
          </t-col>
          <t-col v-if="form.menuType === 'C'" :span="6">
            <t-form-item name="component">
              <template #label>
                <span>
                  <t-tooltip content="访问的组件路径，如：`system/user/index`，默认在`views`目录下" placement="top">
                    <HelpCircleIcon />
                  </t-tooltip>
                  组件路径
                </span>
              </template>
              <t-input v-model="form.component" placeholder="请输入组件路径" />
            </t-form-item>
          </t-col>
          <t-col v-if="form.menuType !== 'M'" :span="6">
            <t-form-item>
              <t-input v-model="form.perms" placeholder="请输入权限标识" maxlength="100" />
              <template #label>
                <span>
                  <t-tooltip content="控制器中定义的权限字符，如：@SaCheckPermission('system:user:list')" placement="top">
                    <HelpCircleIcon />
                  </t-tooltip>
                  权限字符
                </span>
              </template>
            </t-form-item>
          </t-col>
          <t-col v-if="form.menuType === 'C'" :span="6">
            <t-form-item>
              <t-input v-model="form.queryParam" placeholder="请输入路由参数" maxlength="255" />
              <template #label>
                <span>
                  <t-tooltip content='访问路由的默认传递参数，如：`{"id": 1, "name": "ry"}`' placement="top">
                    <HelpCircleIcon />
                  </t-tooltip>
                  路由参数
                </span>
              </template>
            </t-form-item>
          </t-col>
          <t-col v-if="form.menuType === 'C'" :span="6">
            <t-form-item>
              <template #label>
                <span>
                  <t-tooltip content="选择是则会被`keep-alive`缓存，需要匹配组件的`name`和地址保持一致" placement="top">
                    <HelpCircleIcon />
                  </t-tooltip>
                  是否缓存
                </span>
              </template>
              <t-radio-group v-model="form.isCache">
                <t-radio value="0">缓存</t-radio>
                <t-radio value="1">不缓存</t-radio>
              </t-radio-group>
            </t-form-item>
          </t-col>
          <t-col v-if="form.menuType !== 'F'" :span="6">
            <t-form-item>
              <template #label>
                <span>
                  <t-tooltip content="选择隐藏则路由将不会出现在侧边栏，但仍然可以访问" placement="top">
                    <HelpCircleIcon />
                  </t-tooltip>
                  显示状态
                </span>
              </template>
              <t-radio-group v-model="form.visible">
                <t-radio v-for="dict in sys_show_hide" :key="dict.value" :value="dict.value">{{ dict.label }} </t-radio>
              </t-radio-group>
            </t-form-item>
          </t-col>
          <t-col :span="6">
            <t-form-item>
              <template #label>
                <span>
                  <t-tooltip content="选择停用则路由将不会出现在侧边栏，也不能被访问" placement="top">
                    <HelpCircleIcon />
                  </t-tooltip>
                  菜单状态
                </span>
              </template>
              <t-radio-group v-model="form.status">
                <t-radio v-for="dict in sys_normal_disable" :key="dict.value" :value="dict.value">
                  {{ dict.label }}
                </t-radio>
              </t-radio-group>
            </t-form-item>
          </t-col>
          <t-col v-if="form.visible !== '0'" :span="6">
            <t-form-item name="remark">
              <template #label>
                <span>
                  <t-tooltip content="隐藏菜单填写默认激活路由，比如激活父菜单的路由 /system/user" placement="top">
                    <HelpCircleIcon />
                  </t-tooltip>
                  激活路由
                </span>
              </template>
              <t-input v-model="form.remark" placeholder="请输入激活路径" />
            </t-form-item>
          </t-col>
        </t-row>
      </t-form>
      <template #footer>
        <div class="dialog-footer">
          <t-button theme="primary" @click="submitForm">确 定</t-button>
          <t-button theme="default" variant="outline" @click="cancel">取 消</t-button>
        </div>
      </template>
    </t-dialog>

    <t-dialog v-model:visible="deleteDialog.visible" :header="deleteDialog.title" destroy-on-close attach="body" width="750px">
      <t-tree
        ref="menuTreeRef"
        class="tree-border"
        checkable
        :data="menuOptions"
        :keys="{ value: 'menuId', label: 'menuName', children: 'children' }"
        :value="deleteCheckedKeys"
        :expanded="deleteExpandedKeys"
        empty="加载中，请稍候"
        @change="(keys: Array<string | number>) => (deleteCheckedKeys = keys)"
        @expand="(keys: Array<string | number>) => (deleteExpandedKeys = keys)"
      />
      <template #footer>
        <div class="dialog-footer">
          <t-button theme="primary" :loading="deleteLoading" @click="submitDeleteForm">确 定</t-button>
          <t-button theme="default" variant="outline" @click="cancelCascade">取 消</t-button>
        </div>
      </template>
    </t-dialog>
  </div>
</template>

<script setup name="Menu" lang="ts">
import { addMenu, cascadeDelMenu, delMenu, getMenu, listMenu, updateMenu } from '@/api/system/menu';
import { MenuForm, MenuQuery, MenuVO } from '@/api/system/menu/types';
import { MenuTypeEnum } from '@/enums/MenuTypeEnum';
import { checkPermi } from '@/utils/permission';
import { mergeSavedWidths, saveColumnWidths } from '@/hooks/useTableColumnWidth';
// 树形表格只有 EnhancedTable 消费 tree 属性（t-table/PrimaryTable 会静默忽略）
import { EnhancedTable } from 'tdesign-vue-next/es/table';
import { DeleteIcon, Edit1Icon, HelpCircleIcon, PlusIcon, RefreshIcon, SearchIcon } from 'tdesign-icons-vue-next';

interface MenuOptionsType {
  menuId: number;
  menuName: string;
  children: MenuOptionsType[] | undefined;
}

const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const { sys_show_hide, sys_normal_disable } = toRefs<any>(proxy?.useDict('sys_show_hide', 'sys_normal_disable'));

const menuList = ref<MenuVO[]>([]);
const loading = ref(true);
const showSearch = ref(true);
const menuOptions = ref<MenuOptionsType[]>([]);
/** 树表展开态受控：TDesign 没有 EP 的 lazy load 回调，列表一次性构树后由展开数组控制显隐 */
const expandedTreeKeys = ref<Array<string | number>>([]);

const dialog = reactive<DialogOption>({
  visible: false,
  title: ''
});

/** t-form 实例提供 reset()/clearValidate()/validate()，与 EP 表单实例契约不同 */
const queryFormRef = ref<any>();
const menuFormRef = ref<any>();
const initFormData = {
  path: '',
  menuId: undefined,
  parentId: 0,
  menuName: '',
  icon: '',
  menuType: MenuTypeEnum.M,
  orderNum: 1,
  isFrame: '1',
  isCache: '0',
  visible: '0',
  status: '0'
};
const data = reactive<{ form: MenuForm; queryParams: MenuQuery }>({
  form: { ...initFormData },
  queryParams: {
    menuName: undefined,
    status: undefined
  }
});

const { queryParams, form } = toRefs<{ form: MenuForm; queryParams: MenuQuery }>(data);

/** 表单校验规则：t-form 的 rules 形状与 EP 的 ElFormRules 不兼容，单独用 Record 声明（DOC/35 §5） */
const rules: Record<string, any> = {
  menuName: [{ required: true, message: '菜单名称不能为空', trigger: 'blur' }],
  orderNum: [{ required: true, message: '菜单顺序不能为空', trigger: 'blur' }],
  path: [{ required: true, message: '路由地址不能为空', trigger: 'blur' }]
};

/** 菜单树表列配置：prop→colKey、show-overflow-tooltip→ellipsis、模板列走同名 cell 插槽 */
const menuColumns: any[] = [
  { colKey: 'menuName', title: '菜单名称', width: 160, ellipsis: true },
  { colKey: 'icon', title: '图标', align: 'center', width: 100, cell: 'icon' },
  { colKey: 'orderNum', title: '排序', width: 60 },
  { colKey: 'perms', title: '权限标识', ellipsis: true },
  { colKey: 'component', title: '组件路径', ellipsis: true },
  { colKey: 'status', title: '状态', width: 80, cell: 'status' },
  { colKey: 'createTime', title: '创建时间', align: 'center', cell: 'createTime' },
  { colKey: 'operation', title: '操作', width: 180, fixed: 'right', cell: 'operation' }
];
mergeSavedWidths('system/menu/list', menuColumns);

/** 查询菜单列表：TDesign 树表用 children 承载子行，handleTree 按 parentId 组装（接口与请求参数不变） */
const getList = async () => {
  loading.value = true;
  const res = await listMenu(queryParams.value);
  menuList.value = proxy?.handleTree<MenuVO>(res.data, 'menuId') ?? [];
  expandedTreeKeys.value = [];
  loading.value = false;
};
/** 查询菜单下拉树结构 */
const getTreeselect = async () => {
  menuOptions.value = [];
  const response = await listMenu();
  const menu: MenuOptionsType = { menuId: 0, menuName: '主类目', children: [] };
  menu.children = proxy?.handleTree<MenuOptionsType>(response.data, 'menuId');
  menuOptions.value.push(menu);
};
/** 取消按钮 */
const cancel = () => {
  reset();
  dialog.visible = false;
};
/** 表单重置：t-form 只清校验态，表单值以 initFormData 为准 */
const reset = () => {
  form.value = { ...initFormData };
  menuFormRef.value?.clearValidate();
};

/** 搜索按钮操作 */
const handleQuery = () => {
  getList();
};
/** 重置按钮操作 */
const resetQuery = () => {
  queryFormRef.value?.reset();
  queryParams.value.menuName = undefined;
  queryParams.value.status = undefined;
  handleQuery();
};
/** 新增按钮操作 */
const handleAdd = (row?: MenuVO) => {
  reset();
  getTreeselect();
  row && row.menuId ? (form.value.parentId = row.menuId) : (form.value.parentId = 0);
  dialog.visible = true;
  dialog.title = '添加菜单';
};
/** 修改按钮操作 */
const handleUpdate = async (row: MenuVO) => {
  reset();
  await getTreeselect();
  if (row.menuId) {
    const { data } = await getMenu(row.menuId);
    form.value = data;
  }
  dialog.visible = true;
  dialog.title = '修改菜单';
};
/** 提交按钮：t-form.validate() 失败 resolve(false)，不 reject（DOC/35 §5） */
const submitForm = async () => {
  const result = await menuFormRef.value?.validate().catch(() => false);
  const valid = result === true || result === undefined;
  if (!valid) return;
  form.value.menuId ? await updateMenu(form.value) : await addMenu(form.value);
  proxy?.$modal.msgSuccess('操作成功');
  dialog.visible = false;
  await getList();
};
/** 删除按钮操作 */
const handleDelete = async (row: MenuVO) => {
  await proxy?.$modal.confirm('是否确认删除名称为"' + row.menuName + '"的数据项?');
  await delMenu(row.menuId);
  await getList();
  proxy?.$modal.msgSuccess('删除成功');
};

const deleteLoading = ref<boolean>(false);
/** 级联删除树实例：提交前取全选节点，保持与 EP getCheckedKeys() 相同的提交集合 */
const menuTreeRef = ref<any>();
/** 级联删除树：受控勾选/展开值，替代 EP 的 setCheckedKeys/getCheckedKeys */
const deleteCheckedKeys = ref<Array<string | number>>([]);
const deleteExpandedKeys = ref<Array<string | number>>([0]);

/** 级联删除弹窗：用 ref<DialogOption> 声明，避免 vue-tsc 对本页第二个 t-dialog 的 v-model:visible 误判为 Ref 取值 */
const deleteDialog = ref<DialogOption>({
  visible: false,
  title: '级联删除菜单'
});

/** 级联删除按钮操作 */
const handleCascadeDelete = () => {
  deleteCheckedKeys.value = [];
  getTreeselect();
  deleteDialog.value.visible = true;
};

/** 取消按钮 */
const cancelCascade = () => {
  deleteCheckedKeys.value = [];
  deleteDialog.value.visible = false;
};

/** 删除提交按钮：仅提交全选节点（不含半选父节点），与 EP getCheckedKeys() 的提交集合一致 */
const submitDeleteForm = async () => {
  const models = menuTreeRef.value?.getItems?.() ?? [];
  const menuIds = models.filter((m: any) => m.checked && !m.indeterminate).map((m: any) => m.value ?? m.data?.menuId);
  if (menuIds.length < 0) {
    proxy?.$modal.msgWarning('请选择要删除的菜单');
    return;
  }

  deleteLoading.value = true;
  await cascadeDelMenu(menuIds).finally(() => (deleteLoading.value = false));
  await getList();
  proxy?.$modal.msgSuccess('删除成功');
  deleteDialog.value.visible = false;
};

onMounted(() => {
  getList();
});
</script>

<style scoped lang="scss">
.tree-border {
  height: 300px;
  overflow: auto;
}
.menu-toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}
</style>
