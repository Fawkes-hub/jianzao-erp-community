import request from '@/utils/request';
import { AxiosPromise } from 'axios';

export interface SysLogFileVO {
  name: string;
  label: string;
  exists: boolean;
  sizeBytes: number;
}

export interface SysLogTailVO {
  name: string;
  content: string;
}

// 可查看的日志文件列表
export function listFiles(): AxiosPromise<SysLogFileVO[]> {
  return request({
    url: '/monitor/syslog/files',
    method: 'get'
  });
}

// 读取日志尾部
export function tailLog(name: string, lines: number): AxiosPromise<SysLogTailVO> {
  return request({
    url: '/monitor/syslog/tail',
    method: 'get',
    params: { name, lines }
  });
}

// 下载日志文件（带认证头，走 blob）
export function downloadLog(name: string): AxiosPromise<Blob> {
  return request({
    url: '/monitor/syslog/download',
    method: 'get',
    params: { name },
    responseType: 'blob'
  });
}
