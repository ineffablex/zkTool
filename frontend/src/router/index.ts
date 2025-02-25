import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import ClusterView from '../views/ClusterView.vue'
import DemoView from '../views/DemoView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView
    },
    {
      path: '/cluster/:cluster',
      name: 'cluster',
      component: ClusterView
    },
    {
      path: '/demo',
      name: 'demo',
      component: DemoView
    }
  ]
})

export default router
