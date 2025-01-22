import { defineStore } from 'pinia'

export const useZkStore = defineStore('zk', {
  state: () => ({
    currentCluster: null as string | null,
    clusters: [] as string[],
    nodeTree: [] as any[],
    loading: false
  }),
  actions: {
    async fetchClusters() {
      this.loading = true
      try {
        const response = await fetch('/api/zk/clusters')
        const data = await response.json()
        this.clusters = data
      } catch (error) {
        console.error('Failed to fetch clusters:', error)
      } finally {
        this.loading = false
      }
    },

    async connect(address: string) {
      this.loading = true
      try {
        const response = await fetch(`/zk/connect?address=${encodeURIComponent(address)}`)
        if (!response.ok) {
          throw new Error('连接失败')
        }
        this.currentCluster = address
      } catch (error) {
        console.error('连接失败:', error)
        throw error
      } finally {
        this.loading = false
      }
    },
    async fetchNodes() {
      if (!this.currentCluster) {
        throw new Error('请先连接集群')
      }
      
      this.loading = true
      try {
        const response = await fetch(`/api/zk/nodes?cluster=${encodeURIComponent(this.currentCluster)}`)
        if (!response.ok) {
          throw new Error('获取节点信息失败')
        }
        return await response.json()
      } catch (error) {
        console.error('获取节点信息失败:', error)
        throw error
      } finally {
        this.loading = false
      }
    }
  }
})
