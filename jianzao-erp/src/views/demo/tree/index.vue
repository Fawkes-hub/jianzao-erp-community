<template>
  <div class="p-2">
    <transition :enter-active-class="proxy?.animate.searchAnimate.enter" :leave-active-class="proxy?.animate.searchAnimate.leave">
      <div v-show="showSearch" class="mb-[10px]">
        <t-card hover-shadow>
          <t-form ref="queryFormRef" :data="queryParams" layout="inline">
            <t-form-item label="树节点名" name="treeName">
              <t-input v-model="queryParams.treeName" placeholder="请输入树节点名" clearable @enter="handleQuery" />
            </t-form-item>
            <t-form-item>
              <t-button theme="primary" @click="handleQuery">
                <template #icon><SearchIcon /></template>
                搜索
              </t-button>
              <t-button theme="default" variant="outline" @click="resetQuery">
                <template #icon><RefreshIcon /></template>
                重置
              </t-button>
            </t-form-item>
          </t-form>
        </t-card>
      </div>
    </transition>

    <t-card hover-shadow>
      <template #header>
        <div class="flex items-center justify-between w-full">
          <div class="flex items-center gap-2">
            <t-button v-hasPermi="['demo:tree:add']" theme="primary" variant="outline" @click="handleAdd()">
              <template #icon><PlusIcon /></template>
              新增
            </t-button>
            <t-button theme="default" variant="outline" @click="handleToggleExpandAll">
              <template #icon><UnfoldMoreIcon /></template>
              展开/折叠
            </t-button>
          </div>
          <right-toolbar v-model:show-search="showSearch" @query-table="getList"></right-toolbar>
        </div>
      </template>
      <EnhancedTable
        ref="treeTableRef"
        class="erp-table"
        row-key="id"
        :data="treeList"
        :columns="treeColumns"
        :loading="loading"
        bordered
        size="small"
        cell-empty-content="-"
        :resizable="true"
        :tree="{ childrenKey: 'children', checkStrictly: true }"
        :expanded-tree-nodes="expandedTreeKeys"
        @expanded-tree-nodes-change="(keys: Array<string | number>) => (expandedTreeKeys = keys)"
      >
        <template #operation="{ row }">
          <!-- 权限判断挂外层 t-tooltip：v-hasPermi 在 mounted 里 removeChild，会与 t-tooltip 包裹结构打架 -->
          <t-tooltip v-if="checkPermi(['demo:tree:edit'])" content="修改" placement="top">
            <t-button theme="primary" variant="text" shape="square" size="small" @click="handleUpdate(row)">
              <template #icon><Edit1Icon /></template>
            </t-button>
          </t-tooltip>
          <t-tooltip v-if="checkPermi(['demo:tree:add'])" content="新增" placement="top">
            <t-button theme="primary" variant="text" shape="square" size="small" @click="handleAdd(row)">
              <template #icon><PlusIcon /></template>
            </t-button>
          </t-tooltip>
          <t-tooltip v-if="checkPermi(['demo:tree:remove'])" content="删除" placement="top">
            <t-button theme="danger" variant="text" shape="square" size="small" @click="handleDelete(row)">
              <template #icon><DeleteIcon /></template>
            </t-button>
          </t-tooltip>
        </template>
      </EnhancedTable>
    </t-card>
    <!-- 添加或修改测试树对话框 -->
    <t-dialog v-model:visible="dialog.visible" width="500px" attach="body">
      <template #header>{{ dialog.title }}</template>
      <t-form ref="treeFormRef" :data="form" :rules="rules" label-width="80px">
        <t-form-item label="父id" name="parentId">
          <t-tree-select
            v-model="form.parentId"
            :data="treeOptions"
            :keys="{ value: 'id', label: 'treeName', children: 'children' }"
            placeholder="请选择父id"
            clearable
            :tree-props="{ checkStrictly: true }"
          />
        </t-form-item>
        <t-form-item label="部门id" name="deptId">
          <t-input v-model="form.deptId" placeholder="请输入部门id" />
        </t-form-item>
        <t-form-item label="用户id" name="userId">
          <t-input v-model="form.userId" placeholder="请输入用户id" />
        </t-form-item>
        <t-form-item label="值" name="treeName">
          <t-input v-model="form.treeName" placeholder="请输入值" />
        </t-form-item>
      </t-form>
      <template #footer>
        <div class="dialog-footer">
          <t-button :loading="buttonLoading" theme="primary" @click="submitForm">确 定</t-button>
          <t-button theme="default" variant="outline" @click="cancel">取 消</t-button>
        </div>
      </template>
    </t-dialog>
  </div>
</template>

<script setup name="Tree" lang="ts">
import { listTree, getTree, delTree, addTree, updateTree } from '@/api/demo/tree';
import { TreeVO, TreeQuery, TreeForm } from '@/api/demo/tree/types';
import { EnhancedTable } from 'tdesign-vue-next/es/table';
import { checkPermi } from '@/utils/permission';
// TDesign 图标必须显式 import，不会自动解析（DOC/35 §3）
import { DeleteIcon, Edit1Icon, PlusIcon, RefreshIcon, SearchIcon, UnfoldMoreIcon } from 'tdesign-icons-vue-next';

type TreeOption = {
  id: number;
  treeName: string;
  children?: TreeOption[];
};

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const treeList = ref<TreeVO[]>([]);
const treeOptions = ref<TreeOption[]>([]);
const buttonLoading = ref(false);
const showSearch = ref(true);
const isExpandAll = ref(true);
const loading = ref(false);
// 树表展开态受控：替代 EP 的 toggleRowExpansion 递归（对齐 dept/index.vue）
const expandedTreeKeys = ref<Array<string | number>>([]);

