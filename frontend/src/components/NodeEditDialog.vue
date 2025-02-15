<template>
  <DialogModal
    :show="show"
    :title="mode === 'update' ? '编辑节点' : '新建节点'"
    showCancel
    showConfirm
    cancelText="取消"
    confirmText="保存"
    @update:show="$emit('update:show', $event)"
    @confirm="handleSubmit"
  >
    <div class="space-y-4">
      <!-- 节点路径显示 -->
      <div class="form-group">
        <label class="block text-sm font-medium text-gray-700">节点路径</label>
        <div class="mt-1 p-2 bg-gray-50 rounded-md text-gray-600 font-mono text-sm">
          {{ getDisplayPath() }}
        </div>
      </div>

      <!-- 节点名称输入 -->
      <div class="form-group" v-if="mode === 'create'">
        <label class="block text-sm font-medium text-gray-700">
          节点名称 <span class="text-red-500">*</span>
        </label>
        <input
          v-model="formData.name"
          type="text"
          class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500"
          :placeholder="'请输入节点名称'"
          :class="{ 'border-red-500': error.name }"
        >
        <p v-if="error.name" class="mt-1 text-sm text-red-600">{{ error.name }}</p>
      </div>

      <!-- 节点内容输入 -->
      <div class="form-group">
        <label class="block text-sm font-medium text-gray-700">节点内容</label>
        <textarea
          v-model="formData.data"
          rows="6"
          class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500"
          :placeholder="'请输入节点内容'"
        ></textarea>
      </div>
    </div>
  </DialogModal>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import DialogModal from './DialogModal.vue'
import type { NodeData } from '../types/node'
import { ElMessage } from 'element-plus'

const props = defineProps<{
  show: boolean
  mode: 'create' | 'update'
  node?: NodeData | null
  parentPath?: string
}>()

const emit = defineEmits<{
  (e: 'update:show', value: boolean): void
  (e: 'submit', data: { path: string; data: string }): void
}>()

const formData = ref({
  name: '',
  data: ''
})

const error = ref({
  name: ''
})

// 监听显示状态和节点数据变化
watch(
  [() => props.show, () => props.node],
  ([newShow, newNode]) => {
    if (newShow) {
      if (props.mode === 'update' && newNode) {
        // 编辑模式：填充现有数据
        formData.value.data = newNode.data || ''
      } else {
        // 创建模式：清空表单
        formData.value.name = ''
        formData.value.data = ''
      }
      error.value.name = ''
    }
  },
  { immediate: true }
)

// 获取显示路径
const getDisplayPath = () => {
  if (props.mode === 'update' && props.node) {
    return props.node.path
  } else if (props.mode === 'create') {
    const basePath = props.parentPath || '/'
    return basePath === '/' ? `/${formData.value.name}` : `${basePath}/${formData.value.name}`
  }
  return ''
}

// 表单验证
const validateForm = () => {
  error.value.name = ''

  if (props.mode === 'create') {
    if (!formData.value.name) {
      error.value.name = '请输入节点名称'
      return false
    }

    if (/[\\/:*?"<>|]/.test(formData.value.name)) {
      error.value.name = '节点名称包含非法字符'
      return false
    }
  }

  return true
}

// 提交表单
const handleSubmit = () => {
  if (!validateForm()) {
    return
  }

  try {
    const submitData = {
      path: getDisplayPath(),
      data: formData.value.data
    }
    emit('submit', submitData)
  } catch (err) {
    console.error('提交表单失败:', err)
    ElMessage.error('提交失败')
  }
}
</script>

<style scoped>
.form-group {
  margin-bottom: 1rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 500;
}

.error-message {
  color: #ff4d4f;
  font-size: 0.875rem;
  margin-top: 0.25rem;
}
</style> 