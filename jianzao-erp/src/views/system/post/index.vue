<template>
  <div class="p-2">
    <t-row :gutter="20">
      <!-- 部门树：TDesign 12 栅格，span 由原 24 栅格减半（4→2，20→10） -->
      <t-col :span="2">
        <t-card hover-shadow>
          <t-input v-model="deptName" placeholder="请输入部门名称" clearable>
            <template #prefix-icon><SearchIcon /></template>
          </t-input>
          <t-tree
            ref="deptTreeRef"
            class="mt-2"
            :data="deptOptions"
            :keys="{ value: 'id', label: 'label', children: 'children' }"
            :activable="true"
            :actived="deptActived"
            :expand-on-click-node="false"
            :filter="deptFilter"
            :hover="true"
            expand-all
            @click="handleNodeClick"
          />
        </t-card>
      </t-col>
      <t-col :span="10">
        <transition :enter-active-class="proxy?.animate.searchAnimate.enter" :leave-active-class="proxy?.animate.searchAnimate.leave">
          <div v-show="showSearch" class="mb-[10px]">
            <t-card hover-shadow>
              <t-form ref="queryFormRef" :data="queryParams" layout="inline" @submit="handleQuery">
                <t-form-item label="岗位编码" name="postCode">
                  <t-input v-model="queryParams.postCode" placeholder="请输入岗位编码" clearable class="query-w-md" @enter="handleQuery" />
                </t-form-item>
                <t-form-item label="类别编码" name="postCategory">
                  <t-input v-model="queryParams.postCategory" placeholder="请输入类别编码" clearable class="query-w-md" @enter="handleQuery" />
                </t-form-item>
                <t-form-item label="岗位名称" name="postName">
                  <t-input v-model="queryParams.postName" placeholder="请输入岗位名称" clearable class="query-w-md" @enter="handleQuery" />
                </t-form-item>
                <t-form-item label="部门" name="deptId">
                  <t-tree-select
                    v-model="queryParams.deptId"
                    :data="deptOptions"
                    :keys="{ value: 'id', label: 'label', children: 'children' }"
                    placeholder="请选择部门"
                    clearable
                    class="query-w-md"
                    :tree-props="{ checkStrictly: true }"
                  />
                </t-form-item>
                <t-form-item label="状态" name="status">
                  <t-select v-model="queryParams.status" placeholder="岗位状态" clearable class="query-w-sm" :options="sys_normal_disable" />
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
                <t-button v-hasPermi="['system:post:add']" theme="primary" variant="outline" @click="handleAdd">
                  <template #icon><PlusIcon /></template>
                  新增
                </t-button>
                <t-button v-hasPermi="['system:post:edit']" theme="success" variant="outline" :disabled="single" @click="handleUpdate()">
                  <template #icon><Edit1Icon /></template>
                  修改
                </t-button>
                <t-button v-hasPermi="['system:post:remove']" theme="danger" variant="outline" :disabled="multiple" @click="handleDelete()">
                  <template #icon><DeleteIcon /></template>
                  删除
                </t-button>
                <t-button v-hasPermi="['system:post:export']" theme="warning" variant="outline" @click="handleExport">
                  <template #icon><DownloadIcon /></template>
                  导出
                </t-button>
              </div>
              <right-toolbar v-model:show-search="showSearch" @query-table="getList"></right-toolbar>
            </div>
          </template>
          <t-table
            class="erp-table"
            row-key="postId"
            :data="postList"
            :columns="listColumns"
            :loading="loading"
            :hover="true"
            :resizable="true"
            size="small"
            cell-empty-content="-"
            :selected-row-keys="ids"
            @select-change="handleSelectionChange"
            @column-resize-change="(ctx: any) => saveColumnWidths('system-post/list', ctx.columnsWidth)"
          >
            <template #status="{ row }">
              <dict-tag :options="sys_normal_disable" :value="row.status" />
            </template>
            <template #createTime="{ row }">
              <span>{{ proxy.parseTime(row.createTime) }}</span>
            </template>
            <template #operation="{ row }">
              <div class="flex items-center justify-center gap-1">
                <!-- 权限判断改挂外层：v-hasPermi 在 mounted 里删节点，会与 t-tooltip 的包裹结构打架 -->
                <t-tooltip v-if="checkPermi(['system:post:edit'])" content="修改" placement="top">
                  <t-button theme="primary" variant="text" shape="square" size="small" @click="handleUpdate(row)">
                    <template #icon><Edit1Icon /></template>
                  </t-button>
                </t-tooltip>
                <t-tooltip v-if="checkPermi(['system:post:remove'])" content="删除" placement="top">
                  <t-button theme="danger" variant="text" shape="square" size="small" @click="handleDelete(row)">
                    <template #icon><DeleteIcon /></template>
                  </t-button>
                </t-tooltip>
              </div>
            </template>
          </t-table>

          <pagination
            v-show="total > 0"
            v-model:page="queryParams.pageNum"
            v-model:limit="queryParams.pageSize"
            :total="total"
            @pagination="getList"
          />
        </t-card>

        <!-- 添加或修改岗位对话框 -->
        <t-dialog v-model:visible="dialog.visible" :header="dialog.title" width="500px" attach="body" :destroy-on-close="true">
          <t-form ref="postFormRef" :data="form" :rules="rules" label-align="top">
            <t-form-item label="岗位名称" name="postName">
              <t-input v-model="form.postName" placeholder="请输入岗位名称" />
            </t-form-item>
            <t-form-item label="部门" name="deptId">
              <t-tree-select
                v-model="form.deptId"
                :data="deptOptions"
                :keys="{ value: 'id', label: 'label', children: 'children' }"
                placeholder="请选择部门"
                :tree-props="{ checkStrictly: true }"
              />
            </t-form-item>
            <t-form-item label="岗位编码" name="postCode">
              <t-input v-model="form.postCode" placeholder="请输入编码名称" />
            </t-form-item>
            <t-form-item label="类别编码" name="postCategory">
              <t-input v-model="form.postCategory" placeholder="请输入类别编码" />
            </t-form-item>
            <t-form-item label="岗位顺序" name="postSort">
              <t-input-number v-model="form.postSort" :decimal-places="0" :min="0" theme="normal" style="width: 100%" />
            </t-form-item>
            <t-form-item label="岗位状态" name="status">
              <t-radio-group v-model="form.status">
                <t-radio v-for="dict in sys_normal_disable" :key="dict.value" :value="dict.value">{{ dict.label }}</t-radio>
              </t-radio-group>
            </t-form-item>
            <t-form-item label="备注" name="remark">
              <t-textarea v-model="form.remark" :autosize="{ minRows: 2, maxRows: 4 }" placeholder="请输入内容" />
            </t-form-item>
          </t-form>
          <template #footer>
            <t-button theme="primary" @click="submitForm">确 定</t-button>
            <t-button theme="default" variant="outline" @click="cancel">取 消</t-button>
          </template>
        </t-dialog>
      </t-col>
    </t-row>
  </div>
