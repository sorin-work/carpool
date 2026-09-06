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
      meta: { public: true, title: 'Sign in' },
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/RegisterView.vue'),
      meta: { public: true, title: 'Create an account' },
    },
    {
      path: '/groups',
      name: 'groups',
      meta: { title: 'Your groups' },
      component: () => import('@/views/GroupsView.vue'),
    },
    {
      path: '/join/:code',
      name: 'join',
      meta: { title: 'Join a group' },
      component: () => import('@/views/JoinView.vue'),
    },
    {
      path: '/profile',
      name: 'profile',
      meta: { title: 'Profile' },
      component: () => import('@/views/ProfileView.vue'),
    },
    {
      path: '/notifications',
      name: 'notifications',
      meta: { title: 'Notifications' },
      component: () => import('@/views/NotificationsView.vue'),
    },
    {
      path: '/groups/:groupId(\\d+)',
      component: () => import('@/views/GroupView.vue'),
      children: [
        {
          path: '',
          name: 'group-schedule',
          component: () => import('@/views/group/SchedulePanel.vue'),
        },
        {
          path: 'rides/:rideId(\\d+)',
          name: 'ride',
          component: () => import('@/views/RideView.vue'),
        },
        {
          path: 'rotation',
          name: 'group-rotation',
          component: () => import('@/views/group/RotationPanel.vue'),
        },
        {
          path: 'swaps',
          name: 'group-swaps',
          component: () => import('@/views/group/SwapsPanel.vue'),
        },
        {
          path: 'stats',
          name: 'group-stats',
          component: () => import('@/views/group/StatsPanel.vue'),
        },
        {
          path: 'costs',
          name: 'group-costs',
          component: () => import('@/views/group/CostsPanel.vue'),
        },
        {
          path: 'settings',
          name: 'group-settings',
          component: () => import('@/views/group/SettingsPanel.vue'),
        },
        {
          path: 'trips',
          name: 'group-trips',
          component: () => import('@/views/group/TripsPanel.vue'),
        },
        {
          path: 'members',
          name: 'group-members',
          component: () => import('@/views/group/MembersPanel.vue'),
        },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('@/views/NotFoundView.vue'),
      meta: { public: true, title: 'Page not found' },
    },
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

router.afterEach((to) => {
  const title = typeof to.meta.title === 'string' ? to.meta.title : null
  document.title = title ? `${title} · Carpool` : 'Carpool'
})

export default router
