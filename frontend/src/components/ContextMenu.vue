<template>
  <div v-if="show" 
       :style="{ top: `${y}px`, left: `${x}px` }"
       class="absolute z-50 min-w-[200px] py-2 bg-white rounded-lg shadow-lg border border-gray-200">
    <div v-for="item in items" 
         :key="item.label"
         @click="handleItemClick(item)"
         class="px-4 py-2 hover:bg-gray-100 cursor-pointer flex items-center gap-2"
         :class="{ 'opacity-50 cursor-not-allowed': item.disabled }">
      <i v-if="item.icon" :class="item.icon" class="w-4 h-4"></i>
      {{ item.label }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { ContextMenuItem } from '../types/node'

defineProps<{
  show: boolean
  x: number
  y: number
  items: ContextMenuItem[]
}>()

const emit = defineEmits<{
  (e: 'close'): void
}>()

const handleItemClick = (item: ContextMenuItem) => {
  if (!item.disabled) {
    item.action()
    emit('close')
  }
}
</script>

<style scoped>
.context-menu-enter-active,
.context-menu-leave-active {
  transition: opacity 0.2s ease;
}

.context-menu-enter-from,
.context-menu-leave-to {
  opacity: 0;
}
</style>