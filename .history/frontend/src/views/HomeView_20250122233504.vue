<template>
  <div class="p-4 max-w-4xl mx-auto">
    <h1 class="text-2xl font-bold mb-6">Zookeeper 集群管理</h1>

    <div class="bg-white p-6 rounded-lg shadow-md">
      <div class="flex gap-4 mb-6">
        <input
          v-model="clusterAddress"
          type="text"
          placeholder="输入Zookeeper集群地址 (例如: localhost:2181)"
          class="flex-1 p-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
        <button
          @click="connectCluster"
          :disabled="isConnecting"
          class="px-6 py-2 bg-blue-500 text-white rounded-md hover:bg-blue-600 disabled:bg-gray-400 disabled:cursor-not-allowed"
        >
          {{ isConnecting ? '连接中...' : '开始连接' }}
        </button>
      </div>

      <div v-if="error" class="text-red-500 mb-4">{{ error }}</div>

      <div v-if="nodes.length > 0" class="overflow-x-auto">
        <table class="min-w-full bg-white">
          <thead class="bg-gray-50">
            <tr>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">节点名称</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">状态</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">IP地址</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">连接时间</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-200">
            <tr v-for="node in nodes" :key="node.name">
              <td class="px-6 py-4 whitespace-nowrap">{{ node.name }}</td>
              <td class="px-6 py-4 whitespace-nowrap">
                <span :class="statusClass(node.status)">{{ node.status }}</span>
              </td>
              <td class="px-6 py-4 whitespace-nowrap">{{ node.ip }}</td>
              <td class="px-6 py-4 whitespace-nowrap">{{ node.connectionTime }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useZkStore } from '../store'

const clusterAddress = ref('')
const isConnecting = ref(false)
const error = ref('')
const nodes = ref<any[]>([])

const store = useZkStore()

const statusClass = (status: string) => {
  return {
    'px-2 py-1 rounded-full text-sm': true,
    'bg-green-100 text-green-800': status === 'connected',
    'bg-red-100 text-red-800': status === 'disconnected',
    'bg-yellow-100 text-yellow-800': status === 'pending'
  }
}

const connectCluster = async () => {
  if (!clusterAddress.value) {
    error.value = '请输入集群地址'
    return
  }

  isConnecting.value = true
  error.value = ''

  try {
    await store.connect(clusterAddress.value)
    nodes.value = await store.fetchNodes()
  } catch (err) {
    error.value = '连接集群失败: ' + (err as Error).message
    nodes.value = []
  } finally {
    isConnecting.value = false
  }
}
</script>