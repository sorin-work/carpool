<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { watch } from 'vue'
import { useRoute } from 'vue-router'

import { useGroupStore } from '@/stores/group'

const route = useRoute()
const store = useGroupStore()
const { group, loading, error } = storeToRefs(store)

watch(
  () => group.value?.name,
  (name) => {
    if (name) document.title = `${name} · Carpool`
  },
)

watch(
  () => Number(route.params.groupId),
  (id) => {
    if (id && id !== group.value?.id) {
      store.load(id)
    }
  },
  { immediate: true },
)
</script>

<template>
  <p v-if="loading && !group" class="muted">Loading…</p>
  <p v-else-if="error" class="error">{{ error }}</p>
  <section v-else-if="group" class="stack">
    <header>
      <h1>{{ group.name }}</h1>
      <p v-if="group.description" class="muted">{{ group.description }}</p>
    </header>
    <nav class="tabs">
      <RouterLink :to="`/groups/${group.id}`">Schedule</RouterLink>
      <RouterLink :to="`/groups/${group.id}/rotation`">Rotation</RouterLink>
      <RouterLink :to="`/groups/${group.id}/swaps`">Swaps</RouterLink>
      <RouterLink :to="`/groups/${group.id}/stats`">Stats</RouterLink>
      <RouterLink :to="`/groups/${group.id}/costs`">Costs</RouterLink>
      <RouterLink :to="`/groups/${group.id}/trips`">Trips</RouterLink>
      <RouterLink :to="`/groups/${group.id}/members`">Members</RouterLink>
      <RouterLink :to="`/groups/${group.id}/settings`">Settings</RouterLink>
    </nav>
    <RouterView />
  </section>
</template>

<style scoped>
header h1 {
  overflow-wrap: anywhere;
}

header p {
  margin: -0.4rem 0 0;
}

.tabs {
  display: flex;
  gap: 0.25rem;
  border-bottom: 1px solid var(--border);
  overflow-x: auto;
}

.tabs a {
  padding: 0.5rem 0.85rem;
  color: var(--muted);
  text-decoration: none;
  font-weight: 500;
  border-bottom: 2px solid transparent;
  white-space: nowrap;
}

.tabs a.router-link-exact-active {
  color: var(--accent);
  border-bottom-color: var(--accent);
}
</style>
