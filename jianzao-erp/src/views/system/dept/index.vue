<template>
  <div class="p-2">
    <transition :enter-active-class="proxy?.animate.searchAnimate.enter" :leave-active-class="proxy?.animate.searchAnimate.leave">
      <div v-show="showSearch" class="mb-[10px]">
        <t-card>
          <t-form ref="queryFormRef" :data="queryParams" class="erp-filter-row">
            <t-form-item label="部门名称" name="deptName">
              <t-input v-model="queryParams.deptName" placeholder="请输入部门名称" clearable @enter="handleQuery" />
            </t-form-item>
            <t-form-item label="类别编码" name="deptCategory">
              <t-input v-model="queryParams.deptCategory" placeholder="请输入类别编码" clearable style="width: 240px" @enter="handleQuery" />
            </t-form-item>
            <t-form-item label="状态" name="status">
              <t-select v-model="queryParams.status" placeholder="部门状态" clearable :options="sys_normal_disable" />
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
        <div class="dept-toolbar">
          <t-button v-hasPermi="['system:dept:add']" theme="primary" variant="outline" @click="handleAdd()">
            <template #icon><PlusIcon /></template>
            新增
          </t-button>
          <t-button theme="default" variant="outline" @click="handleToggleExpandAll">
            <template #icon><UnfoldMoreIcon /></template>
            展开/折叠
          </t-button>
          <right-toolbar v-model:show-search="showSearch" @query-table="getList"></right-toolbar>
        </div>
      </template>

      <EnhancedTable
        class="erp-table"
        row-key="deptId"
        :data="deptList"
        :columns="deptColumns"
        :loading="loading"
        bordered
        size="small"
        cell-empty-content="-"
        :resizable="true"
        :tree="{ childrenKey: 'children', checkStrictly: true }"
        :expanded-tree-nodes="expandedTreeKeys"
        @expanded-tree-nodes-change="(keys: Array<string | number>) => (expandedTreeKeys = keys)"
        @column-resize-change="(ctx: any) => saveColumnWidths('system/dept/list', ctx.columnsWidth)"
      >
        <template #status="{ row }">
          <dict-tag :options="sys_normal_disable" :value="row.status" />
        </template>
        <template #createTime="{ row }">{{ proxy.parseTime(row.createTime) }}</template>
        <template #operation="{ row }">
          <!-- 权限判断挂外层 t-tooltip：v-hasPermi 在 mounted 里 removeChild，与 tooltip 包裹结构打架（DOC/36 P1） -->
          <t-tooltip v-if="checkPermi(['system:dept:edit'])" content="修改" placement="top">
            <t-button theme="primary" variant="text" shape="square" size="small" @click="handleUpdate(row)">
              <template #icon><Edit1Icon /></template>
            </t-button>
          </t-tooltip>
          <t-tooltip v-if="checkPermi(['system:dept:add'])" content="新增" placement="top">
            <t-button theme="primary" variant="text" shape="square" size="small" @click="handleAdd(row)">
              <template #icon><PlusIcon /></template>
            </t-button>
          </t-tooltip>
          <t-tooltip v-if="checkPermi(['system:dept:remove'])" content="删除" placement="top">
            <t-button theme="danger" variant="text" shape="square" size="small" @click="handleDelete(row)">
              <template #icon><DeleteIcon /></template>
            </t-button>
          </t-tooltip>
        </template>
      </EnhancedTable>
    </t-card>

    <t-dialog v-model:visible="dialog.visible" :header="dialog.title" destroy-on-close attach="body" width="600px">
      <t-form ref="deptFormRef" :data="form" :rules="rules" label-width="80px">
        <t-row :gutter="16">
          <t-col v-if="form.parentId !== 0" :span="12">
            <t-form-item label="上级部门" name="parentId">
              <!-- t-tree-select 用 keys 指定节点字段；父部门平铺数据由 handleTree 生成 children -->
              <t-tree-select
                v-model="form.parentId"
                :data="deptOptions"
                :keys="{ value: 'deptId', label: 'deptName', children: 'children' }"
                placeholder="选择上级部门"
                clearable
              />
            </t-form-item>
          </t-col>
          <t-col :span="6">
            <t-form-item label="部门名称" name="deptName">
              <t-input v-model="form.deptName" placeholder="请输入部门名称" />
            </t-form-item>
          </t-col>
          <t-col :span="6">
            <t-form-item label="类别编码" name="deptCategory">
              <t-input v-model="form.deptCategory" placeholder="请输入类别编码" />
            </t-form-item>
          </t-col>
          <t-col :span="6">
            <t-form-item label="显示排序" name="orderNum">
              <t-input-number v-model="form.orderNum" theme="normal" :min="0" style="width: 100%" />
            </t-form-item>
          </t-col>
          <t-col :span="6">
            <t-form-item label="负责人" name="leader">
              <t-select v-model="form.leader" placeholder="请选择负责人" :options="deptUserOptions" />
            </t-form-item>
          </t-col>
          <t-col :span="6">
            <t-form-item label="联系电话" name="phone">
              <t-input v-model="form.phone" placeholder="请输入联系电话" maxlength="11" />
            </t-form-item>
          </t-col>
          <t-col :span="6">
            <t-form-item label="邮箱" name="email">
              <t-input v-model="form.email" placeholder="请输入邮箱" maxlength="50" />
            </t-form-item>
          </t-col>
          <t-col :span="6">
            <t-form-item label="部门状态" name="status">
              <t-radio-group v-model="form.status">
                <t-radio v-for="dict in sys_normal_disable" :key="dict.value" :value="dict.value">{{ dict.label }}</t-radio>
              </t-radio-group>
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
  </div>
