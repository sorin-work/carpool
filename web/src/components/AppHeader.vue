<script setup lang="ts">
import { onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notifications'

const auth = useAuthStore()
const notifications = useNotificationStore()
const router = useRouter()
const route = useRoute()

let timer: ReturnType<typeof setInterval> | undefined

// Coming back to the tab is when people expect the badge to be current.
function onVisible() {
  if (document.visibilityState === 'visible' && auth.isSignedIn) notifications.refresh()
}

onMounted(() => {
  if (auth.isSignedIn) notifications.refresh()
  timer = setInterval(() => auth.isSignedIn && notifications.refresh(), 60_000)
  document.addEventListener('visibilitychange', onVisible)
})
onUnmounted(() => {
  clearInterval(timer)
  document.removeEventListener('visibilitychange', onVisible)
})
watch(
  () => route.fullPath,
  () => auth.isSignedIn && notifications.refresh(),
)

function signOut() {
  auth.logout()
  router.push({ name: 'login' })
}
</script>

<template>
  <header class="app-header">
    <div class="inner">
      <RouterLink to="/groups" class="brand">
        <img src="/favicon.svg" alt="" width="24" height="24" />
        Carpool
      </RouterLink>
      <nav v-if="auth.isSignedIn" class="row">
        <RouterLink to="/groups">Groups</RouterLink>
        <RouterLink to="/notifications">
          Notifications
          <span v-if="notifications.unread" class="badge count">{{ notifications.unread }}</span>
        </RouterLink>
        <span class="spacer" />
        <RouterLink to="/profile">{{ auth.user?.displayName ?? 'Profile' }}</RouterLink>
        <button class="link" type="button" @click="signOut">Sign out</button>
      </nav>
    </div>
  </header>
</template>

<style scoped>
.app-header {
  background: var(--surface);
  border-bottom: 1px solid var(--border);
}

.inner {
  max-width: 64rem;
  margin: 0 auto;
  padding: 0.6rem 1rem;
  display: flex;
  align-items: center;
  gap: 1.5rem;
}

@media (max-width: 40rem) {
  .inner {
    flex-wrap: wrap;
    gap: 0.5rem 1rem;
  }

  nav {
    gap: 0.75rem;
  }
}

.brand {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-weight: 700;
  color: var(--text);
  text-decoration: none;
}

nav {
  flex: 1;
  gap: 1rem;
}

nav a {
  color: var(--muted);
  text-decoration: none;
  font-weight: 500;
}

.count {
  background: var(--accent);
  color: #fff;
  margin-left: 0.2rem;
}

nav a.router-link-active {
  color: var(--accent);
}
</style>
