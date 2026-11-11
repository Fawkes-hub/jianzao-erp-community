<template>
  <t-dialog v-model:visible="open" :header="'操作日志详细'" width="700px" attach="body" :close-on-overlay-click="true" @closed="info = null">
    <t-descriptions v-if="info" :column="1" bordered>
      <t-descriptions-item label="操作状态">
        <t-tag v-if="info.status === 0" theme="success">正常</t-tag>
        <t-tag v-else-if="info.status === 1" theme="danger">失败</t-tag>
      </t-descriptions-item>
      <t-descriptions-item label="登录信息">
        {{ info.operName }} / {{ info.deptName }} / {{ info.operIp }} / {{ info.operLocation }}
      </t-descriptions-item>
      <t-descriptions-item label="请求信息"> {{ info.requestMethod }} {{ info.operUrl }} </t-descriptions-item>
      <t-descriptions-item label="操作模块"> {{ info.title }} / {{ typeFormat(info) }} </t-descriptions-item>
      <t-descriptions-item label="操作方法"> {{ info.method }} </t-descriptions-item>
      <t-descriptions-item label="请求参数">
        <div class="max-h-300px overflow-y-auto">
          <VueJsonPretty :data="formatToJsonObject(info.operParam)" />
        </div>
      </t-descriptions-item>
      <t-descriptions-item label="返回参数">
        <div class="max-h-300px overflow-y-auto">
          <VueJsonPretty :data="formatToJsonObject(info.jsonResult)" />
        </div>
      </t-descriptions-item>
      <t-descriptions-item label="消耗时间">
        <span> {{ info.costTime }}ms </span></t-descriptions-item
      >
      <t-descriptions-item label="操作时间"> {{ proxy.parseTime(info.operTime) }}</t-descriptions-item>
      <t-descriptions-item v-if="info.status === 1" label="异常信息">
        <span class="text-danger"> {{ info.errorMsg }}</span>
      </t-descriptions-item>
    </t-descriptions>
  </t-dialog>
</template>

<script setup lang="ts">
import type { OperLogForm } from '@/api/monitor/operlog/types';
import VueJsonPretty from 'vue-json-pretty';
import 'vue-json-pretty/lib/styles.css';

const open = ref(false);
const info = ref<OperLogForm | null>(null);
function openDialog(row: OperLogForm) {
  info.value = row;
  open.value = true;
}

function closeDialog() {
  open.value = false;
}

defineExpose({
  openDialog,
  closeDialog
});

/**
 * json转为对象
 * @param data 原始数据
 */
function formatToJsonObject(data: string) {
  try {
    return JSON.parse(data);
  } catch (error) {
    return data;
  }
}

/**
 * 字典信息
 */
const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const { sys_oper_type } = toRefs<any>(proxy?.useDict('sys_oper_type'));
const typeFormat = (row: OperLogForm) => {
  return proxy?.selectDictLabel(sys_oper_type.value, row.businessType);
};
</script>

<style lang="scss" scoped>
/**
label宽度固定
*/
:deep(.t-descriptions__label) {
  min-width: 100px;
}
/**
文字超过 换行显示
*/
:deep(.t-descriptions__content) {
  max-width: 300px;
}
</style>
