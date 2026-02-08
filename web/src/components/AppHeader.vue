<script setup lang="ts">
import { useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()

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
        <span class="spacer" />
        <span class="muted">{{ auth.user?.displayName }}</span>
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

nav a.router-link-active {
  color: var(--accent);
}
</style>
