<template>
  <div class="h-screen flex flex-col">
    <!-- 顶部操作栏 -->
    <div class="bg-white border-b p-4 flex items-center space-x-4">
      <!-- 集群连接区域 -->
      <div class="flex items-center space-x-2 flex-1">
        <input
          v-model="clusterAddress"
          type="text"
          placeholder="输入Zookeeper集群地址 (例如: localhost:2181)"
          class="flex-1 p-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
          @keydown.enter.prevent="handleConnect"
        />
        <button
          @click="handleConnect"
          class="px-4 py-2 bg-blue-500 text-white rounded-md hover:bg-blue-600 focus:outline-none focus:ring-2 focus:ring-blue-500"
          :disabled="!clusterAddress || store.loading"
        >
          {{ store.loading ? '连接中...' : '连接' }}
        </button>
      </div>

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
          class="px-4 py-2 bg-gray-500 text-white rounded-md hover:bg-gray-600 focus:outline-none focus:ring-2 focus:ring-gray-500"
          :disabled="!store.isConnected"
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
    <div v-if="error" class="p-4 bg-red-100 text-red-700">
      {{ error }}
    </div>

    <!-- 主内容区域 -->
    <div class="flex-1 flex overflow-hidden">
      <!-- 左侧树形结构 -->
      <div class="w-1/3 border-r bg-white overflow-hidden flex flex-col">
        <div class="p-4 border-b bg-gray-50">
          <h2 class="text-lg font-medium">节点树</h2>
        </div>
        <div class="flex-1 overflow-auto">
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

      <!-- 右侧节点详情 -->
      <div class="flex-1 bg-white overflow-hidden flex flex-col">
        <div class="p-4 border-b bg-gray-50">
          <h2 class="text-lg font-medium">节点详情</h2>
        </div>
        <div class="flex-1 overflow-auto p-4">
          <template v-if="store.selectedNode">
            <!-- 节点基本信息 -->
            <div class="mb-6">
              <h3 class="text-sm font-medium text-gray-500 mb-2">基本信息</h3>
              <div class="space-y-2">
                <div class="flex">
                  <span class="w-24 text-gray-500">节点路径:</span>
                  <span class="flex-1">{{ store.selectedNode.path }}</span>
                </div>
                <div class="flex">
                  <span class="w-24 text-gray-500">创建时间:</span>
                  <span class="flex-1">{{ store.selectedNode.createTime }}</span>
                </div>
                <div class="flex">
                  <span class="w-24 text-gray-500">修改时间:</span>
                  <span class="flex-1">{{ store.selectedNode.updateTime }}</span>
                </div>
                <div class="flex">
                  <span class="w-24 text-gray-500">版本:</span>
                  <span class="flex-1">{{ store.selectedNode.version }}</span>
                </div>
              </div>
            </div>

            <!-- 节点数据 -->
            <div>
              <h3 class="text-sm font-medium text-gray-500 mb-2">节点数据</h3>
              <div class="border rounded-md bg-gray-50 p-4">
                <pre 
                  class="whitespace-pre-wrap" 
                  contenteditable="true"
                  @focusout="handleNodeDataEdit" 
                  @click="toggleNodeDataEdit"
                  @keydown.enter.prevent="handleEnterKey"
                  >{{ formatNodeData(store.selectedNode.data) }}</pre>
              </div>
            </div>
          </template>
          <div v-else class="h-full flex items-center justify-center text-gray-400">
            请选择一个节点查看详情
          </div>
        </div>
      </div>
    </div>

    <!-- 对话框组件 -->
    <NodeEditDialog
      v-model:show="showNodeEditDialog"
      :mode="editMode"
      :node="store.selectedNode"
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
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useZkStore } from '../store'
import TreeView from '../components/TreeView.vue'
import DialogModal from '../components/DialogModal.vue'
import NodeEditDialog from '../components/NodeEditDialog.vue'
import type { NodeData, NodeOperationData, ApiResponse } from '../types/node'

const store = useZkStore()
const clusterAddress = ref('')
const jumpPath = ref('')
const error = ref<string>('')

// 对话框状态
const showNodeEditDialog = ref(false)
const showDeleteConfirmDialog = ref(false)
const editMode = ref<'create' | 'update'>('create')

// 连接处理
const handleConnect = async () => {
  if (!clusterAddress.value) return
  error.value = ''
  
  try {
    await store.connect(clusterAddress.value)
  } catch (err) {
    const errorMessage = err instanceof Error ? err.message : '未知错误'
    error.value = `连接失败: ${errorMessage}`
    console.error('连接失败:', err)
  }
}

