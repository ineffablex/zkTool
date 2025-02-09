<template>
  <div class="p-4">
    <div class="flex items-center mb-4">
      <button 
        @click="$router.back()"
        class="mr-2 p-2 rounded-full hover:bg-gray-100"
      >
        &larr;
      </button>
      <h1 class="text-2xl font-bold">{{ cluster }}</h1>
    </div>

    <div v-if="store.loading" class="text-center">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary mx-auto"></div>
    </div>

    <div v-else>
      <div class="bg-white rounded-lg shadow p-4 h-[calc(100vh-8rem)]">
        <TreeView 
          :data="store.nodeTree"
          :item-height="32"
          :visible-items="20"
          @node-click="handleNodeClick"
          @toggle-expand="handleToggleExpand"
          @node-context-menu="handleContextMenu"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useZkStore } from '../store'
import TreeView from '@/components/TreeView.vue'
import type { NodeData } from '../types/node'

const route = useRoute()
const store = useZkStore()
const cluster = ref(route.params.cluster as string)

onMounted(async () => {
  await store.fetchNodeTree(cluster.value)
})

// 处理节点点击
const handleNodeClick = async (node: NodeData) => {
  // 如果需要，这里可以添加点击节点时的额外逻辑
  console.log('Node clicked:', node.path)
}

// 处理节点展开/折叠
const handleToggleExpand = async ({ node, expanded }: { node: NodeData; expanded: boolean }) => {
  try {
    if (expanded && (!node.children || node.children.length === 0)) {
      // 如果节点被展开且没有子节点数据，尝试获取子节点
      await store.fetchChildNodes(node.path)
    }
  } catch (error) {
    console.error('Failed to fetch child nodes:', error)
  }
}

// 处理右键菜单
const handleContextMenu = ({ node, x, y }: { node: NodeData; x: number; y: number }) => {
  // 如果需要，这里可以添加自定义的右键菜单处理逻辑
  console.log('Context menu:', { node: node.path, x, y })
}
</script>