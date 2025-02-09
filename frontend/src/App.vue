<template>
  <header class="bg-white shadow">
    <nav class="container mx-auto px-4 py-4">
      <ul class="flex space-x-4">
        <li>
          <RouterLink 
            to="/" 
            class="text-gray-600 hover:text-gray-900"
            active-class="text-blue-600 font-medium"
          >
            首页
          </RouterLink>
        </li>
        <li>
          <RouterLink 
            to="/demo" 
            class="text-gray-600 hover:text-gray-900"
            active-class="text-blue-600 font-medium"
          >
            组件演示
          </RouterLink>
        </li>
      </ul>
    </nav>
  </header>

  <main class="container mx-auto mt-4">
    <RouterView />
  </main>
</template>

<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'
</script>

<style>
@import '@/assets/main.css';
</style>
