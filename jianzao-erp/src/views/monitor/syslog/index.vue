<template>
  <div class="p-2">
    <t-card :bordered="false" class="erp-panel">
      <template #header>
        <div class="erp-panel__head">
          <div class="flex items-center justify-between w-full flex-wrap gap-2">
            <div class="flex items-center gap-2 flex-wrap">
              <t-radio-group v-model="activeFile" theme="button" variant="default-filled" :options="fileOptions" @change="handleFileChange" />
              <t-select v-model="tailLines" :options="tailLineOptions" style="width: 130px" @change="loadLog" />
              <t-button theme="primary" variant="outline" @click="loadLog">
                <template #icon><RefreshIcon /></template>刷新</t-button
              >
              <t-button v-hasPermi="['monitor:syslog:list']" theme="warning" variant="outline" @click="handleDownload">
                <template #icon><DownloadIcon /></template>下载完整文件
              </t-button>
              <span v-if="activeFileInfo" class="log-size"> {{ formatSize(activeFileInfo.sizeBytes) }} </span>
            </div>
            <right-toolbar @query-table="loadLog"></right-toolbar>
          </div>
        </div>
      </template>

      <t-loading :loading="loading">
        <t-alert v-if="!activeFileInfo?.exists" theme="info" :close="false" message="日志文件不存在（可能今天还没有产生日志）" />
        <pre v-else class="log-content">{{ content }}</pre>
      </t-loading>
    </t-card>
  </div>
</template>

<script setup name="Syslog" lang="ts">
import { listFiles, tailLog, downloadLog } from '@/api/monitor/syslog';
import { SysLogFileVO } from '@/api/monitor/syslog';
import { blobValidate } from '@/utils/ruoyi';
import { DownloadIcon, RefreshIcon } from 'tdesign-icons-vue-next';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const files = ref<SysLogFileVO[]>([]);
const activeFile = ref('sys-error.log');
const tailLines = ref(500);
const content = ref('');
const loading = ref(false);

const activeFileInfo = computed(() => files.value.find((f) => f.name === activeFile.value));

// t-radio-group / t-select 的选项统一 { label, value } 数组
const fileOptions = computed(() => files.value.map((f) => ({ label: f.label, value: f.name })));
const tailLineOptions = [
  { label: '最后 200 行', value: 200 },
  { label: '最后 500 行', value: 500 },
  { label: '最后 1000 行', value: 1000 },
  { label: '最后 5000 行', value: 5000 }
];

const loadFiles = async () => {
  const res = await listFiles();
  files.value = res.data;
  if (files.value.length > 0 && !files.value.some((f) => f.name === activeFile.value)) {
    activeFile.value = files.value[0].name;
  }
};

const loadLog = async () => {
  loading.value = true;
  try {
    const res = await tailLog(activeFile.value, tailLines.value);
    content.value = res.data.content || '';
  } finally {
    loading.value = false;
  }
};

const handleFileChange = () => {
  loadLog();
};

const handleDownload = async () => {
  const res = await downloadLog(activeFile.value);
  if (!blobValidate(res.data)) {
    const resText = await (res.data as Blob).text();
    const rspObj = JSON.parse(resText);
    proxy?.$modal.msgError(rspObj.msg || '下载失败，请重试');
    return;
  }
  const blob = new Blob([res.data as unknown as BlobPart], { type: 'application/octet-stream' });
  const link = document.createElement('a');
  link.href = URL.createObjectURL(blob);
  link.download = activeFile.value;
  link.click();
  URL.revokeObjectURL(link.href);
};

const formatSize = (bytes: number) => {
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
  return (bytes / 1024 / 1024).toFixed(1) + ' MB';
};

onMounted(async () => {
  await loadFiles();
  await loadLog();
});
</script>

<style scoped>
.log-size {
  font-size: 13px;
  color: var(--td-text-color-secondary);
}
.log-content {
  margin: 0;
  padding: 12px;
  max-height: 65vh;
  overflow: auto;
  background-color: var(--td-bg-color-secondarycontainer);
  border-radius: 4px;
  font-family: 'JetBrains Mono', Consolas, Menlo, monospace;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
