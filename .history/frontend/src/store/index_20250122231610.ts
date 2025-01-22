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
      // TODO: Implement cluster fetching
    },
    async fetchNodeTree(cluster: string) {
      // TODO: Implement node tree fetching
    }
  }
})
