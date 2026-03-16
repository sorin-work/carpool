<script setup lang="ts">
import { ref } from 'vue'

import { authApi } from '@/api'
import { ApiError } from '@/api/client'
import MemberAvatar from '@/components/MemberAvatar.vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()

const displayName = ref(auth.user?.displayName ?? '')
const error = ref('')
const saved = ref(false)

async function save() {
  error.value = ''
  saved.value = false
  try {
    auth.user = await authApi.updateMe(displayName.value)
    saved.value = true
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : 'Could not save'
  }
}
</script>

<template>
  <section v-if="auth.user" class="stack profile">
    <div class="row">
      <MemberAvatar :name="auth.user.displayName" :size="48" />
      <div>
        <h1>{{ auth.user.displayName }}</h1>
        <p class="muted">{{ auth.user.email }}</p>
      </div>
    </div>
    <form class="card" @submit.prevent="save">
      <label>
        Name shown to your groups
        <input v-model="displayName" maxlength="80" required />
      </label>
      <p v-if="error" class="error">{{ error }}</p>
      <div class="row">
        <button type="submit" class="primary">Save</button>
        <span v-if="saved" class="muted">Saved</span>
      </div>
    </form>
  </section>
</template>

<style scoped>
.profile {
  max-width: 32rem;
}

h1,
p {
  margin: 0;
}
</style>
