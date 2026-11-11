<template>
  <div class="user-info-head" title="点击更换头像" @click="editCropper()">
    <img :src="options.img" class="avatar-img" />
    <div class="avatar-hover-mask">
      <CameraIcon class="text-[18px]" />
      <span>更换头像</span>
    </div>
    <t-dialog v-model:visible="open" width="800px" attach="body" :footer="false" @opened="modalOpened" @close="closeDialog">
      <template #header>{{ title }}</template>
      <t-row>
        <t-col :xs="12" :md="6" :style="{ height: '350px' }">
          <vue-cropper
            v-if="visible"
            ref="cropper"
            :img="options.img"
            :info="true"
            :auto-crop="options.autoCrop"
            :auto-crop-width="options.autoCropWidth"
            :auto-crop-height="options.autoCropHeight"
            :fixed-box="options.fixedBox"
            :output-type="options.outputType"
            @real-time="realTime"
          />
        </t-col>
        <t-col :xs="12" :md="6" :style="{ height: '350px' }">
          <div class="avatar-upload-preview">
            <img :src="options.previews.url" :style="options.previews.img" />
          </div>
        </t-col>
      </t-row>
      <br />
      <t-row>
        <t-col :span="3">
          <t-upload theme="custom" :auto-upload="false" :request-method="requestUpload" :before-upload="beforeUpload">
            <t-button variant="outline">
              选择
              <template #icon><UploadIcon /></template>
            </t-button>
          </t-upload>
        </t-col>
        <t-col :span="1">
          <t-button variant="outline" shape="square" @click="changeScale(1)">
            <template #icon><AddIcon /></template>
          </t-button>
        </t-col>
        <t-col :span="1">
          <t-button variant="outline" shape="square" @click="changeScale(-1)">
            <template #icon><MinusIcon /></template>
          </t-button>
        </t-col>
        <t-col :span="1">
          <t-button variant="outline" shape="square" @click="rotateLeft()">
            <template #icon><AnticlockwiseIcon /></template>
          </t-button>
        </t-col>
        <t-col :span="1">
          <t-button variant="outline" shape="square" @click="rotateRight()">
            <template #icon><RotationIcon /></template>
          </t-button>
        </t-col>
        <t-col :span="3" :offset="2">
          <t-button theme="primary" :loading="submitting" :disabled="submitting" @click="uploadImg()">提 交</t-button>
        </t-col>
      </t-row>
      <div class="avatar-upload__tip">支持 JPG/PNG 等常见图片格式，原图不超过 {{ MAX_SOURCE_SIZE_MB }}MB；提交后按 200×200 压缩为 JPEG</div>
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import 'vue-cropper/dist/index.css';
import { VueCropper } from 'vue-cropper';
import { uploadAvatar } from '@/api/system/user';
import { useUserStore } from '@/store/modules/user';
import { compressImage } from '@/utils/image';
// TDesign 图标必须显式 import，不会自动解析（DOC/35 §3）
import { AddIcon, AnticlockwiseIcon, CameraIcon, MinusIcon, RotationIcon, UploadIcon } from 'tdesign-icons-vue-next';

interface Options {
  img: string | any; // 裁剪图片的地址
  autoCrop: boolean; // 是否默认生成截图框
  autoCropWidth: number; // 默认生成截图框宽度
  autoCropHeight: number; // 默认生成截图框高度
  fixedBox: boolean; // 固定截图框大小 不允许改变
  fileName: string;
  previews: any; // 预览数据
  outputType: string;
  visible: boolean;
}

const userStore = useUserStore();
const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const open = ref(false);
const visible = ref(false);
const title = ref('修改头像');

const cropper = ref<any>({});
/** 原图大小上限（MB）：原图只在浏览器内解码，限制它是为了避免 FileReader 读入超大文件 */
const MAX_SOURCE_SIZE_MB = 10;
const submitting = ref(false);
//图片裁剪数据
const options = reactive<Options>({
  img: userStore.avatar,
  autoCrop: true,
  autoCropWidth: 200,
  autoCropHeight: 200,
  fixedBox: true,
  outputType: 'png',
  fileName: '',
  previews: {},
  visible: false
});

