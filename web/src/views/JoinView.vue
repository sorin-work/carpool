<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { groupApi } from '@/api'
import { ApiError, errorMessage } from '@/api/client'

const route = useRoute()
const router = useRouter()
const code = String(route.params.code)

const preview = ref<Awaited<ReturnType<typeof groupApi.preview>> | null>(null)
const error = ref('')
const busy = ref(false)

onMounted(async () => {
  try {
    preview.value = await groupApi.preview(code)
  } catch (e) {
    error.value =
      e instanceof ApiError && e.status === 404
        ? 'This invite link is no longer valid. Ask the group for a new one.'
        : errorMessage(e, 'Could not open the invite')
  }
})

async function join() {
  busy.value = true
  try {
    const group = await groupApi.join(code)
    await router.replace(`/groups/${group.id}`)
  } catch (e) {
    error.value = errorMessage(e, 'Could not join the group')
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <section class="card auth-card stack">
    <template v-if="preview">
      <h1>{{ preview.name }}</h1>
      <p v-if="preview.description" class="muted">{{ preview.description }}</p>
      <p>{{ preview.memberCount }} {{ preview.memberCount === 1 ? 'member' : 'members' }}</p>
      <p v-if="preview.alreadyMember" class="muted">You are already in this group.</p>
      <button type="button" class="primary" :disabled="busy" @click="join">
        {{ preview.alreadyMember ? 'Open the group' : 'Join this group' }}
      </button>
    </template>
    <p v-else-if="!error" class="muted">Loading…</p>
    <template v-if="error">
      <p class="error">{{ error }}</p>
      <RouterLink to="/groups">Back to your groups</RouterLink>
    </template>
  </section>
</template>
