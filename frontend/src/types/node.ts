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