</template>

<script setup name="Post" lang="ts">
import { listPost, addPost, delPost, getPost, updatePost, deptTreeSelect } from '@/api/system/post';
import { PostForm, PostQuery, PostVO } from '@/api/system/post/types';
import { DeptTreeVO } from '@/api/system/dept/types';
import { checkPermi } from '@/utils/permission';
// 列宽记忆：拖拽后按表名存 localStorage，进页面还原（见 hooks/useTableColumnWidth）
import { mergeSavedWidths, saveColumnWidths } from '@/hooks/useTableColumnWidth';
import { DeleteIcon, DownloadIcon, Edit1Icon, PlusIcon, RefreshIcon, SearchIcon } from 'tdesign-icons-vue-next';
import type { TreeNodeModel } from 'tdesign-vue-next';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const { sys_normal_disable } = toRefs<any>(proxy?.useDict('sys_normal_disable'));

const postList = ref<PostVO[]>([]);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref<Array<number | string>>([]);
/** 工具栏「修改」禁用条件：勾选行数不是 1；「删除」禁用条件：一行未勾选（原由 selection-change 手写同步，改由 ids 派生） */
const single = computed(() => ids.value.length !== 1);
const multiple = computed(() => ids.value.length === 0);
const total = ref(0);
const deptName = ref('');
const deptOptions = ref<DeptTreeVO[]>([]);
// t-tree 的激活节点为数组，用单元素数组维持原部门树的单选高亮语义
const deptActived = ref<Array<string | number>>([]);
const deptTreeRef = ref<any>();
const postFormRef = ref<any>();
const queryFormRef = ref<any>();

const dialog = reactive<DialogOption>({
  visible: false,
  title: ''
});

const initFormData: PostForm = {
  postId: undefined,
  deptId: undefined,
  postCode: '',
  postName: '',
  postCategory: '',
  postSort: 0,
  status: '0',
  remark: ''
};

/** t-form 的 rules 形状与 EP 的 ElFormRules 不兼容，用宽松类型声明 */
const rules: Record<string, any> = {
  postName: [{ required: true, message: '岗位名称不能为空', trigger: 'blur' }],
  postCode: [{ required: true, message: '岗位编码不能为空', trigger: 'blur' }],
  deptId: [{ required: true, message: '部门不能为空', trigger: 'blur' }],
  postSort: [{ required: true, message: '岗位顺序不能为空', trigger: 'blur' }]
};

const data = reactive<{ form: PostForm; queryParams: PostQuery }>({
  form: { ...initFormData },
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    deptId: undefined,
    belongDeptId: undefined,
    postCode: '',
    postName: '',
    postCategory: '',
    status: ''
  }
});

const { queryParams, form } = toRefs<{ form: PostForm; queryParams: PostQuery }>(data);

