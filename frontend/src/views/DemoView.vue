<template>
  <div class="p-8">
    <h1 class="text-2xl font-bold mb-6">组件展示 & 测试</h1>

    <!-- TreeView 组件测试 -->
    <section class="mb-8">
      <h2 class="text-xl font-semibold mb-4">TreeView 组件</h2>
      
      <div class="grid grid-cols-2 gap-8">
        <!-- 基础树形结构 -->
        <div class="border rounded-lg p-4">
          <h3 class="text-lg font-medium mb-3">基础树形结构</h3>
          <TreeView 
            :data="basicTreeData"
            :item-height="32"
            :visible-items="10"
            @node-click="handleNodeClick"
            @toggle-expand="handleToggleExpand"
            @node-context-menu="handleContextMenu"
          />
        </div>

        <!-- 带状态的树形结构 -->
        <div class="border rounded-lg p-4">
          <h3 class="text-lg font-medium mb-3">带状态的节点</h3>
          <TreeView 
            :data="statusTreeData"
            :item-height="32"
            :visible-items="10"
            @node-click="handleNodeClick"
            @toggle-expand="handleToggleExpand"
            @node-context-menu="handleContextMenu"
          />
        </div>

        <!-- 大数据树形结构 -->
        <div class="border rounded-lg p-4">
          <h3 class="text-lg font-medium mb-3">大数据结构</h3>
          <TreeView 
            :data="largeTreeData"
            :item-height="32"
            :visible-items="15"
            @node-click="handleNodeClick"
            @toggle-expand="handleToggleExpand"
            @node-context-menu="handleContextMenu"
          />
        </div>

        <!-- 特殊状态测试 -->
        <div class="border rounded-lg p-4">
          <h3 class="text-lg font-medium mb-3">特殊状态</h3>
          <TreeView 
            :data="specialTreeData"
            :item-height="32"
            :visible-items="10"
            @node-click="handleNodeClick"
            @toggle-expand="handleToggleExpand"
            @node-context-menu="handleContextMenu"
          />
        </div>
      </div>
    </section>

    <!-- ContextMenu 组件测试 -->
    <section class="mb-8">
      <h2 class="text-xl font-semibold mb-4">ContextMenu 组件</h2>
      
      <div class="grid grid-cols-2 gap-8">
        <!-- 基础菜单 -->
        <div class="border rounded-lg p-4">
          <h3 class="text-lg font-medium mb-3">基础菜单</h3>
          <div class="h-32 bg-gray-100 flex items-center justify-center"
               @contextmenu.prevent="showBasicMenu">
            右键点击此区域
          </div>
          <ContextMenu
            v-model:show="basicMenu.show"
            :x="basicMenu.x"
            :y="basicMenu.y"
            :items="basicMenuItems" />
        </div>

        <!-- 禁用状态菜单 -->
        <div class="border rounded-lg p-4">
          <h3 class="text-lg font-medium mb-3">禁用状态</h3>
          <div class="h-32 bg-gray-100 flex items-center justify-center"
               @contextmenu.prevent="showDisabledMenu">
            右键点击此区域
          </div>
          <ContextMenu
            v-model:show="disabledMenu.show"
            :x="disabledMenu.x"
            :y="disabledMenu.y"
            :items="disabledMenuItems" />
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import TreeView from '../components/TreeView.vue'
import ContextMenu from '../components/ContextMenu.vue'
import type { NodeData } from '../types/node'

// TreeView 测试数据
const basicTreeData = ref<NodeData[]>([
  {
    path: '/root',
    name: 'root',
    expanded: false,
    selected: false,
    children: [
      {
        path: '/root/child1',
        name: 'child1',
        expanded: false,
        selected: false,
        isLeaf: true
      },
      {
        path: '/root/child2',
        name: 'child2',
        expanded: false,
        selected: false,
        children: [
          {
            path: '/root/child2/grandchild',
            name: 'grandchild',
            expanded: false,
            selected: false,
            isLeaf: true
          }
        ]
      }
    ]
  }
])

const statusTreeData = ref<NodeData[]>([
  {
    path: '/node1',
    name: 'Connected Node',
    status: 'connected',
    expanded: false,
    selected: false,
    children: [
      {
        path: '/node1/error',
        name: 'Error Node',
        status: 'disconnected',
        expanded: false,
        selected: false,
        isLeaf: true
      }
    ]
  },
  {
    path: '/node2',
    name: 'Warning Node',
    status: 'warning',
    expanded: false,
    selected: false,
    isLeaf: true
  }
])

// 生成大数据树形结构
const generateLargeTree = (depth: number = 3, breadth: number = 5, prefix: string = ''): NodeData[] => {
  const nodes: NodeData[] = []
  for (let i = 0; i < breadth; i++) {
    const path = `${prefix}/node${i}`
    const node: NodeData = {
      path,
      name: `Node ${path}`,
      expanded: false,
      selected: false
    }
    
    if (depth > 0) {
      node.children = generateLargeTree(depth - 1, breadth, path)
    } else {
      node.isLeaf = true
    }
    nodes.push(node)
  }
  return nodes
}

const largeTreeData = ref<NodeData[]>(generateLargeTree())

const specialTreeData = ref<NodeData[]>([
  {
    path: '/empty',
    name: 'Empty Folder',
    expanded: false,
    selected: false,
    children: []
  },
  {
    path: '/long',
    name: 'Very Long Node Name That Should Be Truncated With Ellipsis',
    expanded: false,
    selected: false,
    isLeaf: true
  }
])

// 事件处理
const handleNodeClick = (node: NodeData) => {
  console.log('Node clicked:', node.path)
}

const handleToggleExpand = ({ node, expanded }: { node: NodeData; expanded: boolean }) => {
  console.log('Node toggle:', node.path, expanded)
  // 这里可以模拟异步加载子节点
  if (expanded && node.children && node.children.length === 0) {
    setTimeout(() => {
      node.children = [
        {
          path: `${node.path}/async1`,
          name: 'Async Child 1',
          expanded: false,
          selected: false,
          isLeaf: true
        },
        {
          path: `${node.path}/async2`,
          name: 'Async Child 2',
          expanded: false,
          selected: false,
          isLeaf: true
        }
      ]
    }, 500)
  }
}

const handleContextMenu = ({ node, x, y }: { node: NodeData; x: number; y: number }) => {
  console.log('Context menu:', { node: node.path, x, y })
}

// ContextMenu 测试数据
const basicMenu = ref({
  show: false,
  x: 0,
  y: 0
})

const showBasicMenu = (e: MouseEvent) => {
  basicMenu.value = {
    show: true,
    x: e.clientX,
    y: e.clientY
  }
}

const basicMenuItems = [
  {
    label: '新建',
    icon: 'plus',
    action: () => alert('点击了新建')
  },
  {
    label: '编辑',
    icon: 'edit',
    action: () => alert('点击了编辑')
  },
  {
    label: '删除',
    icon: 'trash',
    action: () => alert('点击了删除')
  }
]

const disabledMenu = ref({
  show: false,
  x: 0,
  y: 0
})

const showDisabledMenu = (e: MouseEvent) => {
  disabledMenu.value = {
    show: true,
    x: e.clientX,
    y: e.clientY
  }
}

const disabledMenuItems = [
  {
    label: '新建',
    icon: 'plus',
    action: () => alert('点击了新建'),
    disabled: true
  },
  {
    label: '编辑',
    icon: 'edit',
    action: () => alert('点击了编辑')
  },
  {
    label: '删除',
    icon: 'trash',
    action: () => alert('点击了删除'),
    disabled: true
  }
]
</script>