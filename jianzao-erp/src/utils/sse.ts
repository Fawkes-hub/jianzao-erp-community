import { getToken } from '@/utils/auth';
import { NotifyPlugin } from 'tdesign-vue-next';
import { useNoticeStore } from '@/store/modules/notice';

// 初始化
export const initSSE = (url: any) => {
  if (import.meta.env.VITE_APP_SSE === 'false') {
    return;
  }

  url = url + '?Authorization=Bearer ' + getToken() + '&clientid=' + import.meta.env.VITE_APP_CLIENT_ID;
  const { data, error } = useEventSource(url, [], {
    autoReconnect: {
      retries: 5,
      delay: 5000,
      onFailed() {
        console.log('Failed to connect after 5 retries');
      }
    }
  });

  watch(error, () => {
    console.log('SSE connection error:', error.value);
    error.value = null;
  });

  watch(data, () => {
    if (!data.value) return;
    useNoticeStore().addNotice({
      message: data.value,
      read: false,
      time: new Date().toLocaleString()
    });
    // duration 缩短至 2s，并显式开启关闭按钮，避免登录欢迎消息长时间占用右上角
    NotifyPlugin.success({
      title: '消息',
      content: data.value,
      duration: 2000,
      closeBtn: true
    });
    data.value = null;
  });
};
