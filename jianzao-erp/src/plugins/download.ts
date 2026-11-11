import axios from 'axios';
import FileSaver from 'file-saver';
import errorCode from '@/utils/errorCode';
import { blobValidate } from '@/utils/ruoyi';
import { LoadingPlugin, MessagePlugin } from 'tdesign-vue-next';
import type { LoadingInstance } from 'tdesign-vue-next';
import { globalHeaders } from '@/utils/request';

const baseURL = import.meta.env.VITE_APP_BASE_API;
let downloadLoadingInstance: LoadingInstance;
export default {
  async oss(ossId: string | number) {
    const url = baseURL + '/resource/oss/download/' + ossId;
    downloadLoadingInstance = LoadingPlugin({ fullscreen: true, text: '正在下载数据，请稍候', preventScrollThrough: true });
    try {
      const res = await axios({
        method: 'get',
        url: url,
        responseType: 'blob',
        headers: globalHeaders()
      });
      const isBlob = blobValidate(res.data);
      if (isBlob) {
        const blob = new Blob([res.data], { type: 'application/octet-stream' });
        FileSaver.saveAs(blob, decodeURIComponent(res.headers['download-filename'] as string));
      } else {
        this.printErrMsg(res.data);
      }
      downloadLoadingInstance.hide();
    } catch (r) {
      console.error(r);
      MessagePlugin.error('下载文件出现错误，请联系管理员！');
      downloadLoadingInstance.hide();
    }
  },
  async zip(url: string, name: string) {
    url = baseURL + url;
    downloadLoadingInstance = LoadingPlugin({ fullscreen: true, text: '正在下载数据，请稍候', preventScrollThrough: true });
    try {
      const res = await axios({
        method: 'get',
        url: url,
        responseType: 'blob',
        headers: globalHeaders()
      });
      const isBlob = blobValidate(res.data);
      if (isBlob) {
        const blob = new Blob([res.data], { type: 'application/zip' });
        FileSaver.saveAs(blob, name);
      } else {
        this.printErrMsg(res.data);
      }
      downloadLoadingInstance.hide();
    } catch (r) {
      console.error(r);
      MessagePlugin.error('下载文件出现错误，请联系管理员！');
      downloadLoadingInstance.hide();
    }
  },
  async printErrMsg(data: any) {
    const resText = await data.text();
    const rspObj = JSON.parse(resText);
    const errMsg = errorCode[rspObj.code] || rspObj.msg || errorCode['default'];
    MessagePlugin.error(errMsg);
  }
};
