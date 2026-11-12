<template>
  <div class="p-2">
    <t-card :bordered="false" class="erp-filter-card">
      <t-form class="erp-filter-row" label-align="right" @submit="getReport">
        <t-form-item label="月份">
          <t-date-picker v-model="month" mode="month" value-type="YYYY-MM" placeholder="请选择月份" style="width: 160px" />
        </t-form-item>
        <div class="erp-filter-actions">
          <t-button theme="primary" @click="getReport">
            <template #icon><SearchIcon /></template>
            查询
          </t-button>
          <t-button theme="default" variant="outline" @click="resetQuery">
            <template #icon><RefreshIcon /></template>
            重置
          </t-button>
        </div>
      </t-form>
    </t-card>

    <div v-loading="loading" class="erp-surface erp-panel">
      <div class="erp-panel__head">
        <span class="font-medium">月度损益概览 - {{ report.month || '-' }}</span>
        <span class="text-xs erp-text-secondary">数据口径：已确认单据；成本按销售出库成本匹配</span>
      </div>

      <template v-if="hasData">
        <t-row :gutter="16" class="!mb-[10px]">
          <t-col :xs="12" :sm="4">
            <div class="compare-item compare-sales">
              <div class="compare-label">销售收入</div>
              <div class="compare-value">{{ formatMoney(report.salesAmount) }}</div>
            </div>
          </t-col>
          <t-col :xs="12" :sm="4">
            <div class="compare-item compare-cost">
              <div class="compare-label">总成本</div>
              <div class="compare-value">{{ formatMoney(report.totalCost) }}</div>
            </div>
          </t-col>
          <t-col :xs="12" :sm="4">
            <div class="compare-item compare-profit">
              <div class="compare-label">毛利</div>
              <div class="compare-value">{{ formatMoney(report.grossProfit) }}</div>
              <div class="compare-extra">毛利率 {{ marginText }}</div>
            </div>
          </t-col>
        </t-row>

        <t-descriptions class="!mb-[10px]" :column="1" bordered size="small">
          <t-descriptions-item label="库存金额（参考）">
            <span class="font-medium">{{ formatMoney(report.stockAmount) }}</span>
            <span class="ml-3 text-xs erp-text-secondary">期初与各类入库形成的库存资产（数量 × 移动加权均价），体现在库存而非本期损益</span>
          </t-descriptions-item>
        </t-descriptions>

        <t-table
          class="erp-table"
          row-key="__key"
          :data="rows"
          :columns="reportColumns"
          :row-class-name="rowClassName"
          :hover="true"
          :resizable="true"
          size="small"
          bordered
          cell-empty-content="-"
          :empty="'暂无成本数据'"
          @column-resize-change="(ctx: any) => saveColumnWidths('finance-cost-report/indicators', ctx.columnsWidth)"
        >
          <template #name="{ row }">
            <span>{{ row.name }}</span>
            <t-tag v-if="row.warning" theme="danger" size="small" class="ml-1">
              {{ row.warning }}
            </t-tag>
          </template>
          <template #amount="{ row }">{{ formatMoney(row.amount) }}</template>
          <template #ratioText="{ row }">{{ row.ratioText }}</template>
        </t-table>
      </template>

      <t-empty v-else-if="!loading" description="暂无成本数据" />
    </div>
  </div>
</template>

<script setup name="ErpFinanceCostReport" lang="ts">
import { costReport } from '@/api/erp/finance';
import type { ErpAmount, FinCostReportVO } from '@/api/erp/types';
import { formatMoney } from '@/utils/erp/calc';
// 列宽记忆：拖拽后按表名存 localStorage，进页面还原（见 hooks/useTableColumnWidth）
import { mergeSavedWidths, saveColumnWidths } from '@/hooks/useTableColumnWidth';
import { RefreshIcon, SearchIcon } from 'tdesign-icons-vue-next';

interface ReportRow {
  /** t-table 行 key（前端生成，不提交后端） */
  __key: string;
  name: string;
  amount: number;
  ratioText: string;
  /** 仅作参考、不计入总成本的指标 */
  reference?: boolean;
  /** 成本缺失提示（DOC/26 §2.4 防线③） */
  warning?: string;
}

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const loading = ref(false);
/** 查询月份，默认当前月 */
const month = ref(currentMonth());

/** 本地当前月份 yyyy-MM */
function currentMonth(): string {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
}

const report = reactive<FinCostReportVO>({
  month: '',
  salesAmount: 0,
  salesCost: 0,
  productionMaterialCost: 0,
  otherOutCost: 0,
  otherExpenseCost: 0,
  totalCost: 0,
  grossProfit: 0,
  grossMargin: undefined,
  stockAmount: 0,
  costMissingCount: 0
});

const num = (value: ErpAmount | undefined | null): number => {
  const n = Number(value ?? 0);
  return Number.isFinite(n) ? n : 0;
};

/** 是否有数据 */
const hasData = computed(() => {
  return (
    num(report.salesAmount) !== 0 ||
    num(report.totalCost) !== 0 ||
    num(report.grossProfit) !== 0 ||
    num(report.salesCost) !== 0 ||
    num(report.productionMaterialCost) !== 0 ||
    num(report.otherOutCost) !== 0 ||
    num(report.otherExpenseCost) !== 0 ||
    num(report.stockAmount) !== 0
  );
});

