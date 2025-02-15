import { defineStore } from 'pinia'
import type { NodeData, NodeOperation, NodeOperationData, ApiResponse } from '../types/node'

interface State {
  currentCluster: string | null;
  clusters: string[];
  nodeTree: NodeData[];
  selectedNode: NodeData | null;
  loading: boolean;
  error: string | null;
  isConnected: boolean;
}

export const useZkStore = defineStore('zk', {
  state: (): State => ({
    currentCluster: null,
    clusters: [],
    nodeTree: [],
    selectedNode: null,
    loading: false,
    error: null,
    isConnected: false
  }),

  getters: {
    isConnected: (state) => !!state.currentCluster,
    hasSelectedNode: (state) => !!state.selectedNode
  },

  actions: {
    // API请求封装
    async request<T>(url: string, options: RequestInit = {}): Promise<T> {
      try {
        const response = await fetch(url, {
          ...options,
          headers: {
            'Content-Type': 'application/json',
            ...options.headers,
          },
        });

        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }

        const result = await response.json();
        if (!result.success) {
          // 对特定错误进行静默处理
          if ((options.method === 'POST' && 
               url.endsWith('/api/zk/nodes') && 
               result.message && 
               result.message.includes('NodeExists')) ||
              (result.message && result.message.includes('NoNode'))) {
            throw new Error(result.message.includes('NodeExists') ? 'NodeExists' : 'NoNode');
          }
          throw new Error(result.message || '操作失败');
        }

        return result;
      } catch (error) {
        // 如果不是NodeExists或NoNode错误，才打印错误日志
        if (!(error instanceof Error && (error.message === 'NodeExists' || error.message === 'NoNode'))) {
          console.error('API请求失败:', error);
        }
        throw error;
      }
    },

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
        const result = await this.request<ApiResponse>(
          `/api/zk/connect?address=${encodeURIComponent(address)}`
        );
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
        const result = await this.request<ApiResponse>(
          `/api/zk/nodes?cluster=${encodeURIComponent(this.currentCluster)}`
        );
        this.nodeTree = this.processNodes([result.data])
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
        const result = await this.request<ApiResponse>(
          `/api/zk/nodes?cluster=${encodeURIComponent(this.currentCluster)}&path=${encodeURIComponent(parentPath)}`
        );
        
        // 更新节点树中的子节点
        const updateNodeChildren = (nodes: NodeData[]): boolean => {
          for (const node of nodes) {
            if (node.path === parentPath) {
              const processedData = this.processNodes([result.data])[0]
              node.children = processedData.children || []
              node.expanded = true // 确保父节点展开
              return true
            }
            if (node.children && updateNodeChildren(node.children)) {
              return true
            }
          }
          return false
        }
        
        updateNodeChildren(this.nodeTree)
      } catch (error: any) {
        // 只有在不是NoNode错误时才记录警告
        if (!(error instanceof Error && error.message === 'NoNode')) {
          console.warn('获取子节点信息失败:', error)
        }
      }
    },

    setSelectedNode(node: NodeData | null) {
      this.selectedNode = node
    },

    async performNodeOperation(operation: NodeOperation, data: NodeOperationData) {
      this.loading = true
      this.error = null
      let response: ApiResponse | undefined
      try {
        if (operation === 'delete') {
          // 检查是否是受保护的 zookeeper 节点
          if (data.path.startsWith('/zookeeper')) {
            throw new Error('系统保护节点，不允许删除');
          }
          
          // 删除节点逻辑
          try {
            // 先获取节点信息，检查是否有子节点
            const nodeInfo = await this.request<ApiResponse>(`/api/zk/nodes?path=${encodeURIComponent(data.path)}`);
            
            // 如果有子节点，先递归删除子节点
            if (nodeInfo.success && nodeInfo.data.children && nodeInfo.data.children.length > 0) {
              for (const child of nodeInfo.data.children) {
                await this.performNodeOperation('delete', {
                  path: child.path,
                  version: -1  // 使用-1表示不检查版本
                });
              }
            }
            
            // 删除当前节点
            response = await this.request<ApiResponse>('/api/zk/nodes/delete', {
              method: 'DELETE',
              body: JSON.stringify({
                cluster: this.currentCluster,
                path: data.path,
                version: data.version || -1
              })
            });
            
            // 刷新父节点
            const parentPath = this.getParentPath(data.path)
            if (parentPath) {
              await this.fetchChildNodes(parentPath)
            } else {
              await this.fetchNodes()
            }
          } catch (error) {
            console.error('删除节点失败:', error);
            throw error;
          }
        } else if (operation === 'create') {
          // 获取父节点路径
          const parentPath = this.getParentPath(data.path)
          
          // 创建节点
          try {
            response = await this.request<ApiResponse>('/api/zk/nodes', {
              method: 'POST',
              body: JSON.stringify({
                cluster: this.currentCluster,
                path: data.path,
                data: data.data || '',
                mode: data.mode || 'PERSISTENT'
              })
            })
          } catch (error: any) {
            if (error.message === 'NodeExists') {
              // 如果节点已存在，尝试更新数据
              response = await this.request<ApiResponse>('/api/zk/nodes/update', {
                method: 'PUT',
                body: JSON.stringify({
                  cluster: this.currentCluster,
                  path: data.path,
                  data: data.data,
                  version: -1
                })
              })
            } else {
              throw error
            }
          }

          // 如果有父节点，更新父节点状态
          if (parentPath) {
            const parentNode = this.findNodeByPath(this.nodeTree, parentPath)
            if (parentNode) {
              // 如果父节点是叶子节点，更新其状态
              if (parentNode.isLeaf) {
                parentNode.isLeaf = false
                parentNode.children = []
              }

              // 如果父节点未展开，获取并展开
              if (!parentNode.expanded) {
                const nodeInfo = await this.request<ApiResponse>(`/api/zk/nodes?path=${encodeURIComponent(parentPath)}`)
                if (nodeInfo.success && nodeInfo.data) {
                  const processedNode = this.processNodes([nodeInfo.data])[0]
                  if (processedNode && processedNode.children) {
                    parentNode.children = processedNode.children
                    parentNode.expanded = true
                  }
                }
              } else {
                // 如果父节点已展开，只更新子节点列表
                const nodeInfo = await this.request<ApiResponse>(`/api/zk/nodes?path=${encodeURIComponent(parentPath)}`)
                if (nodeInfo.success && nodeInfo.data) {
                  const processedNode = this.processNodes([nodeInfo.data])[0]
                  if (processedNode && processedNode.children) {
                    parentNode.children = processedNode.children
                  }
                }
              }

              // 找到并选中新创建的节点
              if (parentNode.children) {
                const newNode = parentNode.children.find(child => child.path === data.path)
                if (newNode) {
                  this.setSelectedNode(newNode)
                }
              }
            }
          }
        } else if (operation === 'update') {
          // 检查是否是受保护的 zookeeper 节点
          if (data.path.startsWith('/zookeeper')) {
            throw new Error('系统保护节点，不允许修改');
          }
          
          // 更新节点逻辑
          response = await this.request<ApiResponse>('/api/zk/nodes/update', {
            method: 'PUT',
            body: JSON.stringify({
              cluster: this.currentCluster,
              path: data.path,
              data: data.data,
              version: -1  // 使用-1表示不检查版本
            })
          });
          
          // 更新节点数据后刷新节点信息
          try {
            const result = await this.request<ApiResponse>(`/api/zk/nodes?path=${encodeURIComponent(data.path)}`);
            if (result.success && result.data) {
              // 更新节点树中的节点数据
              const updateNodeData = (nodes: NodeData[]): boolean => {
                for (const node of nodes) {
                  if (node.path === data.path) {
                    Object.assign(node, result.data)
                    return true
                  }
                  if (node.children && updateNodeData(node.children)) {
                    return true
                  }
                }
                return false
              }
              updateNodeData(this.nodeTree)
              this.setSelectedNode(result.data)
            }
          } catch (err) {
            console.warn('刷新节点信息失败:', err)
          }
        }
        
        return response
      } catch (error) {
        this.error = `节点操作失败: ${operation}`
        console.error(`节点操作失败 ${operation}:`, error)
        throw error
      } finally {
        this.loading = false
      }
    },

    // 获取父节点路径
    getParentPath(path: string): string | null {
      if (!path || path === '/') return null
      const lastSlashIndex = path.lastIndexOf('/')
      return lastSlashIndex === 0 ? '/' : path.substring(0, lastSlashIndex)
    },

    // 工具方法
    processNodes(nodes: NodeData[]): NodeData[] {
      if (!Array.isArray(nodes)) {
        console.warn('processNodes received non-array input:', nodes)
        return []
      }
      try {
        return nodes.map(node => ({
          ...node,
          expanded: false,
          selected: false,
          isLeaf: !node.children || node.children.length === 0,
          children: node.children ? this.processNodes(node.children) : []
        }))
      } catch (error) {
        console.error('处理节点数据失败:', error)
        return []
      }
    },

    // 更新节点展开状态
    updateNodeExpandState(path: string, expanded: boolean) {
      const updateNode = (nodes: NodeData[]): boolean => {
        for (const node of nodes) {
          if (node.path === path) {
            node.expanded = expanded;
            return true;
          }
          if (node.children && updateNode(node.children)) {
            return true;
          }
        }
        return false;
      };
      updateNode(this.nodeTree);
    },

    findNodeByPath(nodes: NodeData[], path: string): NodeData | undefined {
      for (const node of nodes) {
        if (node.path === path) {
          return node;
        }
        if (node.children) {
          const found = this.findNodeByPath(node.children, path);
          if (found) {
            return found;
          }
        }
      }
      return undefined;
    }
  }
})
