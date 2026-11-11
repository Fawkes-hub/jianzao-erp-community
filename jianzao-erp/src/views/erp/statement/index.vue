<template>
  <div class="p-2">
    <t-card :bordered="false" class="erp-filter-card">
      <t-form :data="query" class="erp-filter-row" label-align="right" :label-width="'auto'">
        <t-form-item label="客户" name="customerId">
          <PartnerSelect v-model="query.customerId" kind="customer" placeholder="请选择客户" class="query-w-md" @change="handleQuery" />
        </t-form-item>
        <t-form-item label="账期" name="month">
          <t-date-picker v-model="query.month" mode="month" value-type="YYYY-MM" placeholder="请选择月份" class="query-w-sm" />
        </t-form-item>
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
        <span class="erp-text-secondary">账期 {{ query.month || '-' }} · 服务端渲染</span>
      </div>

      <t-empty v-if="!statementHtml" description="请选择客户与账期后点击预览" :image-style="{ fontSize: '100px' }" />
      <iframe v-else ref="iframeRef" class="statement-frame" :srcdoc="statementHtml" sandbox="allow-same-origin" title="对账单预览"></iframe>
    </div>
  </div>
</template>

<script setup name="ErpStatement" lang="ts">
import { renderStatement } from '@/api/erp/print';
import type { ErpId } from '@/api/erp/types';
import PartnerSelect from '@/components/erp/PartnerSelect.vue';
import { DownloadIcon, PrintIcon, SearchIcon } from 'tdesign-icons-vue-next';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const loading = ref(false);
const statementHtml = ref('');
const iframeRef = ref<HTMLIFrameElement>();

const query = reactive<{
  customerId?: ErpId;
  month: string;
}>({
  customerId: undefined,
  month: new Date().toISOString().slice(0, 7)
});

const handleQuery = async () => {
  if (!query.customerId) {
    proxy?.$modal.msgWarning('请选择客户');
    return;
  }
  loading.value = true;
  statementHtml.value = '';
  try {
    const res = (await renderStatement({ customerId: query.customerId, month: query.month })) as unknown as string;
    statementHtml.value = res ?? '';
  } finally {
    loading.value = false;
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
