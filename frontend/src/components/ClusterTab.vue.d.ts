import { DefineComponent } from 'vue'

declare const ClusterTab: DefineComponent<
  {
    // props 类型定义
  },
  {
    // methods 类型定义
  },
  {
    // emit 事件类型定义
    'cluster-connected': (data: { cluster: { host: string; port: number }; nodes: any }) => void;
    'connection-error': (errorMessage: string) => void;
  }
>

export default ClusterTab 