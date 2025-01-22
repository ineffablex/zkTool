<template>
  <ul>
    <li v-for="node in data" :key="node.path">
      <div class="flex items-center py-2">
        <button 
          v-if="node.children"
          @click="toggleExpand(node)"
          class="mr-2 w-4 h-4 flex items-center justify-center"
        >
          {{ node.expanded ? '-' : '+' }}
        </button>
        <span>{{ node.name }}</span>
      </div>

      <div v-if="node.expanded && node.children" class="pl-4">
        <TreeView :data="node.children" />
      </div>
    </li>
  </ul>
</template>

<script setup lang="ts">
import { ref } from 'vue'

defineProps<{
  data: any[]
}>()

const toggleExpand = (node: any) => {
  node.expanded = !node.expanded
}
</script>