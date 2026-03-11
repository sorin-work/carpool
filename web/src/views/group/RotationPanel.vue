<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { computed, ref, watch } from 'vue'

import { rotationApi } from '@/api'
import { ApiError } from '@/api/client'
import type { RotationProposal } from '@/api/types'
import EmptyState from '@/components/EmptyState.vue'
import { useGroupStore } from '@/stores/group'
import { addDays, formatDay, formatTime, todayIn } from '@/utils/dates'

const store = useGroupStore()
const { group, isManager } = storeToRefs(store)

const from = ref(todayIn(group.value?.timezone ?? 'UTC'))
const days = ref(7)
const proposals = ref<RotationProposal[]>([])
const loading = ref(false)
const applied = ref(false)
const error = ref('')

const to = computed(() => addDays(from.value, days.value))
const unassigned = computed(() => proposals.value.filter((p) => !p.memberId).length)
const perDriver = computed(() => {
  const counts = new Map<string, number>()
  for (const p of proposals.value) {
    if (p.displayName) counts.set(p.displayName, (counts.get(p.displayName) ?? 0) + 1)
  }
  return [...counts.entries()].sort((a, b) => b[1] - a[1])
})

async function preview() {
  if (!group.value) return
  loading.value = true
  error.value = ''
  applied.value = false
  try {
    proposals.value = await rotationApi.preview(group.value.id, from.value, to.value)
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : 'Could not plan the rotation'
  } finally {
    loading.value = false
  }
}

async function apply() {
  if (!group.value) return
  error.value = ''
  try {
    await rotationApi.apply(group.value.id, from.value, to.value)
    applied.value = true
    proposals.value = []
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : 'Could not apply the rotation'
  }
}

watch([from, days], preview, { immediate: true })
</script>

<template>
  <div v-if="group" class="stack">
    <div class="card stack">
      <h2>Driver rotation</h2>
      <p class="muted">
        Rides without a driver go to whoever has driven least over the last eight weeks, skipping
        people who are away.
      </p>
      <div class="row">
        <label class="inline"> From <input v-model="from" type="date" /> </label>
        <label class="inline">
          For
          <select v-model.number="days">
            <option :value="7">1 week</option>
            <option :value="14">2 weeks</option>
            <option :value="28">4 weeks</option>
          </select>
        </label>
      </div>
    </div>

    <p v-if="error" class="error">{{ error }}</p>
    <p v-if="applied" class="badge">Drivers assigned and notified.</p>
    <p v-if="loading" class="muted">Planning…</p>
    <EmptyState v-else-if="!proposals.length && !applied" title="Every ride has a driver">
      Nothing to plan in this period.
    </EmptyState>

    <template v-else-if="proposals.length">
      <div class="row">
        <span v-for="[name, count] in perDriver" :key="name" class="badge neutral">
          {{ name }}: {{ count }}
        </span>
        <span v-if="unassigned" class="badge warning">{{ unassigned }} without a driver</span>
      </div>
      <table class="card">
        <thead>
          <tr>
            <th>Day</th>
            <th>Trip</th>
            <th>Driver</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="p in proposals" :key="p.rideId">
            <td>{{ formatDay(p.date) }} {{ formatTime(p.departureAt, group.timezone) }}</td>
            <td>{{ p.tripName }}</td>
            <td>
              <span v-if="p.displayName">{{ p.displayName }}</span>
              <span v-else class="muted">Nobody available</span>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="isManager">
        <button type="button" class="primary" @click="apply">Assign these drivers</button>
      </div>
    </template>
  </div>
</template>

<style scoped>
.inline {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

table.card {
  padding: 0;
}
</style>
