<template>
  <!-- 导入表 -->
  <t-dialog v-model:visible="visible" top="5vh" width="1100px" attach="body">
    <template #header>导入表</template>
    <t-form ref="queryFormRef" :data="queryParams" layout="inline" label-width="auto">
      <t-form-item label="数据源" name="dataName">
        <t-select v-model="queryParams.dataName" filterable placeholder="请选择/输入数据源名称" :options="dataNameOptions" />
      </t-form-item>
      <t-form-item label="表名称" name="tableName">
        <t-input v-model="queryParams.tableName" placeholder="请输入表名称" clearable @enter="handleQuery" />
      </t-form-item>
      <t-form-item label="表描述" name="tableComment">
        <t-input v-model="queryParams.tableComment" placeholder="请输入表描述" clearable @enter="handleQuery" />
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
    <t-row>
      <t-table
        row-key="tableName"
        bordered
        :data="dbTableList"
        :columns="dbTableColumns"
        :max-height="260"
        :resizable="true"
        size="small"
        cell-empty-content="-"
        :selected-row-keys="tables"
        :select-on-row-click="true"
        @select-change="handleSelectionChange"
      />
      <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
    </t-row>
    <template #footer>
      <div class="dialog-footer">
        <t-button theme="primary" @click="handleImportTable">确 定</t-button>
        <t-button theme="default" variant="outline" @click="visible = false">取 消</t-button>
      </div>
    </template>
  </t-dialog>
</template>

<script setup lang="ts">
import { listDbTable, importTable, getDataNames } from '@/api/tool/gen';
import { DbTableQuery, DbTableVO } from '@/api/tool/gen/types';
import { RefreshIcon, SearchIcon } from 'tdesign-icons-vue-next';

const total = ref(0);
const visible = ref(false);
const tables = ref<Array<string>>([]);
const dbTableList = ref<Array<DbTableVO>>([]);
const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const queryFormRef = ref<any>();

const queryParams = reactive<DbTableQuery>({
  pageNum: 1,
  pageSize: 10,
  dataName: '',
  tableName: '',
  tableComment: ''
});
const dataNameList = ref<Array<string>>([]);
// t-select 用 { label, value } 数组
const dataNameOptions = computed(() => dataNameList.value.map((item) => ({ label: item, value: item })));

/** 导入表列表列配置：t-table 用 columns 数组 */
const dbTableColumns: any[] = [
  { colKey: 'row-select', type: 'multiple', width: 55, align: 'center' },
  { colKey: 'tableName', title: '表名称', minWidth: 160, ellipsis: true },
  { colKey: 'tableComment', title: '表描述', minWidth: 160, ellipsis: true },
  { colKey: 'createTime', title: '创建时间', minWidth: 160 },
  { colKey: 'updateTime', title: '更新时间', minWidth: 160 }
];

const emit = defineEmits(['ok']);

/** 查询参数列表 */
const show = (dataName: string) => {
  getDataNames().then((res) => {
    if (res.code == 200) {
      dataNameList.value = res.data;
      if (dataName) {
        queryParams.dataName = dataName;
      } else {
        queryParams.dataName = dataNameList.value[0];
      }
      getList();
      visible.value = true;
    }
  });
};
/** 多选框选中数据：t-table 的 select-change 回传选中的 rowKey（rowKey=tableName），直接作为表名列表 */
const handleSelectionChange = (keys: Array<string | number>) => {
  tables.value = keys.map((item) => String(item));
};
/** 查询表数据 */
const getList = async () => {
  const res = await listDbTable(queryParams);
  dbTableList.value = res.rows;
  total.value = res.total;
};
/** 搜索按钮操作 */
const handleQuery = () => {
  queryParams.pageNum = 1;
  getList();
};
/** 重置按钮操作：TDesign 的 reset 替代 EP 的 resetFields */
const resetQuery = () => {
  queryFormRef.value?.reset();
  handleQuery();
};
/** 导入按钮操作 */
const handleImportTable = async () => {
  const tableNames = tables.value.join(',');
  if (tableNames == '') {
    proxy?.$modal.msgError('请选择要导入的表');
    return;
  }
  const res = await importTable({ tables: tableNames, dataName: queryParams.dataName });
  proxy?.$modal.msgSuccess(res.msg);
  if (res.code === 200) {
    visible.value = false;
    emit('ok');
  }
};

defineExpose({
  show
});
</script>
