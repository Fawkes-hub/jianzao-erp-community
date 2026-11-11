<template>
  <div class="p-2">
    <t-tabs v-model="activeTab" class="!mb-[10px]" @change="handleTabChange">
      <t-tab-panel label="成品" value="PRODUCT" />
      <t-tab-panel label="原料" value="MATERIAL" />
      <t-tab-panel label="库存预警" value="ALERT" />
    </t-tabs>

    <template v-if="activeTab !== 'ALERT'">
    <t-row :gutter="20">
      <t-col :span="12">
        <transition :enter-active-class="proxy?.animate.searchAnimate.enter" :leave-active-class="proxy?.animate.searchAnimate.leave">
          <div v-show="showSearch" class="erp-row-container">
            <t-card :bordered="false" class="erp-filter-card" title="筛选条件">
              <t-form ref="queryFormRef" :data="queryParams" class="erp-filter-row" label-align="right" :label-width="'auto'">
                <t-form-item label="产品关键字" name="keyword">
                  <t-input v-model="queryParams.keyword" placeholder="请输入产品编码或名称" clearable class="query-w-lg" @enter="handleQuery" />
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

        <div class="erp-surface erp-panel">
          <div class="erp-panel__head">
            <span class="erp-text-secondary">
              {{ activeType === 'PRODUCT' ? '成品库存' : '原料库存' }}（共 {{ stockList.length }} 条，不分页）
            </span>
            <right-toolbar v-model:show-search="showSearch" @query-table="getList" />
          </div>

          <t-table
            class="erp-table"
            row-key="productId"
            :data="stockList"
            :columns="balanceColumns"
            :loading="loading"
            :hover="true"
            :resizable="true"
            size="small"
            bordered
            cell-empty-content="-"
            :empty="'暂无库存数据'"
            @column-resize-change="(ctx: any) => saveColumnWidths('stock/balance', ctx.columnsWidth)"
          >
            <template #qty="{ row }">{{ formatQty(row.qty) }}</template>
            <template #avgCost="{ row }">
              <div>{{ formatMoney(row.avgCost) }}</div>
              <t-tooltip v-if="row.costMissing" content="有库存但成本为 0，出库毛利将按 100% 计" placement="top">
                <t-tag theme="danger" size="small">成本缺失</t-tag>
              </t-tooltip>
            </template>
            <template #stockAmount="{ row }">{{ formatMoney(row.stockAmount) }}</template>
            <template #operation="{ row }">
              <t-tooltip v-if="checkPermi(['erp:stock:query'])" content="明细" placement="top">
                <t-button theme="primary" variant="text" shape="square" size="small" @click="handleDetail(row)">
                  <template #icon><BrowseIcon /></template>
                </t-button>
              </t-tooltip>
            </template>
          </t-table>
        </div>
      </t-col>
    </t-row>

    </template>

    <!-- 库存预警（DOC/25 §4.2） -->
    <t-row v-if="activeTab === 'ALERT'" :gutter="20">
      <t-col :span="12">
        <div class="erp-surface erp-panel">
          <div class="erp-panel__head">
            <span class="erp-text-secondary">库存上下限预警（共 {{ alertList.length }} 条，不分页）</span>
            <div class="flex items-center gap-2">
              <t-select v-model="alertType" placeholder="全部预警" clearable style="width: 140px" :options="alertTypeOptions" @change="loadAlerts" />
              <right-toolbar v-model:show-search="showSearch" @query-table="loadAlerts" />
            </div>
          </div>

          <t-table
            class="erp-table"
            row-key="productId"
            :data="alertList"
            :columns="alertColumns"
            :loading="alertLoading"
            :hover="true"
            :resizable="true"
            size="small"
            bordered
            cell-empty-content="-"
            :empty="'暂无预警：产品资料中设置库存上下限后，异常库存会出现在这里'"
            @column-resize-change="(ctx: any) => saveColumnWidths('stock/alert', ctx.columnsWidth)"
          >
            <template #minStock="{ row }">{{ row.minStock == null ? '-' : formatQty(row.minStock) }}</template>
            <template #maxStock="{ row }">{{ row.maxStock == null ? '-' : formatQty(row.maxStock) }}</template>
            <template #totalQty="{ row }">{{ formatQty(row.totalQty) }}</template>
            <template #gap="{ row }">
              <span :class="row.alertType === 'LOW' ? 'text-red-500' : 'text-orange-500'">
                {{ row.alertType === 'LOW' ? '缺 ' : '超 ' }}{{ formatQty(Math.abs(Number(row.gap))) }}
              </span>
            </template>
          </t-table>
        </div>
      </t-col>
    </t-row>

    <!-- 库存流水明细 -->
    <t-drawer v-model:visible="drawer.visible" :header="drawer.title" size="65%" attach="body">
      <div class="px-4 pb-4">
        <t-descriptions :column="3" bordered size="small" class="mb-3">
          <t-descriptions-item label="产品编码">{{ drawer.productCode || '-' }}</t-descriptions-item>
          <t-descriptions-item label="产品名称">{{ drawer.productName || '-' }}</t-descriptions-item>
          <t-descriptions-item label="当前库存">{{ formatQty(drawer.qty) }}</t-descriptions-item>
        </t-descriptions>

        <t-table
          class="erp-table"
          row-key="id"
          :data="logList"
          :columns="logColumns"
          :loading="logLoading"
          :hover="true"
          :resizable="true"
          size="small"
          bordered
          cell-empty-content="-"
          :empty="'该产品暂无库存流水'"
          @column-resize-change="(ctx: any) => saveColumnWidths('stock/logs', ctx.columnsWidth)"
        >
          <template #bizDate="{ row }">{{ toDate(row.bizDate) }}</template>
          <template #bizType="{ row }">
            <t-tag :theme="bizTypeTheme(row.bizType)" size="small">{{ bizTypeMeta(row.bizType)?.label ?? row.bizType }}</t-tag>
          </template>
          <template #qty="{ row }">{{ formatQty(row.qty) }}</template>
          <template #unitCost="{ row }">{{ formatMoney(row.unitCost) }}</template>
          <template #amount="{ row }">{{ formatMoney(row.amount) }}</template>
          <template #balanceAfter="{ row }">{{ formatQty(row.balanceAfter) }}</template>
        </t-table>
      </div>
    </t-drawer>
  </div>