</template>

<script setup name="Dept" lang="ts">
import { listDept, getDept, delDept, addDept, updateDept, listDeptExcludeChild } from '@/api/system/dept';
import { DeptForm, DeptQuery, DeptVO } from '@/api/system/dept/types';
import { UserVO } from '@/api/system/user/types';
import { listUserByDeptId } from '@/api/system/user';
import { checkPermi } from '@/utils/permission';
import { mergeSavedWidths, saveColumnWidths } from '@/hooks/useTableColumnWidth';
import { EnhancedTable } from 'tdesign-vue-next/es/table';
import { DeleteIcon, Edit1Icon, PlusIcon, RefreshIcon, SearchIcon, UnfoldMoreIcon } from 'tdesign-icons-vue-next';

interface DeptOptionsType {
  deptId: number | string;
  deptName: string;
  children: DeptOptionsType[];
}

const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const { sys_normal_disable } = toRefs<any>(proxy?.useDict('sys_normal_disable'));

const deptList = ref<DeptVO[]>([]);
const loading = ref(true);
const showSearch = ref(true);
const deptOptions = ref<DeptOptionsType[]>([]);
const isExpandAll = ref(true);
/** 展开的部门行 key：TDesign 树表用受控数组，替代 EP 的 toggleRowExpansion（DOC/35 §4） */
const expandedTreeKeys = ref<Array<string | number>>([]);
const deptUserList = ref<UserVO[]>([]);

const dialog = reactive<DialogOption>({
  visible: false,
  title: ''
});

/** t-form 实例提供 reset()/clearValidate()/validate()，与 EP 表单实例契约不同 */
const queryFormRef = ref<any>();
const deptFormRef = ref<any>();

const initFormData: DeptForm = {
  deptId: undefined,
  parentId: undefined,
  deptName: undefined,
  deptCategory: undefined,
  orderNum: 0,
  leader: undefined,
  phone: undefined,
  email: undefined,
  status: '0'
};
const data = reactive<{ form: DeptForm; queryParams: DeptQuery }>({
  form: { ...initFormData },
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    deptName: undefined,
    deptCategory: undefined,
    status: undefined
  }
});

const { queryParams, form } = toRefs<{ form: DeptForm; queryParams: DeptQuery }>(data);

/** 表单校验规则：t-form 的 rules 形状与 EP 的 ElFormRules 不兼容，单独用 Record 声明（DOC/35 §5） */
const rules: Record<string, any> = {
  parentId: [{ required: true, message: '上级部门不能为空', trigger: 'blur' }],
  deptName: [{ required: true, message: '部门名称不能为空', trigger: 'blur' }],
  orderNum: [{ required: true, message: '显示排序不能为空', trigger: 'blur' }],
  email: [{ type: 'email', message: '请输入正确的邮箱地址', trigger: ['blur', 'change'] }],
  phone: [{ pattern: /^1[3456789][0-9]\d{8}$/, message: '请输入正确的手机号码', trigger: 'blur' }]
};

/** 负责人下拉：t-select 需要 { label, value } 数组 */
const deptUserOptions = computed(() => deptUserList.value.map((item) => ({ label: item.userName, value: item.userId })));

