<template>
  <DialogModal
    :show="show"
    title="节点信息"
    showCancel
    :showConfirm="false"
    cancelText="关闭"
    @update:show="$emit('update:show', $event)"
  >
    <div class="space-y-4">
      <div class="grid grid-cols-3 gap-4">
        <div class="font-medium">路径:</div>
        <div class="col-span-2 break-all">{{ node?.path || '-' }}</div>
        
        <div class="font-medium">创建时间:</div>
        <div class="col-span-2">{{ formatTime(node?.stat?.ctime) }}</div>
        
        <div class="font-medium">修改时间:</div>
        <div class="col-span-2">{{ formatTime(node?.stat?.mtime) }}</div>
        
        <div class="font-medium">版本:</div>
        <div class="col-span-2">{{ node?.stat?.version || 0 }}</div>
        
        <div class="font-medium">节点类型:</div>
        <div class="col-span-2">{{ getNodeType() }}</div>

        <div class="font-medium">子节点数:</div>
        <div class="col-span-2">{{ node?.stat?.numChildren || 0 }}</div>

        <div class="font-medium">数据长度:</div>
        <div class="col-span-2">{{ node?.stat?.dataLength || 0 }} 字节</div>
      </div>
    </div>
  </DialogModal>
</template>

<script setup lang="ts">
import { computed, watch } from 'vue'
import DialogModal from './DialogModal.vue'
import type { NodeData } from '../types/node'

const props = defineProps<{
  show: boolean
  node: NodeData | null
}>()

// 添加 watch 来调试节点数据
watch(() => props.node, (newNode) => {
  console.log('节点数据:', newNode)
  if (newNode?.stat) {
    console.log('节点统计信息:', newNode.stat)
  }
}, { immediate: true })

defineEmits<{
  (e: 'update:show', value: boolean): void
}>()

const formatTime = (timestamp: number | undefined): string => {
  if (!timestamp) return '-'
  try {
    // ZooKeeper 的时间戳是毫秒
    const date = new Date(timestamp)
    if (isNaN(date.getTime())) return '-'
    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    })
  } catch (error) {
    console.error('时间格式化错误:', error)
    return '-'
  }
}

const getNodeType = () => {
  if (!props.node) return '-'
  if (props.node.stat?.ephemeralOwner !== 0) {
    return '临时节点'
  }
  if (props.node.stat?.numChildren > 0) {
    return '目录节点'
  }
  if (props.node.stat?.numChildren === 0) {
    return '叶子节点'
  }
  return '-'
}

</script> 