/** 主列表列配置：t-table 用 columns 数组，插槽名与 cell 字符串同名（DOC/35 §5） */
const listColumns: any[] = [
  { colKey: 'row-select', type: 'multiple', width: 46, align: 'center' },
  { colKey: 'postCode', title: '岗位编码', align: 'center', minWidth: 120, ellipsis: true },
  { colKey: 'postCategory', title: '类别编码', align: 'center', minWidth: 120, ellipsis: true },
  { colKey: 'postName', title: '岗位名称', align: 'center', minWidth: 120, ellipsis: true },
  { colKey: 'deptName', title: '部门', align: 'center', minWidth: 120, ellipsis: true },
  { colKey: 'postSort', title: '排序', align: 'center', width: 80, ellipsis: true },
  { colKey: 'status', title: '状态', align: 'center', width: 90, cell: 'status' },
  { colKey: 'createTime', title: '创建时间', align: 'center', width: 180, cell: 'createTime' },
  { colKey: 'operation', title: '操作', align: 'center', width: 100, cell: 'operation' }
];
mergeSavedWidths('system-post/list', listColumns);

/** 部门树过滤条件：t-tree 的 filter 是响应式 prop，保持原按 label 子串匹配的逻辑；未录入时不传 filter 显示全部节点 */
const deptFilter = computed<((node: TreeNodeModel) => boolean) | undefined>(() => {
  if (!deptName.value) return undefined;
  return (node: TreeNodeModel) => String(node.label ?? '').indexOf(deptName.value) !== -1;
});

/** 查询部门下拉树结构 */
const getTreeSelect = async () => {
  const res = await deptTreeSelect();
  deptOptions.value = res.data;
};

/** 节点单击事件：t-tree 的 click 上下文是 { node, e }，node.data 为原始数据；同时清空树选择下拉的选中值 */
const handleNodeClick = (context: { node: TreeNodeModel }) => {
  queryParams.value.belongDeptId = (context.node.data as any).id;
  queryParams.value.deptId = undefined;
  handleQuery();
};

/** 查询岗位列表 */
const getList = async () => {
  loading.value = true;
  const res = await listPost(queryParams.value);
  postList.value = res.rows;
  total.value = res.total;
  // 受控选中：换数据后清空勾选，保持 EP 的 el-table「重取即丢选中」行为
  ids.value = [];
  loading.value = false;
};

/** 取消按钮 */
const cancel = () => {
  reset();
  dialog.visible = false;
};

/** 表单重置：t-form 的 reset() 默认按 empty 清值，这里以 initFormData 为准，只清校验态 */
const reset = () => {
  form.value = { ...initFormData };
  postFormRef.value?.clearValidate();
};

/** 搜索按钮操作 */
const handleQuery = () => {
  queryParams.value.pageNum = 1;
  if (queryParams.value.deptId) {
    queryParams.value.belongDeptId = undefined;
  }
  getList();
};

/** 重置按钮操作 */
const resetQuery = () => {
  queryFormRef.value?.reset();
  queryParams.value.pageNum = 1;
  queryParams.value.deptId = undefined;
  // t-tree 的 actived 为受控数组，清空即取消部门树高亮
  deptActived.value = [];
  /** 清空左边部门树选中值 */
  queryParams.value.belongDeptId = undefined;
  handleQuery();
};

/** 多选框选中数据：t-table 的 select-change 只回传选中的 rowKey（rowKey=postId） */
const handleSelectionChange = (keys: Array<string | number>) => {
  ids.value = keys;
};

/** 新增按钮操作 */
const handleAdd = () => {
  reset();
  dialog.visible = true;
  dialog.title = '添加岗位';
};

/** 修改按钮操作 */
const handleUpdate = async (row?: PostVO) => {
  reset();
  const postId = row?.postId || ids.value[0];
  const res = await getPost(postId);
  Object.assign(form.value, res.data);
  dialog.visible = true;
  dialog.title = '修改岗位';
};

/** 提交按钮 */
const submitForm = async () => {
  // TDesign t-form.validate() 成功 resolve(true)，失败 resolve(false)（不 reject）；兼容两种返回
  const result = await postFormRef.value?.validate().catch(() => false);
  const valid = result === true || result === undefined;
  if (!valid) return;
  form.value.postId ? await updatePost(form.value) : await addPost(form.value);
  proxy?.$modal.msgSuccess('操作成功');
  dialog.visible = false;
  await getList();
};

/** 删除按钮操作 */
const handleDelete = async (row?: PostVO) => {
  const postIds = row?.postId || ids.value;
  await proxy?.$modal.confirm('是否确认删除岗位编号为"' + postIds + '"的数据项？');
  await delPost(postIds);
  await getList();
  proxy?.$modal.msgSuccess('删除成功');
};

/** 导出按钮操作 */
const handleExport = () => {
  proxy?.download(
    'system/post/export',
    {
      ...queryParams.value
    },
    `post_${new Date().getTime()}.xlsx`
  );
};

onMounted(() => {
  getTreeSelect(); // 初始化部门数据
  getList();
});
</script>