/** 毛利率（后端返回百分数，如 "71.75"；销售为 0 时返回 null） */
const marginText = computed(() => {
  const value = report.grossMargin;
  if (value === undefined || value === null || value === '') return '-';
  const rate = Number(value);
  if (!Number.isFinite(rate)) return '-';
  return `${rate.toFixed(2)}%`;
});

/** 占比文本 */
const ratioText = (amount: number): string => {
  const sales = num(report.salesAmount);
  if (sales <= 0) return '-';
  return `${((amount / sales) * 100).toFixed(2)}%`;
};

/** 指标表列配置：t-table 用 columns 数组，cell 名与 #插槽同名（DOC/35 §5） */
const reportColumns: any[] = [
  { colKey: 'name', title: '指标', align: 'center', minWidth: 220, cell: 'name' },
  { colKey: 'amount', title: '金额', align: 'right', width: 180, cell: 'amount' },
  { colKey: 'ratioText', title: '占销售收入比', align: 'right', width: 160, cell: 'ratioText' }
];
mergeSavedWidths('finance-cost-report/indicators', reportColumns);

const rows = computed<ReportRow[]>(() => [
  { __key: 'sales', name: '销售收入', amount: num(report.salesAmount), ratioText: ratioText(num(report.salesAmount)) },
  {
    __key: 'sales-cost',
    name: '销售成本',
    amount: num(report.salesCost),
    ratioText: ratioText(num(report.salesCost)),
    warning: num(report.costMissingCount) > 0 ? `成本缺失 ${num(report.costMissingCount)} 行` : undefined
  },
  { __key: 'other-out', name: '其他出库成本', amount: num(report.otherOutCost), ratioText: ratioText(num(report.otherOutCost)) },
  {
    __key: 'other-expense',
    name: '计入成本的其他支出',
    amount: num(report.otherExpenseCost),
    ratioText: ratioText(num(report.otherExpenseCost))
  },
  { __key: 'total-cost', name: '总成本', amount: num(report.totalCost), ratioText: ratioText(num(report.totalCost)) },
  { __key: 'gross-profit', name: '毛利', amount: num(report.grossProfit), ratioText: marginText.value },
  {
    __key: 'production-material',
    name: '生产材料成本（参考，不计入总成本）',
    amount: num(report.productionMaterialCost),
    ratioText: ratioText(num(report.productionMaterialCost)),
    reference: true
  }
]);

/** 参考行弱化展示 */
const rowClassName = ({ row }: { row: ReportRow }) => (row.reference ? 'is-reference' : '');

/** 查询综合成本报表 */
const getReport = async () => {
  loading.value = true;
  try {
    const res = await costReport(month.value);
    const data = res.data;
    Object.assign(report, {
      month: data?.month ?? month.value,
      salesAmount: num(data?.salesAmount),
      salesCost: num(data?.salesCost),
      productionMaterialCost: num(data?.productionMaterialCost),
      otherOutCost: num(data?.otherOutCost),
      otherExpenseCost: num(data?.otherExpenseCost),
      totalCost: num(data?.totalCost),
      grossProfit: num(data?.grossProfit),
      grossMargin: data?.grossMargin ?? null,
      stockAmount: num(data?.stockAmount),
      costMissingCount: num(data?.costMissingCount)
    });
  } catch {
    Object.assign(report, {
      month: month.value,
      salesAmount: 0,
      salesCost: 0,
      productionMaterialCost: 0,
      otherOutCost: 0,
      otherExpenseCost: 0,
      totalCost: 0,
      grossProfit: 0,
      grossMargin: null,
      stockAmount: 0,
      costMissingCount: 0
    });
  } finally {
    loading.value = false;
  }
};

/** 重置查询 */
const resetQuery = () => {
  month.value = currentMonth();
  getReport();
};

onMounted(() => {
  getReport();
});
</script>

<style scoped lang="scss">
/* 三块对比卡：从 EP 时代的彩色渐变块收敛成 TDesign 语义色（文字取语义色，底色走容器色） */
.compare-item {
  border: 1px solid var(--td-border-level-1-color);
  border-radius: var(--app-radius-lg, 8px);
  padding: 14px 16px;
  background: var(--td-bg-color-container);
}

.compare-label {
  font-size: 13px;
  color: var(--td-text-color-secondary);
}

.compare-value {
  margin-top: 6px;
  font-size: 24px;
  font-weight: 600;
  line-height: 1.2;
  color: var(--td-text-color-primary);
}

.compare-extra {
  margin-top: 4px;
  font-size: 12px;
  color: var(--td-text-color-secondary);
}

.compare-sales .compare-value {
  color: var(--td-brand-color);
}

.compare-cost .compare-value {
  color: var(--td-warning-color);
}

.compare-profit .compare-value {
  color: var(--td-success-color);
}

:deep(.is-reference) {
  color: var(--td-text-color-secondary);
  font-style: italic;
}
</style>
