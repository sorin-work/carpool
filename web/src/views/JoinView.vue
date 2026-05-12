<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { groupApi } from '@/api'
import { ApiError } from '@/api/client'

const route = useRoute()
const router = useRouter()
const error = ref('')

onMounted(async () => {
  try {
    const group = await groupApi.join(String(route.params.code))
    await router.replace(`/groups/${group.id}`)
  } catch (e) {
    error.value =
      e instanceof ApiError && e.status === 404
        ? 'This invite link is no longer valid. Ask the group for a new one.'
        : 'Could not join the group'
  }
})
</script>

<template>
  <section class="card auth-card">
    <p v-if="!error">Joining the group…</p>
    <template v-else>
      <p class="error">{{ error }}</p>
      <RouterLink to="/groups">Back to your groups</RouterLink>
    </template>
  </section>
</template>
