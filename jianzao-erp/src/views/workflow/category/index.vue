<template>
  <div class="p-2">
    <transition :enter-active-class="proxy?.animate.searchAnimate.enter" :leave-active-class="proxy?.animate.searchAnimate.leave">
      <div v-show="showSearch" class="mb-[10px]">
        <t-card>
          <t-form ref="queryFormRef" :data="queryParams" layout="inline" class="erp-filter-row">
            <t-form-item label="分类名称" name="categoryName">
              <t-input v-model="queryParams.categoryName" placeholder="请输入分类名称" clearable @enter="handleQuery" />
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
        <div class="category-toolbar">
          <t-button v-hasPermi="['workflow:category:add']" theme="primary" variant="outline" @click="handleAdd()">
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
        row-key="categoryId"
        :data="categoryList"
        :columns="categoryColumns"
        :loading="loading"
        bordered
        size="small"
        cell-empty-content="-"
        :resizable="true"
        :tree="{ childrenKey: 'children', checkStrictly: true }"
        :expanded-tree-nodes="expandedTreeKeys"
        @expanded-tree-nodes-change="(keys: Array<string | number>) => (expandedTreeKeys = keys)"
      >
        <!-- 权限判断挂外层 t-tooltip：v-hasPermi 在 mounted 里 removeChild，与 tooltip 包裹结构打架（DOC/36 P1） -->
        <template #operation="{ row }">
          <t-tooltip v-if="checkPermi(['workflow:category:edit'])" content="修改" placement="top">
            <t-button theme="primary" variant="text" shape="square" size="small" @click="handleUpdate(row)">
              <template #icon><Edit1Icon /></template>
            </t-button>
          </t-tooltip>
          <t-tooltip v-if="checkPermi(['workflow:category:add'])" content="新增" placement="top">
            <t-button theme="primary" variant="text" shape="square" size="small" @click="handleAdd(row)">
              <template #icon><PlusIcon /></template>
            </t-button>
          </t-tooltip>
          <t-tooltip v-if="checkPermi(['workflow:category:remove'])" content="删除" placement="top">
            <t-button theme="danger" variant="text" shape="square" size="small" @click="handleDelete(row)">
              <template #icon><DeleteIcon /></template>
            </t-button>
          </t-tooltip>
        </template>
      </EnhancedTable>
    </t-card>

    <t-dialog v-model:visible="dialog.visible" :header="dialog.title" width="500px" attach="body" destroy-on-close>
      <t-form ref="categoryFormRef" :data="form" :rules="rules" label-width="80px">
        <t-form-item label="上级分类" name="parentId">
          <!-- t-tree-select 用 keys 指定节点字段；check-strictly 走 treeProps（DOC/35 §3） -->
          <t-tree-select
            v-model="form.parentId"
            :data="categoryOptions"
            :keys="{ value: 'categoryId', label: 'categoryName', children: 'children' }"
            placeholder="请选择上级分类"
            clearable
            :tree-props="{ checkStrictly: true }"
          />
        </t-form-item>
        <t-row :gutter="16">
          <t-col :span="6">
            <t-form-item label="分类名称" name="categoryName">
              <t-input v-model="form.categoryName" placeholder="请输入分类名称" />
            </t-form-item>
          </t-col>
          <t-col :span="6">
            <t-form-item label="排序" name="orderNum">
              <t-input-number v-model="form.orderNum" theme="normal" :decimal-places="0" :min="0" style="width: 100%" />
            </t-form-item>
          </t-col>
        </t-row>
      </t-form>
      <template #footer>
        <div class="dialog-footer">
          <t-button theme="primary" :loading="buttonLoading" @click="submitForm">确 定</t-button>
          <t-button theme="default" variant="outline" @click="cancel">取 消</t-button>
        </div>
      </template>
    </t-dialog>
  </div>
</template>

<script setup name="Category" lang="ts">
import { listCategory, getCategory, delCategory, addCategory, updateCategory } from '@/api/workflow/category';
import { CategoryVO, CategoryQuery, CategoryForm } from '@/api/workflow/category/types';
import { checkPermi } from '@/utils/permission';
import { EnhancedTable } from 'tdesign-vue-next/es/table';
// TDesign 图标必须显式 import，不会自动解析（DOC/35 §3）
import { DeleteIcon, Edit1Icon, PlusIcon, RefreshIcon, SearchIcon, UnfoldMoreIcon } from 'tdesign-icons-vue-next';

type CategoryOption = {
  categoryId: number;
  categoryName: string;
  children?: CategoryOption[];
};

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const categoryList = ref<CategoryVO[]>([]);
const categoryOptions = ref<CategoryOption[]>([]);
const buttonLoading = ref(false);
const showSearch = ref(true);
const isExpandAll = ref(true);
const loading = ref(false);
/** 树表展开态：TDesign 用受控数组替代 EP 的 toggleRowExpansion 递归（DOC/35 §4） */
const expandedTreeKeys = ref<Array<string | number>>([]);

