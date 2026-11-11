<template>
  <div class="component-upload-image">
    <t-upload
      ref="imageUploadRef"
      v-model="uploadFiles"
      theme="image"
      multiple
      :action="uploadImgUrl"
      :headers="headers"
      :accept="fileAccept"
      :max="limit"
      :auto-upload="true"
      :show-image-file-name="false"
      :before-upload="handleBeforeUpload"
      @validate="handleValidate"
      @success="handleUploadSuccess"
      @fail="handleUploadError"
      @remove="handleDelete"
    >
      <!-- t-upload 的自定义 trigger 插槽不会自动带点击，须显式调用 triggerUpload 才能打开文件选择 -->
      <template #trigger>
        <div class="image-upload__add" @click="imageUploadRef?.triggerUpload()"><AddIcon size="24px" /></div>
      </template>
    </t-upload>
    <!-- 上传提示：沿用 EP 时代的文案与条件，不改业务逻辑 -->
    <div v-if="showTip" class="image-upload__tip">
      请上传
      <template v-if="fileSize">
        大小不超过 <b style="color: #f56c6c">{{ fileSize }}MB</b>
      </template>
      <template v-if="fileType">
        格式为 <b style="color: #f56c6c">{{ fileType.join('/') }}</b>
      </template>
      的文件
    </div>
  </div>
</template>

<script setup lang="ts">
import { AddIcon } from 'tdesign-icons-vue-next';
import type { UploadFile, SuccessContext, UploadFailContext, UploadRemoveContext } from 'tdesign-vue-next';
import { listByIds, delOss } from '@/api/system/oss';
import { OssVO } from '@/api/system/oss/types';
import { propTypes } from '@/utils/propTypes';
import { globalHeaders } from '@/utils/request';
import { compressImage, DEFAULT_IMAGE_MAX_SIZE_KB, resolveOutputMimeType, syncUploadFile } from '@/utils/image';

const props = defineProps({
  modelValue: {
    type: [String, Object, Array],
    default: () => []
  },
  // 图片数量限制
  limit: propTypes.number.def(5),
  // 大小限制(MB)
  fileSize: propTypes.number.def(5),
  // 文件类型, 例如['png', 'jpg', 'jpeg']
  fileType: propTypes.array.def(['png', 'jpg', 'jpeg']),
  // 是否显示提示
  isShowTip: {
    type: Boolean,
    default: true
  },
  // 是否支持压缩，默认开启；需要保留原始字节时显式传 false
  compressSupport: {
    type: Boolean,
    default: true
  },
  // 压缩目标大小，单位KB。默认超过300KB的文件压缩至300KB以内
  compressTargetSize: propTypes.number.def(DEFAULT_IMAGE_MAX_SIZE_KB)
});

const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const emit = defineEmits(['update:modelValue']);

const baseUrl = import.meta.env.VITE_APP_BASE_API;
const uploadImgUrl = ref(baseUrl + '/resource/oss/upload'); // 上传的图片服务器地址
const headers = ref(globalHeaders());

const fileList = ref<any[]>([]);
const showTip = computed(() => props.isShowTip && (props.fileType || props.fileSize));

// t-upload 是受控 files；image-card 按 max 自动隐藏加号，替代 EP 的 .hide 类
const imageUploadRef = ref<{ triggerUpload: () => void }>();
const uploadFiles = computed({
  get: () => fileList.value,
  set: (val: UploadFile[]) => {
    fileList.value = val;
  }
});

// 监听 fileType 变化，更新 fileAccept
const fileAccept = computed(() => props.fileType.map((type) => `.${type}`).join(','));

watch(
  () => props.modelValue,
  async (val: string) => {
    if (val) {
      // 首先将值转为数组
      let list: OssVO[] = [];
      if (Array.isArray(val)) {
        list = val as OssVO[];
      } else {
        const res = await listByIds(val);
        list = res.data;
      }
      // 然后将数组转为对象数组
      fileList.value = list.map((item) => {
        // 字符串回显处理 如果此处存的是url可直接回显 如果存的是id需要调用接口查出来
        let itemData;
        if (typeof item === 'string') {
          itemData = { name: item, url: item };
        } else {
          // 此处name使用ossId 防止删除出现重名
          itemData = { name: item.ossId, url: item.url, ossId: item.ossId };
        }
        return itemData;
      });
    } else {
      fileList.value = [];
      return [];
    }
  },
  { deep: true, immediate: true }
);

/**
 * 上传/压缩期间的全局 loading 引用计数：
 * 多选时每个文件独立压缩/上传，只有最后一个文件结束才关闭遮罩，避免先完成的文件把仍在处理的遮罩关掉
 */
const pendingCount = ref(0);
const startPending = () => {
  if (pendingCount.value === 0) {
    proxy?.$modal.loading('正在上传图片，请稍候...');
  }
  pendingCount.value += 1;
};
const finishPending = (count = 1) => {
  pendingCount.value = Math.max(0, pendingCount.value - count);
  if (pendingCount.value === 0) {
    proxy?.$modal.closeLoading();
  }
};

