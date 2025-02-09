import { defineStore } from 'pinia'
import { NodeData, NodeOperation } from '../types/node'

interface State {
  currentCluster: string | null;
  clusters: string[];
  nodeTree: NodeData[];
  selectedNode: NodeData | null;
  loading: boolean;
  error: string | null;
}

export const useZkStore = defineStore('zk', {
  state: (): State => ({
    currentCluster: null,
    clusters: [],
    nodeTree: [],
    selectedNode: null,
    loading: false,
    error: null
  }),

  getters: {
    isConnected: (state) => !!state.currentCluster,
    hasSelectedNode: (state) => !!state.selectedNode
  },

  actions: {
    // 集群管理
    async fetchClusters() {
      this.loading = true
      this.error = null
      try {
        const response = await fetch('/api/zk/clusters')
        const data = await response.json()
        this.clusters = data
      } catch (error) {
        this.error = '获取集群列表失败'
        console.error('Failed to fetch clusters:', error)
      } finally {
        this.loading = false
      }
    },

    async connect(address: string) {
      this.loading = true
      this.error = null
      try {
        const response = await fetch(`api/zk/connect?address=${encodeURIComponent(address)}`)
        if (!response.ok) {
          throw new Error('连接失败')
        }
        this.currentCluster = address
        await this.fetchNodes()
      } catch (error) {
        this.error = '连接集群失败'
        console.error('连接失败:', error)
        throw error
      } finally {
        this.loading = false
      }
    },

    async fetchNodeTree(cluster: string) {
      this.loading = true
      this.error = null
      try {
        // 如果不是当前连接的集群，先进行连接
        if (cluster !== this.currentCluster) {
          await this.connect(cluster)
        }
        // 获取节点树
        await this.fetchNodes()
      } catch (error) {
        this.error = '获取节点树失败'
        console.error('获取节点树失败:', error)
        throw error
      } finally {
        this.loading = false
      }
    },

    // 节点操作
    async fetchNodes() {
      if (!this.currentCluster) {
        throw new Error('请先连接集群')
      }
      
      this.loading = true
      this.error = null
      try {
        const response = await fetch(`/api/zk/nodes?cluster=${encodeURIComponent(this.currentCluster)}`)
        if (!response.ok) {
          throw new Error('获取节点信息失败')
        }
        const data = await response.json()
        this.nodeTree = this.processNodes(data)
      } catch (error) {
        this.error = '获取节点信息失败'
        console.error('获取节点信息失败:', error)
        throw error
      } finally {
        this.loading = false
      }
    },

    async fetchChildNodes(parentPath: string) {
      if (!this.currentCluster) {
        throw new Error('请先连接集群')
      }
      
      this.error = null
      try {
        const response = await fetch(
          `/api/zk/nodes/children?cluster=${encodeURIComponent(this.currentCluster)}&path=${encodeURIComponent(parentPath)}`
        )
        if (!response.ok) {
          throw new Error('获取子节点信息失败')
        }
        const childNodes = await response.json()
        
        // 更新节点树中的子节点
        const updateNodeChildren = (nodes: NodeData[]): boolean => {
          for (const node of nodes) {
            if (node.path === parentPath) {
              node.children = this.processNodes(childNodes)
              return true
            }
            if (node.children && updateNodeChildren(node.children)) {
              return true
            }
          }
          return false
        }
        
        updateNodeChildren(this.nodeTree)
      } catch (error) {
        this.error = '获取子节点信息失败'
        console.error('获取子节点信息失败:', error)
        throw error
      }
    },

    setSelectedNode(node: NodeData | null) {
      this.selectedNode = node
    },

    async performNodeOperation(operation: NodeOperation, node: NodeData, data?: any) {
      this.loading = true
      this.error = null
      try {
        const url = `/api/zk/nodes/${operation}`
        const response = await fetch(url, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            cluster: this.currentCluster,
            path: node.path,
            ...data
          })
        })

        if (!response.ok) {
          throw new Error(`操作失败: ${operation}`)
        }

        // 刷新节点树
        await this.fetchNodes()
      } catch (error) {
        this.error = `节点操作失败: ${operation}`
        console.error(`节点操作失败 ${operation}:`, error)
        throw error
      } finally {
        this.loading = false
      }
    },

    // 工具方法
    processNodes(nodes: NodeData[]): NodeData[] {
      return nodes.map(node => ({
        ...node,
        expanded: false,
        selected: false,
        isLeaf: !node.children || node.children.length === 0,
        children: node.children ? this.processNodes(node.children) : undefined
      }))
    }
  }
})
