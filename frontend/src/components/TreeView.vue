<template>
  <div ref="containerRef"
       class="tree-container overflow-auto"
       @scroll="handleScroll"
       :style="{ height: `${visibleItems * itemHeight}px` }">
    <div :style="{ height: `${getTotalHeight(data)}px`, position: 'relative' }">
      <ul class="tree-list absolute w-full"
          :style="{ transform: `translateY(${scrollTop}px)` }">
        <li v-for="{ node, level } in visibleNodes" 
            :key="node.path"
            class="tree-item"
            :style="{ 
              height: `${itemHeight}px`,
              paddingLeft: `${level * 1.5}rem`
            }">
          <div class="flex items-center h-full py-1 px-2 rounded-md hover:bg-gray-100 cursor-pointer select-none"
               :class="{ 'bg-blue-50': node.selected }"
               @click="handleNodeClick(node)"
               @contextmenu.prevent="showContextMenu($event, node)">
            <!-- 展开/折叠图标 -->
            <button v-if="!node.isLeaf"
                    @click.stop
                    class="mr-2 w-6 h-6 flex items-center justify-center text-gray-500 hover:text-gray-700"
                    data-testid="toggle-button">
              <svg v-if="node.expanded" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
              </svg>
              <svg v-else class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
              </svg>
            </button>
            <span v-else class="w-6"></span>

            <!-- 节点图标 -->
            <span class="mr-2">
              <svg v-if="node.isLeaf" 
                   class="w-4 h-4 text-gray-500" 
                   fill="none" 
                   stroke="currentColor" 
                   viewBox="0 0 24 24"
                   data-testid="leaf-icon">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 21h10a2 2 0 002-2V5a2 2 0 00-2-2H7a2 2 0 00-2 2v14a2 2 0 002 2z" />
              </svg>
              <svg v-else 
                   class="w-4 h-4 text-yellow-500" 
                   fill="none" 
                   stroke="currentColor" 
                   viewBox="0 0 24 24"
                   data-testid="folder-icon">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z" />
              </svg>
            </span>

            <!-- 节点名称 -->
            <span class="flex-grow truncate" :title="node.path">{{ node.name }}</span>

            <!-- 节点状态 -->
            <span v-if="node.status" 
                  class="ml-2 px-2 py-0.5 text-xs rounded-full"
                  :class="{
                    'bg-green-100 text-green-800': node.status === 'connected',
                    'bg-red-100 text-red-800': node.status === 'disconnected',
                    'bg-yellow-100 text-yellow-800': node.status === 'warning'
                  }">
              {{ node.status }}
            </span>
          </div>
        </li>
      </ul>
    </div>
  </div>

  <!-- 右键菜单 -->
  <ContextMenu
    v-model:show="contextMenu.show"
    :x="contextMenu.x"
    :y="contextMenu.y"
    :items="getContextMenuItems(contextMenu.node)"
    @close="closeContextMenu" />
</template>

<script setup lang="ts">
import { ref, watch, computed, onUnmounted } from 'vue'
import { useZkStore } from '../store'
import type { NodeData, ContextMenuItem } from '../types/node'
import ContextMenu from './ContextMenu.vue'

const props = defineProps<{
  data: NodeData[]
  itemHeight?: number
  visibleItems?: number
}>()

const containerRef = ref<HTMLElement | null>(null)
const scrollTop = ref(0)

// 设置默认值
const itemHeight = computed(() => props.itemHeight || 32) // 每个节点的高度
const visibleItems = computed(() => props.visibleItems || 20) // 可见节点数量

// 计算总高度
const getTotalHeight = (nodes: NodeData[]): number => {
  let height = nodes.length * itemHeight.value
  nodes.forEach(node => {
    if (node.expanded && node.children) {
      height += getTotalHeight(node.children)
    }
  })
  return height
}

// 扁平化树结构
const flattenTree = (nodes: NodeData[], level = 0): { node: NodeData; level: number }[] => {
  const result: { node: NodeData; level: number }[] = []
  nodes.forEach(node => {
    result.push({ node, level })
    if (node.expanded && node.children) {
      result.push(...flattenTree(node.children, level + 1))
    }
  })
  return result
}

// 计算可见节点
const visibleNodes = computed(() => {
  const flatNodes = flattenTree(props.data)
  const startIndex = Math.floor(scrollTop.value / itemHeight.value)
  const endIndex = Math.min(startIndex + visibleItems.value, flatNodes.length)
  return flatNodes.slice(startIndex, endIndex)
})

// 处理滚动事件
const handleScroll = (event: Event) => {
  const target = event.target as HTMLElement
  scrollTop.value = target.scrollTop
}

