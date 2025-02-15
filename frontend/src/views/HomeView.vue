<template>
  <div class="home">
    <div class="resize-container">
      <ClusterTab 
        @cluster-connected="handleClusterConnected" 
        @connection-error="handleConnectionError"
      />
      <div class="main-content">
        <!-- 顶部操作栏 -->
        <div class="bg-white border-b p-4 flex items-center space-x-4">
          <!-- 节点路径跳转 -->
          <div class="flex items-center space-x-2 flex-1">
            <input
              v-model="jumpPath"
              type="text"
              placeholder="输入节点路径 (例如: /zookeeper)"
              class="flex-1 p-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              @keydown.enter.prevent="handleJumpToNode"
            />
            <button
              @click="handleJumpToNode"
              class="px-4 py-2 bg-gray-500 text-white rounded-md hover:bg-gray-600 focus:outline-none focus:ring-2 focus:ring-gray-500 disabled:opacity-50 disabled:cursor-not-allowed"
              :disabled="!store.isConnected"
              :title="!store.isConnected ? '请先连接集群' : ''"
            >
              跳转
            </button>
          </div>

          <!-- 节点操作按钮组 -->
          <div class="flex items-center space-x-2">
            <button
              @click="handleCreateNode"
              class="px-3 py-2 bg-green-500 text-white rounded-md hover:bg-green-600 focus:outline-none focus:ring-2 focus:ring-green-500"
              :disabled="!store.isConnected"
            >
              新增
            </button>
            <button
              @click="handleUpdateNode"
              class="px-3 py-2 bg-yellow-500 text-white rounded-md hover:bg-yellow-600 focus:outline-none focus:ring-2 focus:ring-yellow-500"
              :disabled="!store.hasSelectedNode"
            >
              修改
            </button>
            <button
              @click="handleDeleteNode"
              class="px-3 py-2 bg-red-500 text-white rounded-md hover:bg-red-600 focus:outline-none focus:ring-2 focus:ring-red-500"
              :disabled="!store.hasSelectedNode"
            >
              删除
            </button>
          </div>
        </div>

        <!-- 错误提示 -->
        <!-- <div v-if="error" class="p-4 bg-red-100 text-red-700">
          {{ error }}
        </div> -->

        <!-- 主内容区域 -->
        <div class="flex-1 flex overflow-hidden content-wrapper">
          <!-- 左侧树形结构 -->
          <div class="w-1/3 border-r bg-white overflow-hidden flex flex-col tree-wrapper">
            <div class="p-4 border-b bg-gray-50">
              <h2 class="text-lg font-medium">节点树</h2>
            </div>
            <div class="flex-1 overflow-auto custom-scrollbar">
              <TreeView
                v-if="store.nodeTree.length > 0"
                :data="store.nodeTree"
                :item-height="32"
                :visible-items="20"
                @node-click="handleNodeClick"
                @toggle-expand="handleToggleExpand"
                @node-context-menu="handleContextMenu"
                @node-operation="handleNodeOperation"
              />
            </div>
          </div>
          <!-- 右侧节点内容 -->
          <div class="flex-1 bg-white overflow-hidden flex flex-col content-panel">
            <div class="p-4 border-b bg-gray-50">
              <h2 class="text-lg font-medium">节点内容</h2>
            </div>
            <div class="flex-1 overflow-auto custom-scrollbar">
              <pre class="overflow-wrap break-word w-full" contenteditable="true" 
              @keydown.enter.prevent="handleEnterKey"  
              style="text-align: left;">{{ formatNodeData(store.selectedNode?.data) }}</pre>
            </div>
          </div>
        </div>

        <!-- 对话框组件 -->
        <NodeEditDialog
          v-model:show="showNodeEditDialog"
          :mode="editMode"
          :node="store.selectedNode"
          :parent-path="editMode === 'create' ? store.selectedNode?.path : ''"
          @submit="handleNodeEdit"
        />

        <DialogModal
          v-model:show="showDeleteConfirmDialog"
          title="删除确认"
          showCancel
          showConfirm
          cancelText="取消"
          confirmText="删除"
          confirmButtonClass="bg-red-500 text-white hover:bg-red-600 focus:ring-red-500"
          @confirm="handleNodeDelete"
        >
          <p class="text-gray-600">确定要删除节点 "{{ store.selectedNode?.path }}" 吗？此操作不可撤销。</p>
        </DialogModal>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useZkStore } from '../store'
