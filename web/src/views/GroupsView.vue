<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { groupApi } from '@/api'
import { ApiError } from '@/api/client'
import type { GroupSummary } from '@/api/types'
import EmptyState from '@/components/EmptyState.vue'
import UpcomingRides from '@/components/UpcomingRides.vue'

const router = useRouter()

const groups = ref<GroupSummary[]>([])
const loading = ref(true)
const loadError = ref('')

const newName = ref('')
const newDescription = ref('')
const inviteCode = ref('')
const createError = ref('')
const joinError = ref('')
const busy = ref(false)

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    groups.value = await groupApi.list()
  } catch (e) {
    loadError.value = e instanceof ApiError ? e.message : 'Could not load your groups'
  } finally {
    loading.value = false
  }
}

async function create() {
  createError.value = ''
  busy.value = true
  try {
    const timezone = Intl.DateTimeFormat().resolvedOptions().timeZone
    const group = await groupApi.create(newName.value, newDescription.value || undefined, timezone)
    await router.push(`/groups/${group.id}`)
  } catch (e) {
    createError.value = e instanceof ApiError ? e.message : 'Could not create the group'
  } finally {
    busy.value = false
  }
}

async function join() {
  joinError.value = ''
  busy.value = true
  try {
    const group = await groupApi.join(inviteCode.value)
    await router.push(`/groups/${group.id}`)
  } catch (e) {
    joinError.value =
      e instanceof ApiError && e.status === 404
        ? 'No group uses that invite code'
        : 'Could not join the group'
  } finally {
    busy.value = false
  }
}

const roleLabel: Record<string, string> = { OWNER: 'Owner', ADMIN: 'Admin', MEMBER: 'Member' }

onMounted(load)
</script>

<template>
  <section class="stack">
    <UpcomingRides />

    <h1>Your carpool groups</h1>

    <p v-if="loading" class="muted">Loading…</p>
    <p v-else-if="loadError" class="error">{{ loadError }}</p>
    <EmptyState v-else-if="!groups.length" title="You are not in a group yet">
      Create one for your office or school run, or join one with an invite code.
    </EmptyState>
    <ul v-else class="group-list">
      <li v-for="group in groups" :key="group.id">
        <RouterLink :to="`/groups/${group.id}`" class="card group-card">
          <span class="name">{{ group.name }}</span>
          <span v-if="group.description" class="muted">{{ group.description }}</span>
          <span class="row muted">
            <span class="badge neutral">{{ roleLabel[group.myRole] }}</span>
            {{ group.memberCount }} {{ group.memberCount === 1 ? 'member' : 'members' }}
          </span>
        </RouterLink>
      </li>
    </ul>

    <div class="forms">
      <form class="card" @submit.prevent="create">
        <h2>Start a group</h2>
        <label>
          Name
          <input v-model="newName" maxlength="80" placeholder="Elm St school run" required />
        </label>
        <label>
          Description <span class="muted">(optional)</span>
          <input v-model="newDescription" maxlength="500" placeholder="Weekday mornings" />
        </label>
        <p v-if="createError" class="error">{{ createError }}</p>
        <button type="submit" class="primary" :disabled="busy">Create group</button>
      </form>

      <form class="card" @submit.prevent="join">
        <h2>Join with a code</h2>
        <label>
          Invite code
          <input
            v-model="inviteCode"
            placeholder="ABCD-EFGH"
            autocapitalize="characters"
            required
          />
        </label>
        <p v-if="joinError" class="error">{{ joinError }}</p>
        <button type="submit" :disabled="busy">Join group</button>
      </form>
    </div>
  </section>
</template>

<style scoped>
.group-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: grid;
  gap: 0.75rem;
  grid-template-columns: repeat(auto-fill, minmax(16rem, 1fr));
}

.group-card {
  display: grid;
  gap: 0.35rem;
  color: inherit;
  text-decoration: none;
}

.group-card:hover {
  border-color: var(--accent);
}

.name {
  font-weight: 600;
  font-size: 1.05rem;
}

.forms {
  display: grid;
  gap: 1rem;
  grid-template-columns: repeat(auto-fit, minmax(18rem, 1fr));
}
</style>
