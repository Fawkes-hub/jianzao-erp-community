<template>
  <div class="p-2">
    <t-card :bordered="false" class="erp-filter-card">
      <t-form :data="query" class="erp-filter-row" label-align="right" :label-width="'auto'">
        <t-form-item label="客户" name="customerId">
          <PartnerSelect v-model="query.customerId" kind="customer" placeholder="请选择客户" class="query-w-md" @change="handleQuery" />
        </t-form-item>
        <t-form-item label="对账方式" name="mode">
          <t-radio-group v-model="query.mode" variant="default-filled" @change="handleModeChange">
            <t-radio-button value="MONTH">按月</t-radio-button>
            <t-radio-button value="RANGE">自定义区间</t-radio-button>
          </t-radio-group>
        </t-form-item>
        <t-form-item v-if="query.mode === 'MONTH'" label="账期" name="month">
          <t-date-picker v-model="query.month" mode="month" value-type="YYYY-MM" placeholder="请选择月份" class="query-w-sm" />
        </t-form-item>
        <template v-else>
          <t-form-item label="开始日期" name="startDate">
            <t-date-picker v-model="query.startDate" mode="date" value-type="YYYY-MM-DD" placeholder="请选择开始日期" class="query-w-sm" />
          </t-form-item>
          <t-form-item label="结束日期" name="endDate">
            <t-date-picker v-model="query.endDate" mode="date" value-type="YYYY-MM-DD" placeholder="请选择结束日期" class="query-w-sm" />
          </t-form-item>
        </template>
        <div class="erp-filter-actions">
          <t-button theme="primary" :loading="loading" @click="handleQuery">
            <template #icon><SearchIcon /></template>
            预览
          </t-button>
          <t-button theme="default" variant="outline" :disabled="!statementHtml" @click="handlePrint">
            <template #icon><PrintIcon /></template>
            打印
          </t-button>
          <t-button theme="default" variant="outline" :disabled="!statementHtml" @click="handleOpen">
            <template #icon><DownloadIcon /></template>
            新窗口打开
          </t-button>
        </div>
      </t-form>
    </t-card>

    <div v-loading="loading" class="erp-surface erp-panel">
      <div class="erp-panel__head">
        <span class="erp-panel-title">对账单预览</span>
        <span class="erp-text-secondary">{{ periodText }} · 服务端渲染</span>
      </div>

      <t-empty
        v-if="!statementHtml"
        :description="`请选择客户与${query.mode === 'RANGE' ? '起止日期' : '账期'}后点击预览`"
        :image-style="{ fontSize: '100px' }"
      />
      <iframe
        v-else
        ref="iframeRef"
        class="statement-frame"
        style="display: block; width: 100%; height: 70vh; min-height: 600px; box-sizing: border-box"
        :srcdoc="statementHtml"
        sandbox="allow-same-origin allow-modals"
        title="对账单预览"
      ></iframe>
    </div>
  </div>
</template>

<script setup name="ErpStatement" lang="ts">
import { renderStatement } from '@/api/erp/print';
import type { ErpId } from '@/api/erp/types';
import PartnerSelect from '@/components/erp/PartnerSelect.vue';
import { DownloadIcon, PrintIcon, SearchIcon } from 'tdesign-icons-vue-next';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

/** 本地日期 yyyy-MM-dd（不用 toISOString，避免 UTC 偏移导致跨天/跨月） */
const formatDate = (date: Date) => {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
};

/** 解析后端 HTTP 200 + JSON 错误体，避免把 JSON 写进预览 iframe */
const parseBackendMsg = (text: string) => {
  try {
    return (JSON.parse(text) as { msg?: string })?.msg ?? '';
  } catch {
    return '';
  }
};

const today = new Date();
const todayText = formatDate(today);

const loading = ref(false);
const statementHtml = ref('');
const iframeRef = ref<HTMLIFrameElement>();
/** 请求序号：切换方式/重新查询后丢弃过期响应 */
let requestSeq = 0;

const query = reactive<{
  customerId?: ErpId;
  mode: 'MONTH' | 'RANGE';
  month: string;
  startDate: string;
  endDate: string;
}>({
  customerId: undefined,
  mode: 'MONTH',
  month: todayText.slice(0, 7),
  startDate: `${todayText.slice(0, 7)}-01`,
  endDate: todayText
});

/** 预览标题里的对账期间文案 */
const periodText = computed(() => {
  if (query.mode === 'RANGE') return `对账期间 ${query.startDate || '-'} 至 ${query.endDate || '-'}`;
  return `账期 ${query.month || '-'}`;
});

/** 切换对账方式后清空旧预览，避免月度/区间结果混看 */
const handleModeChange = () => {
  requestSeq += 1;
  loading.value = false;
  statementHtml.value = '';
};

/** 区间天数（含首尾），与后端 MAX_RANGE_DAYS=366 对齐 */
const rangeDays = () => Math.round((Date.parse(query.endDate) - Date.parse(query.startDate)) / 86400000) + 1;

const handleQuery = async () => {
  if (!query.customerId) {
    proxy?.$modal.msgWarning('请选择客户');
    return;
  }
  if (query.mode === 'RANGE') {
    if (!query.startDate || !query.endDate) {
      proxy?.$modal.msgWarning('请选择开始日期和结束日期');
      return;
    }
    if (query.startDate > query.endDate) {
      proxy?.$modal.msgWarning('开始日期不能晚于结束日期');
      return;
    }
    if (rangeDays() > 366) {
      proxy?.$modal.msgWarning('对账区间不能超过 366 天');
      return;
    }
  }
  loading.value = true;
  statementHtml.value = '';
  const seq = ++requestSeq;
  try {
    // 区间模式只传起止日；月度模式保持原 month 参数
    const params =
      query.mode === 'RANGE'
        ? { customerId: query.customerId, startDate: query.startDate, endDate: query.endDate }
        : { customerId: query.customerId, month: query.month };
    const res = (await renderStatement(params)) as unknown as string;
    // 期间已切换或已发起新查询：旧响应直接丢弃，避免回填到新模式
    if (seq !== requestSeq) return;
    const html = String(res ?? '');
    if (!html.trimStart().startsWith('<')) {
      proxy?.$modal.msgError(parseBackendMsg(html) || '对账单生成失败');
      statementHtml.value = '';
      return;
    }
    statementHtml.value = html;
  } catch {
    if (seq === requestSeq) proxy?.$modal.msgError('对账单生成失败，请稍后重试');
  } finally {
    if (seq === requestSeq) loading.value = false;
  }
};

/**
 * 打印走 iframe 内容窗口，浏览器拦截弹窗时提示用户改用「新窗口打开」
 */
const handlePrint = () => {
  const win = iframeRef.value?.contentWindow;
  if (!win) {
    proxy?.$modal.msgWarning('请先预览对账单');
    return;
  }
  try {
    win.focus();
    win.print();
  } catch {
    proxy?.$modal.msgWarning('打印被浏览器拦截，请使用「新窗口打开」后打印');
  }
};

const handleOpen = () => {
  if (!statementHtml.value) return;
  const win = window.open('', '_blank');
  if (!win) {
    proxy?.$modal.msgWarning('新窗口被浏览器拦截，请允许弹出窗口');
    return;
  }
  win.document.write(statementHtml.value);
  win.document.close();
};
</script>

<style scoped lang="scss">
.statement-frame {
  width: 100%;
  height: 70vh;
  border: 1px solid var(--td-border-level-1-color);
  border-radius: 4px;
  background: #fff;
}
</style>
