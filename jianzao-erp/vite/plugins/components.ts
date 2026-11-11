import Components from 'unplugin-vue-components/vite';
import { TDesignResolver } from 'unplugin-vue-components/resolvers';

export default (path: any) => {
  return Components({
    resolvers: [
      // 全站统一 TDesign 组件自动导入（DOC/35 §3）
      TDesignResolver({
        library: 'vue-next'
      })
    ],
    dts: path.resolve(path.resolve(__dirname, '../../src'), 'types', 'components.d.ts')
  });
};