import TreeView from '../components/TreeView.vue'
import DialogModal from '../components/DialogModal.vue'
import NodeEditDialog from '../components/NodeEditDialog.vue'
import type { NodeData, NodeOperationData, ApiResponse } from '../types/node'
import ClusterTab from '@/components/ClusterTab.vue'
import { ElMessage } from 'element-plus'

const store = useZkStore()
const jumpPath = ref('')
const error = ref<string>('')

// 集群管理状态
const selectedCluster = ref<any>(null)

// 对话框状态
const showNodeEditDialog = ref(false)
const showDeleteConfirmDialog = ref(false)
const editMode = ref<'create' | 'update'>('create')

// 新的状态
const isEditing = ref(false)

// 新增状态变量
const tempNodeData = ref<string>('')
const hasDataChanged = ref(false)

// 节点点击处理
const handleNodeClick = (node: NodeData) => {
  // 清理内容
  const preElement = document.querySelector('pre[contenteditable="true"]') as HTMLPreElement;
  if (preElement) {
    preElement.textContent = preElement.textContent?.replace(/^[\s\n]+|[\s\n]+$/g, '') || '';
  }
  // 设置新选中的节点
  store.setSelectedNode(node);
  // 同步节点路径到输入框
  jumpPath.value = node.path;
}

// 节点展开/折叠处理
const handleToggleExpand = async ({ node, expanded }: { node: NodeData; expanded: boolean }) => {
  try {
    if (expanded && (!node.children || node.children.length === 0)) {
      await store.fetchChildNodes(node.path)
    }
  } catch (error) {
    console.error('展开节点失败:', error)
  }
}

// 右键菜单处理
const handleContextMenu = ({ node, x, y }: { node: NodeData; x: number; y: number }) => {
  console.log('右键菜单:', { node: node.path, x, y })
}

// 节点路径跳转
const handleJumpToNode = async () => {
  if (!jumpPath.value) return;
  
  // 检查连接状态
  if (!store.isConnected) {
    error.value = '请先连接到集群';
    return;
  }

  error.value = '';
  try {
    // 去除首尾空格并规范化路径
    const path = jumpPath.value.trim().replace(/^\/+|\/+$/g, '');
    const fullPath = '/' + path;

    // 获取目标节点数据
    const response = await store.request<ApiResponse<NodeData>>(`/api/zk/nodes?path=${encodeURIComponent(fullPath)}`);
    
    if (response.success && response.data) {
      // 展开路径上的所有父节点
      const pathParts = fullPath.split('/').filter(Boolean);
      
      // 检查根节点状态
      const rootNode = store.nodeTree[0];
      if (rootNode && !rootNode.expanded) {
        await store.fetchChildNodes('/');
        store.updateNodeExpandState('/', true);
        // 等待根节点展开
        await new Promise(resolve => setTimeout(resolve, 50));
      }
      
      // 逐级检查并展开节点
      let currentPath = '';
      let currentNode = rootNode;
      
      for (const part of pathParts) {
        currentPath += '/' + part;
        if (currentPath === '/') continue;
        
        // 在当前节点的子节点中查找目标节点
        const targetNode = currentNode?.children?.find(child => child.path === currentPath);
        
        // 如果找到节点但未展开，则展开它
        if (targetNode && !targetNode.expanded) {
          try {
            await store.fetchChildNodes(currentPath);
            store.updateNodeExpandState(currentPath, true);
            // 更新当前节点引用
            currentNode = targetNode;
            // 短暂等待以确保 DOM 更新
            await new Promise(resolve => setTimeout(resolve, 50));
          } catch (err) {
            console.error(`展开节点 ${currentPath} 失败:`, err);
          }
        } else if (targetNode) {
          // 如果节点已经展开，直接更新当前节点引用
          currentNode = targetNode;
        }
      }
      
      // 选中目标节点并更新路径
      store.setSelectedNode(response.data);
      jumpPath.value = fullPath;
      error.value = '';
    }
  } catch (err) {
    console.error('节点跳转失败:', err);
    const errorMessage = err instanceof Error ? err.message : '未知错误';
    error.value = `跳转失败: ${errorMessage}`;
  }
};