const emit = defineEmits<{
  (e: 'node-click', node: NodeData): void
  (e: 'node-context-menu', event: { node: NodeData; x: number; y: number }): void
  (e: 'toggle-expand', event: { node: NodeData; expanded: boolean }): void
  (e: 'node-operation', event: { type: 'create' | 'update' | 'delete'; node: NodeData }): void
}>()

const store = useZkStore()

// 右键菜单状态
const contextMenu = ref({
  show: false,
  x: 0,
  y: 0,
  node: null as NodeData | null
})

// 展开/折叠节点
// 初始化节点展开状态
const initNodeExpanded = (nodes: NodeData[]) => {
  nodes.forEach(node => {
    if (node.expanded === undefined) {
      node.expanded = false
    }
    if (node.children) {
      initNodeExpanded(node.children)
    }
  })
}

// 监听数据变化，初始化节点状态
watch(() => props.data, (newData: NodeData[]) => {
  initNodeExpanded(newData)
}, { immediate: true, deep: true })

const toggleExpand = (event: MouseEvent, node: NodeData) => {
  event.stopPropagation()
  if (!node.isLeaf) {
    node.expanded = !node.expanded
    emit('toggle-expand', { node, expanded: node.expanded })
    // 如果展开且没有子节点，初始化空数组
    if (node.expanded && !node.children) {
      node.children = []
    }
  }
}

// 处理节点点击
const handleNodeClick = (node: NodeData) => {
  clearSelectedNodes(props.data)
  node.selected = true
  store.setSelectedNode(node)
  emit('node-click', node)
  
  // 如果不是叶子节点，点击时也触发展开/折叠
  if (!node.isLeaf) {
    toggleExpand(new MouseEvent('click'), node)
  }
}

// 处理双击事件
const handleDoubleClick = (node: NodeData) => {
  // 双击时不需要额外处理展开/折叠，因为单击已经处理了
  // 这里可以添加其他双击特定的逻辑
}

// 清除所有节点的选中状态
const clearSelectedNodes = (nodes: NodeData[]) => {
  nodes.forEach(node => {
    node.selected = false
    if (node.children) {
      clearSelectedNodes(node.children)
    }
  })
}

// 显示右键菜单
const showContextMenu = (event: MouseEvent, node: NodeData) => {
  event.preventDefault()
  contextMenu.value = {
    show: true,
    x: event.clientX,
    y: event.clientY,
    node
  }
  emit('node-context-menu', { node, x: event.clientX, y: event.clientY })
  
  // 添加全局点击事件监听器
  setTimeout(() => {
    document.addEventListener('click', handleClickOutside)
  }, 0)
}

// 处理点击外部
const handleClickOutside = (event: MouseEvent) => {
  // 关闭菜单
  closeContextMenu()
  // 移除事件监听器
  document.removeEventListener('click', handleClickOutside)
}

// 关闭右键菜单
const closeContextMenu = () => {
  contextMenu.value.show = false
  contextMenu.value.node = null
}

// 在组件卸载时清理事件监听器
onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})

// 获取右键菜单项
const getContextMenuItems = (node: NodeData | null): ContextMenuItem[] => {
  if (!node) return []

  const store = useZkStore()
  const menuItems: ContextMenuItem[] = [
    {
      label: '节点信息',
      icon: 'info',
      action: () => {}, // 空函数满足类型要求
      items: [
        { label: `路径: ${node.path}`, action: () => {}, disabled: true },
        { label: `创建时间: ${formatTime(Number(node.createTime))}`, action: () => {}, disabled: true },
        { label: `修改时间: ${formatTime(Number(node.updateTime))}`, action: () => {}, disabled: true },
        { label: `版本: ${node.version}`, action: () => {}, disabled: true }
      ]
    },
    {
      label: '新建子节点',
      icon: 'plus',
      action: () => {
        store.setSelectedNode(node)
        emit('node-operation', { type: 'create', node })
      }
    },
    {
      label: '编辑节点',
      icon: 'edit',
      action: () => {
        store.setSelectedNode(node)
        emit('node-operation', { type: 'update', node })
      }
    },
    {
      label: '删除节点',
      icon: 'trash',
      action: () => {
        store.setSelectedNode(node)
        emit('node-operation', { type: 'delete', node })
      }
    },
    {
      label: '复制路径',
      icon: 'copy',
      action: () => navigator.clipboard.writeText(node.path)
    }
  ]

  return menuItems
}

// 格式化时间
const formatTime = (timestamp: number | undefined): string => {
  if (!timestamp) return '-'
  return new Date(timestamp).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}
</script>

<style scoped>
.tree-container {
  position: relative;
  overflow-y: auto;
  overflow-x: hidden;
}

.tree-list {
  @apply list-none p-0 m-0;
}

.tree-item {
  @apply relative;
}

.tree-item > div {
  @apply transition-colors duration-200;
}
</style>