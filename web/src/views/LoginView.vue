<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { errorMessage } from '@/api/client'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const email = ref('')
const password = ref('')
const error = ref('')
const busy = ref(false)

async function submit() {
  error.value = ''
  busy.value = true
  try {
    await auth.login(email.value, password.value)
    const next = typeof route.query.next === 'string' ? route.query.next : '/groups'
    await router.push(next.startsWith('/') ? next : '/groups')
  } catch (e) {
    error.value = errorMessage(e, 'Sign-in failed')
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <section class="auth-card card">
    <h1>Sign in</h1>
    <form @submit.prevent="submit">
      <label>
        Email
        <input v-model="email" type="email" autocomplete="email" required />
      </label>
      <label>
        Password
        <input v-model="password" type="password" autocomplete="current-password" required />
      </label>
      <p v-if="error" class="error" role="alert">{{ error }}</p>
      <button type="submit" class="primary" :disabled="busy">
        {{ busy ? 'Signing in…' : 'Sign in' }}
      </button>
    </form>
    <p class="muted">No account yet? <RouterLink to="/register">Create one</RouterLink></p>
  </section>
</template>