</template>

<script setup name="ErpStock" lang="ts">
import { listStockAlerts, listStockBalances, listStockLogs } from '@/api/erp/stock';
import { ERP_STOCK_BIZ_TYPE } from '@/api/erp/enums';
import type { ErpStockBizType } from '@/api/erp/enums';
import type { ErpAmount, ErpProductType, ErpStockAlertVO, ErpStockLogVO, ErpStockVO } from '@/api/erp/types';
import { formatMoney, formatQty } from '@/utils/erp/calc';
// 列宽记忆：拖拽后按表名存 localStorage，进页面还原（见 hooks/useTableColumnWidth）
import { mergeSavedWidths, saveColumnWidths } from '@/hooks/useTableColumnWidth';
import { checkPermi } from '@/utils/permission';
import { BrowseIcon, RefreshIcon, SearchIcon } from 'tdesign-icons-vue-next';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const router = useRouter();

/** 库存余额列表列配置：t-table 用 columns 数组，插槽名与 cell 字符串同名（DOC/35 §5） */
const balanceColumns: any[] = [
  { colKey: 'productCode', title: '产品编码', align: 'center', width: 140, ellipsis: true },
  { colKey: 'productName', title: '产品名称', align: 'center', minWidth: 160, ellipsis: true },
  { colKey: 'spec', title: '规格', align: 'center', width: 120, ellipsis: true },
  { colKey: 'unit', title: '单位', align: 'center', width: 70 },
  { colKey: 'qty', title: '当前库存', align: 'right', width: 120, cell: 'qty' },
  { colKey: 'avgCost', title: '平均成本', align: 'right', width: 120, cell: 'avgCost' },
  { colKey: 'stockAmount', title: '库存金额', align: 'right', width: 130, cell: 'stockAmount' },
  { colKey: 'operation', title: '操作', align: 'center', width: 100, cell: 'operation' }
];
mergeSavedWidths('stock/balance', balanceColumns);

/** 库存预警列表列配置：下限/上限缺失时显示 -，缺口按缺口方向着色 */
const alertColumns: any[] = [
  { colKey: 'productCode', title: '产品编码', align: 'center', width: 140, ellipsis: true },
  { colKey: 'productName', title: '产品名称', align: 'center', minWidth: 160, ellipsis: true },
  { colKey: 'spec', title: '规格', align: 'center', width: 120, ellipsis: true },
  { colKey: 'unit', title: '单位', align: 'center', width: 70 },
  { colKey: 'minStock', title: '下限', align: 'right', width: 100, cell: 'minStock' },
  { colKey: 'maxStock', title: '上限', align: 'right', width: 100, cell: 'maxStock' },
  { colKey: 'totalQty', title: '当前库存', align: 'right', width: 110, cell: 'totalQty' },
  { colKey: 'gap', title: '缺口', align: 'right', width: 110, cell: 'gap' }
];
mergeSavedWidths('stock/alert', alertColumns);

