<template>
  <router-view />
</template>

<script setup lang="ts">
import { useSettingsStore } from '@/store/modules/settings';
import { handleThemeStyle } from '@/utils/theme';

onMounted(() => {
  nextTick(() => {
    // 初始化主题样式与 TDesign 暗色模式
    const isDark = localStorage.getItem('useDarkKey') === 'dark';
    document.documentElement.setAttribute('theme-mode', isDark ? 'dark' : 'light');
    if (isDark) {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
    handleThemeStyle(useSettingsStore().theme);
  });
});
</script>
