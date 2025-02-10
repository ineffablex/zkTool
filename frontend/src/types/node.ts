export interface NodeData {
  path: string;
  name: string;
  data?: string;
  children?: NodeData[];
  expanded?: boolean;
  selected?: boolean;
  isLeaf?: boolean;
  status?: string;
  createTime?: string;
  updateTime?: string;
  version?: string;
}

export interface ContextMenuItem {
  label: string;
  icon?: string;
  action: () => void;
  disabled?: boolean;
}

export type NodeOperation = 'create' | 'update' | 'delete' | 'copy' | 'move';

export interface NodeOperationData {
  path: string;
  data?: string;
  mode?: string;
  version?: number;
}

export interface ApiResponse<T = any> {
  success: boolean;
  message?: string;
  data?: T;
}