/** 部门树表列配置：t-table 用 columns 数组，cell 字符串与 #插槽同名（DOC/35 §5） */
const deptColumns: any[] = [
  { colKey: 'deptName', title: '部门名称', width: 260 },
  { colKey: 'deptCategory', title: '类别编码', align: 'center', width: 200 },
  { colKey: 'orderNum', title: '排序', align: 'center', width: 200 },
  { colKey: 'status', title: '状态', align: 'center', width: 100, cell: 'status' },
  { colKey: 'createTime', title: '创建时间', align: 'center', width: 200, cell: 'createTime' },
  { colKey: 'operation', title: '操作', align: 'center', fixed: 'right', cell: 'operation' }
];
mergeSavedWidths('system/dept/list', deptColumns);

/** 查询菜单列表 */
const getList = async () => {
  loading.value = true;
  const res = await listDept(queryParams.value);
  const data = proxy?.handleTree<DeptVO>(res.data, 'deptId');
  if (data) {
    deptList.value = data;
    // 先写展开态再换数据：增强表只在 data 变更时按当前 expandedTreeNodes 展平子行
    expandedTreeKeys.value = isExpandAll.value ? collectKeys(data) : [];
  }
  loading.value = false;
};

/** 递归收集所有部门 key，用于「展开全部」的受控展开态 */
const collectKeys = (data: DeptVO[]): Array<string | number> => {
  const keys: Array<string | number> = [];
  data.forEach((item) => {
    keys.push(item.deptId);
    if (item.children && item.children.length > 0) keys.push(...collectKeys(item.children));
  });
  return keys;
};

/** 查询当前部门的所有用户 */
async function getDeptAllUser(deptId: any) {
  if (deptId !== null && deptId !== '' && deptId !== undefined) {
    const res = await listUserByDeptId(deptId);
    deptUserList.value = res.data;
  }
}

/** 取消按钮 */
const cancel = () => {
  reset();
  dialog.visible = false;
};
/** 表单重置：t-form 只清校验态，表单值以 initFormData 为准 */
const reset = () => {
  form.value = { ...initFormData };
  deptFormRef.value?.clearValidate();
};

/** 搜索按钮操作 */
const handleQuery = () => {
  getList();
};
/** 重置按钮操作 */
const resetQuery = () => {
  queryFormRef.value?.reset();
  queryParams.value.deptName = undefined;
  queryParams.value.deptCategory = undefined;
  queryParams.value.status = undefined;
  handleQuery();
};

/** 展开/折叠操作：受控 expandedTreeKeys，替代 EP 的 toggleRowExpansion 递归 */
const handleToggleExpandAll = () => {
  isExpandAll.value = !isExpandAll.value;
  expandedTreeKeys.value = isExpandAll.value ? collectKeys(deptList.value) : [];
};

/** 新增按钮操作 */
const handleAdd = async (row?: DeptVO) => {
  reset();
  const res = await listDept();
  const data = proxy?.handleTree<DeptOptionsType>(res.data, 'deptId');
  if (data) {
    deptOptions.value = data;
    if (row && row.deptId) {
      form.value.parentId = row?.deptId;
    }
    dialog.visible = true;
    dialog.title = '添加部门';
  }
};

/** 修改按钮操作 */
const handleUpdate = async (row: DeptVO) => {
  reset();
  //查询当前部门所有用户
  getDeptAllUser(row.deptId);
  const res = await getDept(row.deptId);
  form.value = res.data;
  const response = await listDeptExcludeChild(row.deptId);
  const data = proxy?.handleTree<DeptOptionsType>(response.data, 'deptId');
  if (data) {
    deptOptions.value = data;
    if (data.length === 0) {
      const noResultsOptions: DeptOptionsType = {
        deptId: res.data.parentId,
        deptName: res.data.parentName,
        children: []
      };
      deptOptions.value.push(noResultsOptions);
    }
  }
  dialog.visible = true;
  dialog.title = '修改部门';
};
/** 提交按钮：t-form.validate() 失败 resolve(false)，不 reject（DOC/35 §5） */
const submitForm = async () => {
  const result = await deptFormRef.value?.validate().catch(() => false);
  const valid = result === true || result === undefined;
  if (!valid) return;
  form.value.deptId ? await updateDept(form.value) : await addDept(form.value);
  proxy?.$modal.msgSuccess('操作成功');
  dialog.visible = false;
  await getList();
};
/** 删除按钮操作 */
const handleDelete = async (row: DeptVO) => {
  await proxy?.$modal.confirm('是否确认删除名称为"' + row.deptName + '"的数据项?');
  await delDept(row.deptId);
  await getList();
  proxy?.$modal.msgSuccess('删除成功');
};

onMounted(() => {
  getList();
});
</script>

<style lang="scss" scoped>
.dept-toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}
</style>