/** 编辑头像 */
const editCropper = () => {
  open.value = true;
};
/** 打开弹出层结束时的回调 */
const modalOpened = () => {
  visible.value = true;
};
/** 覆盖默认上传行为：真正上传在「提交」里走 uploadAvatar，这里只做本地裁剪预览 */
const requestUpload = (): any => Promise.resolve({ status: 'success', response: {} });
/** 向左旋转 */
const rotateLeft = () => {
  cropper.value.rotateLeft();
};
/** 向右旋转 */
const rotateRight = () => {
  cropper.value.rotateRight();
};
/** 图片缩放 */
const changeScale = (num: number) => {
  num = num || 1;
  cropper.value.changeScale(num);
};
/** 上传预处理：只把图片读进裁剪器，返回 false 阻止 t-upload 发起网络请求 */
const beforeUpload = (file: any): boolean => {
  const raw = file?.raw ?? file;
  if (String(raw?.type ?? '').indexOf('image/') === -1) {
    proxy?.$modal.msgError('文件格式错误，请上传图片类型,如：JPG，PNG后缀的文件。');
    return false;
  }
  if (raw?.size && raw.size > MAX_SOURCE_SIZE_MB * 1024 * 1024) {
    proxy?.$modal.msgError(`图片大小不能超过 ${MAX_SOURCE_SIZE_MB} MB`);
    return false;
  }
  const reader = new FileReader();
  reader.readAsDataURL(raw);
  reader.onload = () => {
    options.img = reader.result;
    options.fileName = raw.name || 'avatar.png';
  };
  return false;
};
/** 读取裁剪结果：vue-cropper 只提供回调式 API，这里包一层 Promise 便于串行处理 */
const getCropBlob = () =>
  new Promise<Blob | null>((resolve) => {
    cropper.value.getCropBlob((blob: Blob) => resolve(blob));
  });

/** 上传图片：裁剪结果统一转成 JPEG 再提交，200×200 尺寸保持不变 */
const uploadImg = async () => {
  if (submitting.value) return;
  submitting.value = true;
  try {
    const blob = await getCropBlob();
    if (!blob) {
      proxy?.$modal.msgError('图片裁剪失败，请重试');
      return;
    }
    // 裁剪器保留 PNG 以保留透明通道；转 JPEG 时由压缩工具补白底，避免透明区域变黑
    const source = new File([blob], options.fileName || 'avatar.png', { type: blob.type || 'image/png' });
    const uploadFile = await compressImage(source, {
      maxEdge: Math.max(options.autoCropWidth, options.autoCropHeight),
      maxSizeKB: 100,
      skipBelowKB: 0,
      quality: 0.9,
      mimeType: 'image/jpeg',
      preserveAlpha: false
    });
    const formData = new FormData();
    formData.append('avatarfile', uploadFile, uploadFile.name);
    const res = await uploadAvatar(formData);
    open.value = false;
    options.img = res.data.imgUrl;
    userStore.setAvatar(options.img);
    proxy?.$modal.msgSuccess('修改成功');
    visible.value = false;
  } catch (error) {
    // 请求异常已由 request 拦截器提示；这里只记录，避免重复弹错
    console.warn('[avatar] 头像上传失败', error);
  } finally {
    submitting.value = false;
  }
};
/** 实时预览 */
const realTime = (data: any) => {
  options.previews = data;
};
/** 关闭窗口 */
const closeDialog = () => {
  options.img = userStore.avatar;
  options.visible = false;
};
</script>

<style lang="scss" scoped>
.user-info-head {
  position: relative;
  display: inline-block;
  width: 96px;
  height: 96px;
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
  box-shadow: var(--app-shadow-sm);
  border: 2px solid var(--td-border-level-2-color);
  background-color: var(--td-bg-color-container);
  transition: all 0.2s ease;

  &:hover {
    border-color: var(--td-brand-color);
    box-shadow: var(--app-shadow-md);

    .avatar-hover-mask {
      opacity: 1;
    }
  }

  .avatar-img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
  }

  .avatar-hover-mask {
    position: absolute;
    inset: 0;
    background: rgba(0, 0, 0, 0.55);
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    color: #fff;
    opacity: 0;
    transition: opacity 0.2s ease;
    font-size: 11px;
    gap: 2px;
    backdrop-filter: blur(1px);
  }
}

.avatar-upload__tip {
  margin-top: 8px;
  color: var(--td-text-color-secondary);
  font-size: 12px;
  line-height: 20px;
}
</style>
