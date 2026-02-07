import { createRouter, createWebHistory } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', redirect: '/groups' },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { public: true },
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/RegisterView.vue'),
      meta: { public: true },
    },
    {
      path: '/groups',
      name: 'groups',
      component: () => import('@/views/GroupsView.vue'),
    },
    { path: '/:pathMatch(.*)*', redirect: '/groups' },
  ],
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (to.meta.public) {
    return auth.isSignedIn && (to.name === 'login' || to.name === 'register') ? '/groups' : true
  }
  if (!auth.isSignedIn) {
    return { name: 'login', query: to.fullPath !== '/groups' ? { next: to.fullPath } : {} }
  }
  await auth.restore()
  return auth.isSignedIn ? true : { name: 'login' }
})

export default router
