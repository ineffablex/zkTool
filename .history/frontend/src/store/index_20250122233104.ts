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
    async fetchNodeTree(cluster: string) {
      // TODO: Implement node tree fetching
    }
  }
})