// 节点操作处理
const handleNodeOperation = ({ type, node }: { type: 'create' | 'update' | 'delete'; node: NodeData }) => {
  if (type === 'create') {
    editMode.value = 'create'
    showNodeEditDialog.value = true
  } else if (type === 'update') {
    editMode.value = 'update'
    // 确保在打开对话框前已经设置了选中的节点
    store.setSelectedNode(node)
    showNodeEditDialog.value = true
  } else if (type === 'delete') {
    if (node.path.startsWith('/zookeeper')) {
      error.value = '系统保护节点，不允许删除'
      ElMessage.error(error.value)
      return
    }
    // 检查是否有子节点
    if (node.children && node.children.length > 0) {
      error.value = '该节点包含子节点，不能直接删除'
      ElMessage.error(error.value)
      return
    }
    showDeleteConfirmDialog.value = true
  }
}

const handleNodeEdit = async (data: NodeOperationData) => {
  try {
    // 确保data.data是字符串，去除前后的空格
    data.data = typeof data.data === 'string' ? data.data.trim() : '';

    if (editMode.value === 'create') {
      // 如果是在现有节点下创建子节点，需要拼接完整路径
      let parentNode = null;
      if (store.selectedNode) {
        const parentPath = store.selectedNode.path;
        const nodePath = data.path.startsWith('/') ? data.path.slice(1) : data.path;
        data.path = parentPath === '/' ? '/' + nodePath : parentPath + '/' + nodePath;
        parentNode = store.selectedNode;
      }

      await store.performNodeOperation('create', data);
      ElMessage.success('节点创建成功');

      // 智能处理节点展开
      if (parentNode) {
        // 如果父节点是叶子节点，需要更新其状态
        if (parentNode.isLeaf) {
          parentNode.isLeaf = false;
          parentNode.children = [];
        }

        // 如果父节点未展开，则只需要展开父节点
        if (!parentNode.expanded) {
          const response = await store.request<ApiResponse<NodeData>>(`/api/zk/nodes?path=${encodeURIComponent(parentNode.path)}`);
          if (response.success && response.data) {
            parentNode.children = response.data.children || [];
            parentNode.expanded = true;
            
            // 找到新创建的节点并选中它
            const newNode = parentNode.children.find(child => child.path === data.path);
            if (newNode) {
              store.setSelectedNode(newNode);
              jumpPath.value = newNode.path;
            }
          }
        } else {
          // 如果父节点已经展开，只需要刷新其子节点
          const response = await store.request<ApiResponse<NodeData>>(`/api/zk/nodes?path=${encodeURIComponent(parentNode.path)}`);
          if (response.success && response.data) {
            parentNode.children = response.data.children || [];
            
            // 找到新创建的节点并选中它
            const newNode = parentNode.children.find(child => child.path === data.path);
            if (newNode) {
              store.setSelectedNode(newNode);
              jumpPath.value = newNode.path;
            }
          }
        }
      }
    } else if (editMode.value === 'update') {
      // 更新节点
      await store.performNodeOperation('update', {
        path: store.selectedNode!.path,
        data: data.data
      });
      
      // 更新成功后刷新节点信息
      const response = await store.request<ApiResponse<NodeData>>(`/api/zk/nodes?path=${encodeURIComponent(store.selectedNode!.path)}`);
      if (response.success && response.data) {
        // 更新节点数据
        store.setSelectedNode(response.data);
      }
      
      ElMessage.success('节点修改成功');
    }
    
    showNodeEditDialog.value = false;
    error.value = ''; // 清除可能存在的错误信息
  } catch (err) {
    const errorMessage = err instanceof Error ? err.message : '未知错误';
    error.value = `${editMode.value === 'create' ? '创建' : '更新'}节点失败: ${errorMessage}`;
    ElMessage.error(error.value);
  }
};

const handleNodeDelete = async () => {
  if (!store.selectedNode) return
  
  // 再次检查是否有子节点（以防在弹窗期间发生变化）
  if (store.selectedNode.children && store.selectedNode.children.length > 0) {
    error.value = '该节点包含子节点，不能直接删除'
    ElMessage.error(error.value);
    showDeleteConfirmDialog.value = false
    return
  }

  try {
    const deletedNodePath = store.selectedNode.path
    await store.performNodeOperation('delete', {
      path: deletedNodePath
    })

    // 获取父节点路径
    const parentPath = deletedNodePath.substring(0, deletedNodePath.lastIndexOf('/'))
    const parentNode = findNodeByPath(store.nodeTree, parentPath === '' ? '/' : parentPath)
    
    // 重新获取父节点的子节点数据
    if (parentNode) {
      const response = await store.request<ApiResponse<NodeData>>(`/api/zk/nodes?path=${encodeURIComponent(parentNode.path)}`);
      if (response.success && response.data) {
        // 更新父节点的子节点数据
        parentNode.children = response.data.children || []
        // 如果没有子节点了，将其标记为叶子节点
        if (parentNode.children.length === 0) {
          parentNode.isLeaf = true
        }
      }
    }

    showDeleteConfirmDialog.value = false
    store.setSelectedNode(null)
    error.value = '' // 清除可能存在的错误信息
    ElMessage.success('节点删除成功');
  } catch (err) {
    const errorMessage = err instanceof Error ? err.message : '未知错误'
    error.value = `删除节点失败: ${errorMessage}`
    ElMessage.error(error.value);
  }
}