/** 库存流水列配置：单号/往来单位空值统一走 cell-empty-content */
const logColumns: any[] = [
  { colKey: 'bizDate', title: '日期', align: 'center', width: 110, cell: 'bizDate' },
  { colKey: 'bizType', title: '类型', align: 'center', width: 100, cell: 'bizType' },
  { colKey: 'bizNo', title: '单号', align: 'center', minWidth: 140, ellipsis: true },
  { colKey: 'partnerName', title: '往来单位', align: 'center', minWidth: 140, ellipsis: true },
  { colKey: 'qty', title: '数量', align: 'right', width: 110, cell: 'qty' },
  { colKey: 'unitCost', title: '单位成本', align: 'right', width: 110, cell: 'unitCost' },
  { colKey: 'amount', title: '金额', align: 'right', width: 120, cell: 'amount' },
  { colKey: 'balanceAfter', title: '结存数量', align: 'right', width: 110, cell: 'balanceAfter' }
];
mergeSavedWidths('stock/logs', logColumns);

const stockList = ref<ErpStockVO[]>([]);
const logList = ref<ErpStockLogVO[]>([]);
const loading = ref(true);
const logLoading = ref(false);
const showSearch = ref(true);
/** 查询表单：已换 t-form，ref 提供 reset()（EP 是 resetFields()） */
const queryFormRef = ref<any>();

/** 当前页签：成品 / 原料 / 预警 */
const activeTab = ref<'PRODUCT' | 'MATERIAL' | 'ALERT'>('PRODUCT');
const activeType = computed<ErpProductType | undefined>(() => (activeTab.value === 'ALERT' ? undefined : (activeTab.value as ErpProductType)));

/** 预警状态 */
const alertList = ref<ErpStockAlertVO[]>([]);
const alertLoading = ref(false);
const alertType = ref<'LOW' | 'HIGH' | undefined>(undefined);
/** 预警类型筛选项：t-select 需要 { label, value } 数组 */
const alertTypeOptions = [
  { label: '低于下限', value: 'LOW' },
  { label: '高于上限', value: 'HIGH' }
];

const loadAlerts = async () => {
  alertLoading.value = true;
  try {
    const res = await listStockAlerts({ type: alertType.value });
    alertList.value = res.data ?? [];
  } catch {
    alertList.value = [];
  } finally {
    alertLoading.value = false;
  }
};

const queryParams = reactive({
  keyword: ''
});

const drawer = reactive({
  visible: false,
  title: '库存流水明细',
  productCode: '',
  productName: '',
  qty: 0 as ErpAmount
});

/** 日期只取 yyyy-MM-dd */
const toDate = (value: string | undefined): string => (value ? String(value).slice(0, 10) : '');

/** 流水业务类型中文 */
const bizTypeMeta = (value?: string) => (value ? ERP_STOCK_BIZ_TYPE[value as ErpStockBizType] : undefined);

/** enums 的 type 沿用 EP 四色命名，t-tag 无 info 主题，统一落到 default */
const BIZ_TYPE_THEME: Record<string, 'default' | 'primary' | 'warning' | 'danger' | 'success'> = {
  primary: 'primary',
  success: 'success',
  warning: 'warning',
  danger: 'danger',
  info: 'default'
};

/** 流水业务类型标签主题 */
const bizTypeTheme = (value?: string) => BIZ_TYPE_THEME[bizTypeMeta(value)?.type ?? 'info'] ?? 'default';

/** 加载库存余额（后端返回全量数组，不分页，keyword / type 由后端过滤） */
const getList = async () => {
  loading.value = true;
  try {
    const res = await listStockBalances({
      keyword: queryParams.keyword.trim() || undefined,
      type: activeType.value
    });
    stockList.value = res.data ?? [];
  } catch {
    stockList.value = [];
  } finally {
    loading.value = false;
  }
};

/** 搜索按钮操作 */
const handleQuery = () => {
  getList();
};

/** 重置按钮操作 */
const resetQuery = () => {
  queryFormRef.value?.reset();
  queryParams.keyword = '';
  getList();
};

/** 切换成品/原料/预警页签 */
const handleTabChange = () => {
  if (activeTab.value === 'ALERT') {
    loadAlerts();
  } else {
    getList();
  }
};

/** 查看库存流水明细 */
const handleDetail = async (row: ErpStockVO) => {
  drawer.visible = true;
  drawer.title = `库存流水明细 - ${row.productName || row.productCode || ''}`;
  drawer.productCode = row.productCode || '';
  drawer.productName = row.productName || '';
  drawer.qty = row.qty;
  logList.value = [];
  logLoading.value = true;
  try {
    const res = await listStockLogs(row.productId);
    logList.value = res.data ?? [];
  } catch {
    logList.value = [];
  } finally {
    logLoading.value = false;
  }
};

onMounted(() => {
  const tab = router.currentRoute.value.query.tab;
  if (tab === 'ALERT') {
    activeTab.value = 'ALERT';
    loadAlerts();
  } else {
    getList();
  }
});
</script>

<style scoped lang="scss">
/* 页签只做切换，列表在 tabs 外层渲染；隐藏 t-tabs 自带的（空）面板区，避免多出一段留白 */
:deep(.t-tabs__content) {
  display: none;
}
</style>
