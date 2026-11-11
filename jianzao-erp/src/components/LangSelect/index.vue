<template>
  <!-- TDesign dropdown 插槽方向与 EP 相反：默认插槽是触发器，#dropdown 才是面板 -->
  <t-dropdown trigger="click" @click="handleLanguageChange">
    <div class="lang-select--style">
      <!-- translate 图形笔画密度高，线条收细到 1.3 后与顶栏其余 2px 图标视觉重量一致 -->
      <translate-icon :stroke-width="1.3" />
    </div>
    <template #dropdown>
      <t-dropdown-menu>
        <t-dropdown-item :value="'zh_CN'" :disabled="appStore.language === 'zh_CN'">中文</t-dropdown-item>
        <t-dropdown-item :value="'en_US'" :disabled="appStore.language === 'en_US'">English</t-dropdown-item>
      </t-dropdown-menu>
    </template>
  </t-dropdown>
</template>

<script setup lang="ts">
import { MessagePlugin } from 'tdesign-vue-next';
import { TranslateIcon } from 'tdesign-icons-vue-next';
import { useI18n } from 'vue-i18n';
import { useAppStore } from '@/store/modules/app';

const appStore = useAppStore();
const { locale } = useI18n();

const message: any = {
  zh_CN: '切换语言成功！',
  en_US: 'Switch Language Successful!'
};
// t-dropdown 的 @click 回调给整个 option 对象，语言值在 dropdownItem.value（与 EP @command 语义一致）
const handleLanguageChange = (dropdownItem: any) => {
  const lang = dropdownItem?.value;
  if (!lang) return;
  locale.value = lang;
  appStore.changeLanguage(lang);
  MessagePlugin.success(message[lang] || '切换语言成功！');
};
</script>

<style lang="scss" scoped>
.lang-select--style {
  font-size: 18px;
  display: inline-flex;
  align-items: center;
}
</style>
