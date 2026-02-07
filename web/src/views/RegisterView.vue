<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'

import { ApiError } from '@/api/client'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()

const email = ref('')
const displayName = ref('')
const password = ref('')
const error = ref('')
const fields = ref<Record<string, string>>({})
const busy = ref(false)

async function submit() {
  error.value = ''
  fields.value = {}
  busy.value = true
  try {
    await auth.register(email.value, displayName.value, password.value)
    await router.push('/groups')
  } catch (e) {
    if (e instanceof ApiError) {
      error.value = e.message
      fields.value = e.fields
    } else {
      error.value = 'Registration failed'
    }
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <section class="auth-card card">
    <h1>Create an account</h1>
    <form @submit.prevent="submit">
      <label>
        Your name
        <input v-model="displayName" autocomplete="name" maxlength="80" required />
        <small v-if="fields.displayName" class="error">{{ fields.displayName }}</small>
      </label>
      <label>
        Email
        <input v-model="email" type="email" autocomplete="email" required />
        <small v-if="fields.email" class="error">{{ fields.email }}</small>
      </label>
      <label>
        Password
        <input
          v-model="password"
          type="password"
          autocomplete="new-password"
          minlength="8"
          required
        />
        <small v-if="fields.password" class="error">{{ fields.password }}</small>
        <small v-else class="muted">At least 8 characters.</small>
      </label>
      <p v-if="error && !Object.keys(fields).length" class="error" role="alert">{{ error }}</p>
      <button type="submit" class="primary" :disabled="busy">
        {{ busy ? 'Creating…' : 'Create account' }}
      </button>
    </form>
    <p class="muted">Already registered? <RouterLink to="/login">Sign in</RouterLink></p>
  </section>
</template>
