<template>
  <DialogModal
    :show="show"
    @update:show="$emit('update:show', $event)"
    :title="title"
    showCancel
    showConfirm
    cancelText="取消"
    confirmText="确定"
    :confirmButtonClass="confirmButtonClass"
    @confirm="handleConfirm"
  >
    <div class="space-y-4">
      <!-- 路径输入 -->
      <div v-if="mode === 'create'">
        <label class="block text-sm font-medium text-gray-700 mb-1">节点路径</label>
        <input
          v-model="formData.path"
          type="text"
          class="w-full p-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
          :placeholder="pathPlaceholder"
        />
      </div>

      <!-- 数据输入 -->
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">节点数据</label>
        <textarea
          v-model="formData.data"
          rows="4"
          class="w-full p-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
          placeholder="输入节点数据"
        ></textarea>
      </div>

      <!-- 节点类型选择 -->
      <div v-if="mode === 'create'">
        <label class="block text-sm font-medium text-gray-700 mb-1">节点类型</label>
        <select
          v-model="formData.mode"
          class="w-full p-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
        >
          <option value="PERSISTENT">持久节点</option>
          <option value="PERSISTENT_SEQUENTIAL">持久顺序节点</option>
          <option value="EPHEMERAL">临时节点</option>
          <option value="EPHEMERAL_SEQUENTIAL">临时顺序节点</option>
        </select>
      </div>
    </div>
  </DialogModal>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import DialogModal from './DialogModal.vue'
import type { NodeData } from '../types/node'

const props = defineProps<{
  show: boolean
  mode: 'create' | 'update'
  node?: NodeData | null
}>()

const emit = defineEmits<{
  (e: 'update:show', value: boolean): void
  (e: 'submit', data: { path: string; data: string; mode?: string }): void
}>()

const formData = ref({
  path: '',
  data: '',
  mode: 'PERSISTENT'
})

// 监听 props 变化，更新表单数据
watch(() => props.show, (newShow) => {
  if (newShow) {
    // 打开对话框时初始化表单数据
    formData.value = {
      path: props.mode === 'update' && props.node ? props.node.path : '',
      data: props.mode === 'update' && props.node ? props.node.data || '' : '',
      mode: 'PERSISTENT'
    }
  }
})

const title = computed(() => props.mode === 'create' ? '创建节点' : '修改节点')

const confirmButtonClass = computed(() => 
  props.mode === 'create' 
    ? 'bg-green-500 text-white hover:bg-green-600 focus:ring-green-500'
    : 'bg-yellow-500 text-white hover:bg-yellow-600 focus:ring-yellow-500'
)

const pathPlaceholder = computed(() => {
  if (props.node) {
    return '在 ' + props.node.path + ' 下创建子节点'
  }
  return '输入节点完整路径'
})

const handleConfirm = () => {
  emit('submit', {
    path: formData.value.path,
    data: formData.value.data,
    mode: props.mode === 'create' ? formData.value.mode : undefined
  })
}
</script> 