// 添加查找节点的辅助函数
const findNodeByPath = (nodes: NodeData[], path: string): NodeData | null => {
  for (const node of nodes) {
    if (node.path === path) {
      return node
    }
    if (node.children) {
      const found = findNodeByPath(node.children, path)
      if (found) {
        return found
      }
    }
  }
  return null
}

// 格式化节点数据
const formatNodeData = (data: string | undefined | null): string => {
  if (data === undefined || data === null) return '';
  try {
    const parsed = JSON.parse(data);
    console.log("格式化节点数据", JSON.stringify(parsed, null, 2).trim());
    return JSON.stringify(parsed, null, 2).trim();
  } catch {
    return data.trim();
  }
};



// 修改回车键处理函数
const handleEnterKey = (event: KeyboardEvent) => {
  event.preventDefault();
}

// 修改更新节点函数
const handleUpdateNode = async () => {
  if (!store.selectedNode) return
  
  try {
    // 获取当前编辑框中的内容
    const preElement = document.querySelector('pre[contenteditable="true"]') as HTMLPreElement
    const newData = preElement?.textContent || ''
    
    // 如果数据没有变化，不做任何处理
    if (newData === formatNodeData(store.selectedNode.data)) {
      ElMessage.info('节点内容未发生变化')
      return
    }
    
    // 保存修改
    await store.performNodeOperation('update', {
      path: store.selectedNode.path,
      data: newData
    })
    
    // 更新成功后重置状态
    hasDataChanged.value = false
    tempNodeData.value = ''
    error.value = '' // 清除可能存在的错误信息
    ElMessage.success('节点内容修改成功')
  } catch (err) {
    const errorMessage = err instanceof Error ? err.message : '未知错误'
    error.value = `更新节点数据失败: ${errorMessage}`
    ElMessage.error(error.value)
  }
}

// 顶部按钮处理函数
const handleCreateNode = () => {
  store.setSelectedNode(null) // 清除选中节点，创建根节点
  handleNodeOperation({ type: 'create', node: { path: '/', name: 'root' } as NodeData })
}

const handleDeleteNode = () => {
  if (!store.selectedNode) return
  handleNodeOperation({ type: 'delete', node: store.selectedNode })
}

interface ClusterConnectionData {
  cluster: {
    host: string;
    port: number;
  };
  nodes: any; // 根据实际节点数据结构定义类型
}

const handleClusterConnected = async (data: ClusterConnectionData) => {
  try {
    // 更新状态和当前集群信息
    store.$patch({ 
      isConnected: true,
      currentCluster: `${data.cluster.host}:${data.cluster.port}`
    });
    
    // 更新节点树
    if (data.nodes) {
      store.$patch({ nodeTree: Array.isArray(data.nodes) ? data.nodes : [data.nodes] });
    }
    
    // 清除错误信息
    error.value = '';
  } catch (err: any) {
    error.value = `连接失败: ${err.message}`;
    console.error('连接失败:', err);
  }
};

const handleConnectionError = (errorMessage: string) => {
  error.value = errorMessage;
};
</script>

<style scoped>
.home {
  display: flex;
  margin-left: 5vh;
  height: 100vh;
  overflow: hidden;
}

.resize-container {
  display: flex;
  width: 100%;
  height: 100%;
  overflow: hidden;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  height: 100%;
  min-width: 0;
}

.main-content > .content-wrapper {
  height: calc(100vh - 4rem);
  min-height: 0;
}

.main-content .tree-wrapper {
  height: 100% !important;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.main-content .content-panel {
  height: 100% !important;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.node-content {
  flex: 1;
  overflow: auto;
  padding: 1rem;
  background-color: white;
}

pre[contenteditable="true"] {
  @apply outline-none border-2 border-blue-300 rounded-md p-2 w-full max-h-[98%] min-h-[98%] overflow-auto;
  white-space: pre-wrap;
  word-wrap: break-word;
}
</style>