const queryFormRef = ref<any>();
const treeFormRef = ref<any>();
const treeTableRef = ref<any>();

const dialog = reactive<DialogOption>({
  visible: false,
  title: ''
});

const initFormData: TreeForm = {
  id: undefined,
  parentId: undefined,
  deptId: undefined,
  userId: undefined,
  treeName: undefined
};

const data = reactive<{ form: TreeForm; queryParams: TreeQuery }>({
  form: { ...initFormData },
  queryParams: {
    parentId: undefined,
    deptId: undefined,
    userId: undefined,
    treeName: undefined
  }
});

/** t-form 的 rules 形状与 EP 的 ElFormRules 不兼容，用宽松类型声明 */
const rules: Record<string, any> = {
  id: [{ required: true, message: '主键不能为空', trigger: 'blur' }],
  parentId: [{ required: true, message: '父id不能为空', trigger: 'blur' }],
  deptId: [{ required: true, message: '部门id不能为空', trigger: 'blur' }],
  userId: [{ required: true, message: '用户id不能为空', trigger: 'blur' }],
  treeName: [{ required: true, message: '值不能为空', trigger: 'blur' }]
};

const { queryParams, form } = toRefs(data);

/** 测试树表列配置：t-table 用 columns 数组，cell 字符串与 #插槽同名（DOC/35 §4） */
const treeColumns: any[] = [
  { colKey: 'parentId', title: '父id', align: 'center' },
  { colKey: 'deptId', title: '部门id', align: 'center' },
  { colKey: 'userId', title: '用户id', align: 'center' },
  { colKey: 'treeName', title: '树节点名', align: 'center' },
  { colKey: 'operation', title: '操作', align: 'center', cell: 'operation' }
];

/** 查询测试树列表 */
const getList = async () => {
  loading.value = true;
  const res = await listTree(queryParams.value);
  const data = proxy?.handleTree<TreeVO>(res.data, 'id', 'parentId');
  if (data) {
    treeList.value = data;
    // 先写展开态再换数据：增强表只在 data 变更时按当前 expandedTreeNodes 展平子行
    expandedTreeKeys.value = isExpandAll.value ? collectKeys(data) : [];
    loading.value = false;
  }
};

/** 查询测试树下拉树结构 */
const getTreeselect = async () => {
  const res = await listTree();
  treeOptions.value = [];
  const data: TreeOption = { id: 0, treeName: '顶级节点', children: [] };
  data.children = proxy?.handleTree<TreeOption>(res.data, 'id', 'parentId');
  treeOptions.value.push(data);
};

/** 递归收集所有节点 key，用于「展开全部」的受控展开态 */
const collectKeys = (data: TreeVO[]): Array<string | number> => {
  const keys: Array<string | number> = [];
  data.forEach((item) => {
    keys.push(item.id);
    if (item.children && item.children.length > 0) keys.push(...collectKeys(item.children));
  });
  return keys;
};

// 取消按钮
const cancel = () => {
  reset();
  dialog.visible = false;
};

// 表单重置：t-form.reset() 清校验态，随后以 initFormData 覆盖
const reset = () => {
  treeFormRef.value?.reset();
  form.value = { ...initFormData };
};

/** 搜索按钮操作 */
const handleQuery = () => {
  getList();
};

/** 重置按钮操作 */
const resetQuery = () => {
  queryFormRef.value?.reset();
  handleQuery();
};

/** 新增按钮操作 */
const handleAdd = (row?: TreeVO) => {
  reset();
  getTreeselect();
  if (row && row.id) {
    form.value.parentId = row.id;
  } else {
    form.value.parentId = 0;
  }
  dialog.visible = true;
  dialog.title = '添加测试树';
};

/** 展开/折叠操作：受控 expandedTreeKeys，替代 EP 的 toggleRowExpansion 递归 */
const handleToggleExpandAll = () => {
  isExpandAll.value = !isExpandAll.value;
  expandedTreeKeys.value = isExpandAll.value ? collectKeys(treeList.value) : [];
};

/** 修改按钮操作 */
const handleUpdate = async (row: TreeVO) => {
  reset();
  await getTreeselect();
  if (row) {
    form.value.parentId = row.id;
  }
  const res = await getTree(row.id);
  Object.assign(form.value, res.data);
  dialog.visible = true;
  dialog.title = '修改测试树';
};

/** 提交按钮：t-form.validate() 失败 resolve(false)，不 reject（DOC/35 §5） */
const submitForm = async () => {
  const result = await treeFormRef.value?.validate().catch(() => false);
  const valid = result === true || result === undefined;
  if (!valid) return;
  buttonLoading.value = true;
  if (form.value.id) {
    await updateTree(form.value).finally(() => (buttonLoading.value = false));
  } else {
    await addTree(form.value).finally(() => (buttonLoading.value = false));
  }
  proxy?.$modal.msgSuccess('操作成功');
  dialog.visible = false;
  await getList();
};

/** 删除按钮操作 */
const handleDelete = async (row: TreeVO) => {
  await proxy?.$modal.confirm('是否确认删除测试树编号为"' + row.id + '"的数据项？');
  loading.value = true;
  await delTree(row.id).finally(() => (loading.value = false));
  await getList();
  proxy?.$modal.msgSuccess('删除成功');
};

onMounted(() => {
  getList();
});
</script>
