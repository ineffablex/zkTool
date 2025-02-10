<template>
  <Transition name="modal">
    <div v-if="show" class="fixed inset-0 z-50 overflow-y-auto">
      <!-- 背景遮罩 -->
      <div class="fixed inset-0 bg-black bg-opacity-50 transition-opacity" @click="handleClose"></div>

      <!-- 对话框 -->
      <div class="flex min-h-full items-center justify-center p-4">
        <div class="relative w-full max-w-md transform overflow-hidden rounded-lg bg-white shadow-xl transition-all">
          <!-- 标题 -->
          <div class="border-b px-4 py-3">
            <h3 class="text-lg font-medium">{{ title }}</h3>
          </div>

          <!-- 内容 -->
          <div class="px-4 py-3">
            <slot></slot>
          </div>

          <!-- 按钮 -->
          <div class="border-t px-4 py-3 flex justify-end space-x-2">
            <button
              v-if="showCancel"
              @click="handleClose"
              class="px-4 py-2 bg-gray-100 text-gray-700 rounded-md hover:bg-gray-200 focus:outline-none focus:ring-2 focus:ring-gray-500"
            >
              {{ cancelText }}
            </button>
            <button
              v-if="showConfirm"
              @click="handleConfirm"
              :class="[
                'px-4 py-2 rounded-md focus:outline-none focus:ring-2',
                confirmButtonClass || 'bg-blue-500 text-white hover:bg-blue-600 focus:ring-blue-500'
              ]"
            >
              {{ confirmText }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup lang="ts">
defineProps<{
  show: boolean
  title: string
  showCancel?: boolean
  showConfirm?: boolean
  cancelText?: string
  confirmText?: string
  confirmButtonClass?: string
}>()

const emit = defineEmits<{
  (e: 'update:show', value: boolean): void
  (e: 'confirm'): void
  (e: 'cancel'): void
}>()

const handleClose = () => {
  emit('update:show', false)
  emit('cancel')
}

const handleConfirm = () => {
  emit('confirm')
}
</script>

<style scoped>
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.3s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}
</style> 