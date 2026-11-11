<template>
  <div class="p-2">
    <transition :enter-active-class="proxy?.animate.searchAnimate.enter" :leave-active-class="proxy?.animate.searchAnimate.leave">
      <div v-show="showSearch" class="mb-[10px]">
        <t-card hover-shadow>
          <t-form ref="queryFormRef" :data="queryParams" layout="inline">
            <t-form-item label="key键" name="testKey">
              <t-input v-model="queryParams.testKey" placeholder="请输入key键" clearable @enter="handleQuery" />
            </t-form-item>
            <t-form-item label="值" name="value">
              <t-input v-model="queryParams.value" placeholder="请输入值" clearable @enter="handleQuery" />
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
            <t-button v-hasPermi="['demo:demo:add']" theme="primary" variant="outline" @click="handleAdd">
              <template #icon><PlusIcon /></template>
              新增
            </t-button>
            <t-button v-hasPermi="['demo:demo:edit']" theme="success" variant="outline" :disabled="single" @click="handleUpdate()">
              <template #icon><Edit1Icon /></template>
              修改
            </t-button>
            <t-button v-hasPermi="['demo:demo:remove']" theme="danger" variant="outline" :disabled="multiple" @click="handleDelete()">
              <template #icon><DeleteIcon /></template>
              删除
            </t-button>
            <t-button v-hasPermi="['demo:demo:export']" theme="warning" variant="outline" @click="handleExport">
              <template #icon><DownloadIcon /></template>
              导出
            </t-button>
          </div>
          <right-toolbar v-model:show-search="showSearch" @query-table="getList"></right-toolbar>
        </div>
      </template>

      <t-table
        class="erp-table"
        row-key="id"
        :data="demoList"
        :columns="demoColumns"
        :loading="loading"
        :hover="true"
        :resizable="true"
        bordered
        size="small"
        cell-empty-content="-"
        :selected-row-keys="ids"
        @select-change="handleSelectionChange"
      >
        <template #operation="{ row }">
          <div class="flex items-center justify-center gap-1">
            <!-- 权限判断挂外层 t-tooltip：v-hasPermi 在 mounted 里 removeChild，会与 t-tooltip 包裹结构打架 -->
            <t-tooltip v-if="checkPermi(['demo:demo:edit'])" content="修改" placement="top">
              <t-button theme="primary" variant="text" shape="square" size="small" @click="handleUpdate(row)">
                <template #icon><Edit1Icon /></template>
              </t-button>
            </t-tooltip>
            <t-tooltip v-if="checkPermi(['demo:demo:remove'])" content="删除" placement="top">
              <t-button theme="danger" variant="text" shape="square" size="small" @click="handleDelete(row)">
                <template #icon><DeleteIcon /></template>
              </t-button>
            </t-tooltip>
          </div>
        </template>
      </t-table>

      <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
    </t-card>
    <!-- 添加或修改测试单对话框 -->
    <t-dialog v-model:visible="dialog.visible" width="500px" attach="body">
      <template #header>{{ dialog.title }}</template>
      <t-form ref="demoFormRef" :data="form" :rules="rules" label-width="80px">
        <t-form-item label="部门id" name="deptId">
          <t-input v-model="form.deptId" placeholder="请输入部门id" />
        </t-form-item>
        <t-form-item label="用户id" name="userId">
          <t-input v-model="form.userId" placeholder="请输入用户id" />
        </t-form-item>
        <t-form-item label="排序号" name="orderNum">
          <t-input v-model="form.orderNum" placeholder="请输入排序号" />
        </t-form-item>
        <t-form-item label="key键" name="testKey">
          <t-input v-model="form.testKey" placeholder="请输入key键" />
        </t-form-item>
        <t-form-item label="值" name="value">
          <t-input v-model="form.value" placeholder="请输入值" />
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

<script setup name="Demo" lang="ts">
import { listDemo, getDemo, delDemo, addDemo, updateDemo } from '@/api/demo/demo';
import { DemoVO, DemoQuery, DemoForm } from '@/api/demo/demo/types';
import { checkPermi } from '@/utils/permission';
// TDesign 图标必须显式 import，不会自动解析（DOC/35 §3）
import { DeleteIcon, DownloadIcon, Edit1Icon, PlusIcon, RefreshIcon, SearchIcon } from 'tdesign-icons-vue-next';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const demoList = ref<DemoVO[]>([]);
const buttonLoading = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref<Array<string | number>>([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);

const queryFormRef = ref<any>();
const demoFormRef = ref<any>();

const dialog = reactive<DialogOption>({
  visible: false,
  title: ''
});

const initFormData: DemoForm = {
  id: undefined,
  deptId: undefined,
  userId: undefined,
  orderNum: undefined,
  testKey: undefined,
  value: undefined
};
const data = reactive<{ form: DemoForm; queryParams: DemoQuery }>({
  form: { ...initFormData },
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    deptId: undefined,
    userId: undefined,
    orderNum: undefined,
    testKey: undefined,
    value: undefined
  }
});

