<template>
  <div class="p-4">
    <h1 class="text-2xl font-bold mb-4">Zookeeper 集群管理</h1>
    
    <div v-if="store.loading" class="text-center">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary mx-auto"></div>
    </div>

    <div v-else>
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        <div 
          v-for="cluster in store.clusters"
          :key="cluster"
          class="p-4 border rounded-lg hover:shadow-lg transition-shadow cursor-pointer"
          @click="selectCluster(cluster)"
        >
          <h2 class="text-lg font-semibold">{{ cluster }}</h2>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useZkStore } from '../store'
import { useRouter } from 'vue-router'

const router = useRouter()

const store = useZkStore()

const selectCluster = (cluster: string) => {
  router.push({ name: 'Cluster', params: { cluster } })
}
</script>