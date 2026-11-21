<template>
  <t-dialog v-model:visible="visible" :header="title" width="820px" :footer="false" attach="body" :destroy-on-close="true">
    <div class="flex items-center gap-2 mb-2">
      <t-button theme="primary" :disabled="!html" @click="print">打印</t-button>
    </div>
    <div v-loading="loading" style="min-height: 200px">
      <iframe
        v-if="html"
        ref="frame"
        :srcdoc="html"
        sandbox="allow-same-origin allow-modals"
        title="打印预览"
        style="width: 100%; height: 62vh; border: 1px solid var(--td-border-level-1-color)"
      ></iframe>
    </div>
  </t-dialog>
</template>

<script setup lang="ts">
import { renderPayment, renderReceipt } from '@/api/erp/print';
import type { ErpId } from '@/api/erp/types';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const visible = ref(false);
const loading = ref(false);
const html = ref('');
const title = ref('');
const frame = ref<HTMLIFrameElement>();
let requestId = 0;

/** 打开收付款单打印预览，并忽略过期请求。 */
const open = async (kind: 'receipt' | 'payment', id: ErpId, no?: string) => {
  const current = ++requestId;
  title.value = `打印${kind === 'receipt' ? '收款单' : '付款单'} - ${no ?? ''}`;
  html.value = '';
  visible.value = true;
  loading.value = true;
  try {
    const result = (await (kind === 'receipt' ? renderReceipt(id) : renderPayment(id))) as unknown as string;
    if (current !== requestId) return;
    if (typeof result !== 'string' || !result.trim() || result.trim().startsWith('{')) {
      proxy?.$modal.msgWarning('打印预览生成失败，请检查模板');
      return;
    }
    html.value = result;
  } catch {
    if (current === requestId) proxy?.$modal.msgWarning('打印预览加载失败，请稍后重试');
  } finally {
    if (current === requestId) loading.value = false;
  }
};

/** 在用户点击后调用预览窗口的打印。 */
const print = () => {
  const win = frame.value?.contentWindow;
  if (!win) {
    proxy?.$modal.msgWarning('预览尚未加载完成');
    return;
  }
  win.focus();
  win.print();
};

defineExpose({ open });
</script>