/** t-form 的 rules 形状与 EP 的 ElFormRules 不兼容，用宽松类型声明 */
const rules: Record<string, any> = {
  id: [{ required: true, message: '主键不能为空', trigger: 'blur' }],
  deptId: [{ required: true, message: '部门id不能为空', trigger: 'blur' }],
  userId: [{ required: true, message: '用户id不能为空', trigger: 'blur' }],
  orderNum: [{ required: true, message: '排序号不能为空', trigger: 'blur' }],
  testKey: [{ required: true, message: 'key键不能为空', trigger: 'blur' }],
  value: [{ required: true, message: '值不能为空', trigger: 'blur' }]
};

const { queryParams, form } = toRefs(data);

/** 测试单列表列配置：t-table 用 columns 数组，cell 字符串与 #插槽同名（DOC/35 §4） */
const demoColumns: any[] = [
  { colKey: 'row-select', type: 'multiple', width: 55, align: 'center' },
  { colKey: 'id', title: '主键', align: 'center' },
  { colKey: 'deptId', title: '部门id', align: 'center' },
  { colKey: 'userId', title: '用户id', align: 'center' },
  { colKey: 'orderNum', title: '排序号', align: 'center' },
  { colKey: 'testKey', title: 'key键', align: 'center' },
  { colKey: 'value', title: '值', align: 'center' },
  { colKey: 'operation', title: '操作', align: 'center', cell: 'operation' }
];

/** 查询测试单列表 */
const getList = async () => {
  loading.value = true;
  const res = await listDemo(queryParams.value);
  demoList.value = res.rows;
  total.value = res.total;
  loading.value = false;
};

/** 取消按钮 */
const cancel = () => {
  reset();
  dialog.visible = false;
};

/** 表单重置：t-form.reset() 清校验态，随后以 initFormData 覆盖，避免空串写回提交字段 */
const reset = () => {
  demoFormRef.value?.reset();
  form.value = { ...initFormData };
};

/** 搜索按钮操作 */
const handleQuery = () => {
  queryParams.value.pageNum = 1;
  getList();
};

/** 重置按钮操作 */
const resetQuery = () => {
  queryFormRef.value?.reset();
  handleQuery();
};

/** 多选框选中数据：t-table 的 select-change 只回传选中的 rowKey（rowKey=id） */
const handleSelectionChange = (keys: Array<string | number>) => {
  ids.value = keys;
  single.value = keys.length != 1;
  multiple.value = !keys.length;
};

/** 新增按钮操作 */
const handleAdd = () => {
  reset();
  dialog.visible = true;
  dialog.title = '添加测试单';
};

/** 修改按钮操作 */
const handleUpdate = async (row?: DemoVO) => {
  reset();
  const _id = row?.id || ids.value[0];
  const res = await getDemo(_id);
  Object.assign(form.value, res.data);
  dialog.visible = true;
  dialog.title = '修改测试单';
};

/** 提交按钮：t-form.validate() 失败 resolve(false)，不 reject（DOC/35 §5） */
const submitForm = async () => {
  const result = await demoFormRef.value?.validate().catch(() => false);
  const valid = result === true || result === undefined;
  if (!valid) return;
  buttonLoading.value = true;
  if (form.value.id) {
    await updateDemo(form.value).finally(() => (buttonLoading.value = false));
  } else {
    await addDemo(form.value).finally(() => (buttonLoading.value = false));
  }
  proxy?.$modal.msgSuccess('修改成功');
  dialog.visible = false;
  await getList();
};

/** 删除按钮操作 */
const handleDelete = async (row?: DemoVO) => {
  const _ids = row?.id || ids.value;
  await proxy?.$modal.confirm('是否确认删除测试单编号为"' + _ids + '"的数据项？').finally(() => (loading.value = false));
  await delDemo(_ids);
  proxy?.$modal.msgSuccess('删除成功');
  await getList();
};

/** 导出按钮操作 */
const handleExport = () => {
  proxy?.download(
    'demo/demo/export',
    {
      ...queryParams.value
    },
    `demo_${new Date().getTime()}.xlsx`
  );
};

onMounted(() => {
  getList();
});
</script>