// 节点点击处理
const handleNodeClick = (node: NodeData) => {
  store.setSelectedNode(node)
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
  if (!jumpPath.value) return
  try {
    // 去除首尾空格并规范化路径
    const path = jumpPath.value.trim().replace(/^\/+|\/+$/g, '')
    const fullPath = '/' + path

    const response = await store.request<ApiResponse<NodeData>>(`/api/zk/nodes?path=${encodeURIComponent(fullPath)}`)
    
    if (response.success && response.data) {
      // 展开路径上的所有父节点
      const pathParts = fullPath.split('/').filter(Boolean)
      let currentPath = ''
      
      for (const part of pathParts) {
        currentPath += '/' + part
        if (currentPath !== fullPath) { // 不是目标节点时才需要展开
          await store.fetchChildNodes(currentPath)
        }
      }
      
      // 选中目标节点
      store.setSelectedNode(response.data)
      
      // 清空输入
      jumpPath.value = ''
    }
  } catch (err) {
    console.error('节点跳转失败:', err)
    const errorMessage = err instanceof Error ? err.message : '未知错误'
    error.value = `跳转失败: ${errorMessage}`
  }
}

// 节点操作处理
const handleNodeOperation = ({ type, node }: { type: 'create' | 'update' | 'delete'; node: NodeData }) => {
  if (type === 'create') {
    editMode.value = 'create'
    showNodeEditDialog.value = true
  } else if (type === 'update') {
    editMode.value = 'update'
    showNodeEditDialog.value = true
  } else if (type === 'delete') {
    if (node.path.startsWith('/zookeeper')) {
      error.value = '系统保护节点，不允许删除'
      return
    }
    showDeleteConfirmDialog.value = true
  }
}

const handleNodeEdit = async (data: NodeOperationData) => {
  try {
    if (editMode.value === 'create') {
      // 如果是在现有节点下创建子节点，需要拼接完整路径
      if (store.selectedNode) {
        const parentPath = store.selectedNode.path
        const nodePath = data.path.startsWith('/') ? data.path.slice(1) : data.path
        data.path = parentPath === '/' ? '/' + nodePath : parentPath + '/' + nodePath
      }
      await store.performNodeOperation('create', data)
    } else {
      await store.performNodeOperation('update', {
        path: store.selectedNode!.path,
        data: data.data
      })
    }
    showNodeEditDialog.value = false
  } catch (err) {
    const errorMessage = err instanceof Error ? err.message : '未知错误'
    error.value = `${editMode.value === 'create' ? '创建' : '更新'}节点失败: ${errorMessage}`
  }
}

const handleNodeDelete = async () => {
  if (!store.selectedNode) return
  try {
    await store.performNodeOperation('delete', {
      path: store.selectedNode.path
    })
    showDeleteConfirmDialog.value = false
    store.setSelectedNode(null)
  } catch (err) {
    const errorMessage = err instanceof Error ? err.message : '未知错误'
    error.value = `删除节点失败: ${errorMessage}`
  }
}

// 节点详情编辑
const handleNodeDataEdit = async (event: FocusEvent) => {
  const target = event.target as HTMLPreElement
  const newData = target.textContent || ''
  const originalData = store.selectedNode?.data || ''
  
  if (!store.selectedNode || newData === originalData) {
    target.contentEditable = 'false'
    target.classList.remove('bg-yellow-50')
    return
  }

  try {
    await store.performNodeOperation('update', {
      path: store.selectedNode.path,
      data: newData
    })
    target.contentEditable = 'false'
    target.classList.remove('bg-yellow-50')
  } catch (err) {
    const errorMessage = err instanceof Error ? err.message : '未知错误'
    error.value = `更新节点数据失败: ${errorMessage}`
    // 恢复原始数据
    target.textContent = formatNodeData(originalData)
  }
}

// 切换节点数据为可编辑状态
const toggleNodeDataEdit = (event: MouseEvent) => {
  const target = event.target as HTMLPreElement
  if (target.contentEditable === 'true') return
  
  target.contentEditable = 'true'
  target.classList.add('bg-yellow-50')
  target.focus()
  
  // 选择所有文本
  const range = document.createRange()
  range.selectNodeContents(target)
  const selection = window.getSelection()
  if (selection) {
    selection.removeAllRanges()
    selection.addRange(range)
  }
}

// 格式化节点数据
const formatNodeData = (data: string | undefined | null): string => {
  if (!data) return '无数据'
  try {
    const parsed = JSON.parse(data)
    return JSON.stringify(parsed, null, 2)
  } catch {
    return data
  }
}

// 顶部按钮处理函数
const handleCreateNode = () => {
  store.setSelectedNode(null) // 清除选中节点，创建根节点
  handleNodeOperation({ type: 'create', node: { path: '/', name: 'root' } as NodeData })
}

const handleUpdateNode = () => {
  // 不再需要这个函数，因为我们使用直接编辑
}

const handleDeleteNode = () => {
  if (!store.selectedNode) return
  handleNodeOperation({ type: 'delete', node: store.selectedNode })
}

// 处理回车键
const handleEnterKey = (event: KeyboardEvent) => {
  const target = event.target as HTMLPreElement
  if (target) {
    target.blur()
  }
}
</script>

<style scoped>
pre[contenteditable="true"] {
  @apply outline-none border-2 border-blue-300 rounded-md p-2;
}
</style>