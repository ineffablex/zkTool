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
      <div class="bg-white rounded-lg shadow p-4">
        <TreeView :data="store.nodeTree" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useZkStore } from '@/store'
import TreeView from '@/components/TreeView.vue'

const route = useRoute()
const store = useZkStore()
const cluster = ref(route.params.cluster as string)

onMounted(async () => {
  await store.fetchNodeTree(cluster.value)
})
</script>