const queryFormRef = ref<any>();
const categoryFormRef = ref<any>();

const dialog = reactive<DialogOption>({
  visible: false,
  title: ''
});

const initFormData: CategoryForm = {
  categoryId: undefined,
  categoryName: '',
  parentId: undefined,
  orderNum: 0
};

const data = reactive<{ form: CategoryForm; queryParams: CategoryQuery }>({
  form: { ...initFormData },
  queryParams: {
    categoryName: undefined
  }
});

const { queryParams, form } = toRefs(data);

/** 表单校验规则：t-form 的 rules 形状与 EP 的 ElFormRules 不兼容，单独用 Record 声明（DOC/35 §5） */
const rules: Record<string, any> = {
  categoryId: [{ required: true, message: '流程分类ID不能为空', trigger: 'blur' }],
  parentId: [{ required: true, message: '请选择上级分类', trigger: 'change' }],
  categoryName: [{ required: true, message: '请输入分类名称', trigger: 'blur' }]
};

/** 分类树表列配置：t-table 用 columns 数组，cell 字符串与 #插槽同名（DOC/35 §5） */
const categoryColumns: any[] = [
  { colKey: 'categoryName', title: '分类名称', width: 260 },
  { colKey: 'orderNum', title: '显示顺序', align: 'center', width: 200 },
  { colKey: 'createTime', title: '创建时间', align: 'center', width: 180 },
  { colKey: 'operation', title: '操作', fixed: 'right', align: 'center', cell: 'operation' }
];

/** 查询流程分类列表 */
const getList = async () => {
  loading.value = true;
  const res = await listCategory(queryParams.value);
  const data = proxy?.handleTree<CategoryVO>(res.data, 'categoryId', 'parentId');
  if (data) {
    categoryList.value = data;
    // 先写展开态再换数据：增强表只在 data 变更时按当前 expandedTreeNodes 展平子行
    expandedTreeKeys.value = isExpandAll.value ? collectKeys(data) : [];
    loading.value = false;
  }
};

/** 递归收集所有分类 key，用于「展开全部」的受控展开态 */
const collectKeys = (data: CategoryVO[]): Array<string | number> => {
  const keys: Array<string | number> = [];
  data.forEach((item) => {
    keys.push(item.categoryId);
    if (item.children && item.children.length > 0) keys.push(...collectKeys(item.children));
  });
  return keys;
};

/** 查询流程分类下拉树结构 */
const getTreeselect = async () => {
  const res = await listCategory();
  categoryOptions.value = [];
  // 处理树形数据
  const data = proxy?.handleTree<CategoryOption>(res.data, 'categoryId', 'parentId');
  if (data) {
    categoryOptions.value = data; // 将处理后的树形数据赋值
  }
};

// 取消按钮
const cancel = () => {
  reset();
  dialog.visible = false;
};

// 表单重置：t-form 只清校验态，表单值以 initFormData 为准
const reset = () => {
  form.value = { ...initFormData };
  categoryFormRef.value?.clearValidate();
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
const handleAdd = (row?: CategoryVO) => {
  reset();
  getTreeselect();
  if (row?.categoryId) {
    form.value.parentId = row.categoryId;
  } else {
    form.value.parentId = undefined;
  }
  dialog.visible = true;
  dialog.title = '添加流程分类';
};

/** 展开/折叠操作：受控 expandedTreeKeys，替代 EP 的 toggleRowExpansion 递归 */
const handleToggleExpandAll = () => {
  isExpandAll.value = !isExpandAll.value;
  expandedTreeKeys.value = isExpandAll.value ? collectKeys(categoryList.value) : [];
};

/** 修改按钮操作 */
const handleUpdate = async (row: CategoryVO) => {
  reset();
  await getTreeselect();
  if (row != null) {
    form.value.parentId = row.parentId;
  }
  const res = await getCategory(row.categoryId);
  Object.assign(form.value, res.data);
  dialog.visible = true;
  dialog.title = '修改流程分类';
};

/** 提交按钮：t-form.validate() 失败 resolve(false)，不 reject（DOC/35 §5） */
const submitForm = async () => {
  const result = await categoryFormRef.value?.validate().catch(() => false);
  const valid = result === true || result === undefined;
  if (!valid) return;
  buttonLoading.value = true;
  if (form.value.categoryId) {
    await updateCategory(form.value).finally(() => (buttonLoading.value = false));
  } else {
    await addCategory(form.value).finally(() => (buttonLoading.value = false));
  }
  proxy?.$modal.msgSuccess('操作成功');
  dialog.visible = false;
  getList();
};

/** 删除按钮操作 */
const handleDelete = async (row: CategoryVO) => {
  await proxy?.$modal.confirm('是否确认删除"' + row.categoryName + '"的分类？');
  loading.value = true;
  await delCategory(row.categoryId).finally(() => (loading.value = false));
  await getList();
  proxy?.$modal.msgSuccess('删除成功');
};

onMounted(() => {
  getList();
});
</script>

<style lang="scss" scoped>
.category-toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}
</style>
