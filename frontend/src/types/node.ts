export interface NodeStat {
  czxid: number
  mzxid: number
  ctime: number
  mtime: number
  version: number
  cversion: number
  aversion: number
  ephemeralOwner: number
  dataLength: number
  numChildren: number
  pzxid: number
}

export interface NodeData {
  path: string
  name: string
  data?: string
  children?: NodeData[]
  expanded?: boolean
  selected?: boolean
  isLeaf?: boolean
  status?: string
  createTime?: string
  updateTime?: string
  version?: string
  stat?: NodeStat
}

export interface ContextMenuItem {
  label: string
  icon?: string
  action: () => void
  items?: ContextMenuItem[]
  disabled?: boolean
}

export type NodeOperation = 'create' | 'update' | 'delete' | 'copy' | 'move'

export interface NodeOperationData {
  path: string
  data?: string
  version?: number
  mode?: string
}

export interface ApiResponse<T = any> {
  success: boolean
  message?: string
  data: T
}