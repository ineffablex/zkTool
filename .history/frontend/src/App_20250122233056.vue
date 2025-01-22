<template>
  <RouterView />
</template>

<script setup lang="ts">
import { RouterView } from 'vue-router'
import { useZkStore } from './store'

const store = useZkStore()
store.fetchClusters()
</script>

<style>
@tailwind base;
@tailwind components;
@tailwind utilities;
</style>