/** 上传前校验格式/大小；通过后压缩并原地替换 raw（TDesign 的 XHR 取 file.raw） */
const handleBeforeUpload = async (file: any) => {
  let isImg = false;
  if (props.fileType.length) {
    let fileExtension = '';
    if (file.name.lastIndexOf('.') > -1) {
      fileExtension = file.name.slice(file.name.lastIndexOf('.') + 1);
    }
    isImg = props.fileType.some((type: any) => {
      if (file.type.indexOf(type) > -1) return true;
      if (fileExtension && fileExtension.indexOf(type) > -1) return true;
      return false;
    });
  } else {
    isImg = file.type.indexOf('image') > -1;
  }
  if (!isImg) {
    proxy?.$modal.msgError(`文件格式不正确, 请上传${props.fileType.join('/')}图片格式文件!`);
    return false;
  }
  if (file.name.includes(',')) {
    proxy?.$modal.msgError('文件名不正确，不能包含英文逗号!');
    return false;
  }
  if (props.fileSize) {
    const isLt = file.size / 1024 / 1024 < props.fileSize;
    if (!isLt) {
      proxy?.$modal.msgError(`上传图片大小不能超过 ${props.fileSize} MB!`);
      return false;
    }
  }

  // 校验通过后才计入 pending，避免被拒绝的文件把遮罩计数带偏
  startPending();
  if (props.compressSupport && file.size / 1024 > props.compressTargetSize) {
    try {
      // 组件已按目标体积做过一次筛选，这里不再按默认阈值跳过，避免 compressTargetSize < 300 时被误跳过
      const compressed = await compressImage(file.raw ?? file, {
        maxSizeKB: props.compressTargetSize,
        skipBelowKB: 0,
        mimeType: resolveOutputMimeType(props.fileType)
      });
      syncUploadFile(file, compressed);
    } catch (error) {
      // compressImage 内部已兜底，这里只为避免上传流程被意外异常打断
      console.warn('[image-upload] 图片压缩失败，使用原图上传', error);
    }
  }
  return true;
};

/** 数量/校验事件：对应 EP 的 on-exceed 与 before-upload 拒绝 */
const handleValidate = (ctx: { type: string; files: UploadFile[] }) => {
  if (ctx.type === 'FILES_OVER_LENGTH_LIMIT') {
    proxy?.$modal.msgError(`上传文件数量不能超过 ${props.limit} 个!`);
  }
};

// 上传成功回调：results[i] 为 { response, files: [UploadFile] }，单文件上传时 results 为空、走 ctx.response/ctx.file
const handleUploadSuccess = (ctx: SuccessContext) => {
  const items: any[] = ctx.results?.length
    ? ctx.results.map((r: any) => ({ response: r.response, file: r.files?.[0] }))
    : [{ response: ctx.response, file: ctx.file }];
  items.forEach((item: any) => {
    const res = item.response;
    const file = item.file;
    if (res?.code === 200) {
      // 把后端 ossId/url 写回受控文件对象，供回显/删除/提交串使用
      if (file) {
        file.name = res.data.fileName;
        file.url = res.data.url;
        file.ossId = res.data.ossId;
      }
    } else {
      proxy?.$modal.msgError(res?.msg || '上传图片失败');
      if (file) {
        fileList.value = fileList.value.filter((f) => f !== file);
      }
    }
  });
  // 业务失败也算本次上传已结束，计数按实际结算的文件数扣减
  finishPending(items.length);
  emit('update:modelValue', listToString(fileList.value));
};

// 上传失败（HTTP 层）：整批失败时 failedFiles 可能包含多个文件，按实际数量结算
const handleUploadError = (ctx: UploadFailContext) => {
  proxy?.$modal.msgError(ctx?.response?.msg || '上传图片失败');
  finishPending(ctx?.failedFiles?.length || 1);
};

// 删除图片：TDesign 受控模型已内部移除该项，此处同步删 OSS 并回写提交串
const handleDelete = (ctx: UploadRemoveContext) => {
  const ossId = (ctx.file as any)?.ossId;
  if (ossId) {
    delOss(ossId);
  }
  emit('update:modelValue', listToString(fileList.value));
};

// 对象转成指定字符串分隔
const listToString = (list: any[], separator?: string) => {
  let strs = '';
  separator = separator || ',';
  for (const i in list) {
    if (undefined !== list[i].ossId && list[i].url.indexOf('blob:') !== 0) {
      strs += list[i].ossId + separator;
    }
  }
  return strs != '' ? strs.substring(0, strs.length - 1) : '';
};
</script>

<style lang="scss" scoped>
// 受控模式下 t-upload 内部已按 max 隐藏加号；自定义触发块保持 EP 图片卡加号的观感
.image-upload__add {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  cursor: pointer;
}

.image-upload__tip {
  color: var(--td-text-color-secondary);
  font-size: 12px;
}
</style>
