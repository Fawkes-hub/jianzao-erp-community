<template>
  <t-form :ref="setFormRef" :data="infoForm" :rules="rules" label-width="150px">
    <t-row :gutter="16">
      <t-col :span="6">
        <t-form-item label="表名称" name="tableName">
          <t-input v-model="infoForm.tableName" placeholder="请输入仓库名称" />
        </t-form-item>
      </t-col>
      <t-col :span="6">
        <t-form-item label="表描述" name="tableComment">
          <t-input v-model="infoForm.tableComment" placeholder="请输入" />
        </t-form-item>
      </t-col>
    </t-row>
    <t-row :gutter="16">
      <t-col :span="6">
        <t-form-item label="实体类名称" name="className">
          <t-input v-model="infoForm.className" placeholder="请输入" />
        </t-form-item>
      </t-col>
      <t-col :span="6">
        <t-form-item label="作者" name="functionAuthor">
          <t-input v-model="infoForm.functionAuthor" placeholder="请输入" />
        </t-form-item>
      </t-col>
    </t-row>
    <t-row :gutter="16">
      <t-col :span="12">
        <t-form-item label="备注" name="remark">
          <t-textarea v-model="infoForm.remark" :autosize="{ minRows: 3 }" placeholder="请输入" />
        </t-form-item>
      </t-col>
    </t-row>
  </t-form>
</template>

<script setup lang="ts">
import { propTypes } from '@/utils/propTypes';

const prop = defineProps({
  info: propTypes.any.def({})
});

const infoForm = computed(() => prop.info);

// TDesign 的 rules 形状与 EP 不兼容，按 Record<string, any> 声明
const rules: Record<string, any> = {
  tableName: [{ required: true, message: '请输入表名称', trigger: 'blur' }],
  tableComment: [{ required: true, message: '请输入表描述', trigger: 'blur' }],
  className: [{ required: true, message: '请输入实体类名称', trigger: 'blur' }],
  functionAuthor: [{ required: true, message: '请输入作者', trigger: 'blur' }]
};

const formRef = ref<any>();

// 父组件 editTable.vue 通过 $refs.basicInfoForm.validate(callback) 做提交校验（EP 回调式契约，
// 该父文件不在本迁移批次内）。TDesign 的 validate() 返回 Promise 且忽略回调入参，且失败时
// resolve 错误对象而非 false。这里用函数 ref 同时完成两件事：把真实 t-form 实例存进 formRef，
// 并把 refs.basicInfoForm 指向兼容对象（转 boolean 后回调），保持父组件提交判定不变：
// r===true||r===undefined 视为通过。卸载时清掉兼容对象，避免悬空引用。
const setFormRef = (formInstance: any, refs: Record<string, any>) => {
  formRef.value = formInstance;
  if (!formInstance) {
    delete refs.basicInfoForm;
    return;
  }
  refs.basicInfoForm = {
    validate: (callback: (valid: boolean) => void) => {
      Promise.resolve(formRef.value?.validate())
        .then((r: any) => callback(r === true || r === undefined))
        .catch(() => callback(false));
    }
  };
};
</script>
