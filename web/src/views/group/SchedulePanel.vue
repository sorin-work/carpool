<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { computed, ref, watch } from 'vue'

import { rideApi } from '@/api'
import { ApiError, download } from '@/api/client'
import type { Ride } from '@/api/types'
import EmptyState from '@/components/EmptyState.vue'
import RideActions from '@/components/RideActions.vue'
import RideCard from '@/components/RideCard.vue'
import { useGroupStore } from '@/stores/group'
import { addDays, formatDay, todayIn } from '@/utils/dates'

const store = useGroupStore()
const { group, me } = storeToRefs(store)

const from = ref(todayIn(group.value?.timezone ?? 'UTC'))
const rides = ref<Ride[]>([])
const loading = ref(true)
const error = ref('')

const to = computed(() => addDays(from.value, 7))

const days = computed(() => {
  const byDate = new Map<string, Ride[]>()
  for (const ride of rides.value) {
    byDate.set(ride.date, [...(byDate.get(ride.date) ?? []), ride])
  }
  return [...byDate.entries()].map(([date, list]) => ({ date, rides: list }))
})

async function load() {
  if (!group.value) return
  loading.value = true
  error.value = ''
  try {
    rides.value = await rideApi.schedule(group.value.id, from.value, to.value)
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : 'Could not load the schedule'
  } finally {
    loading.value = false
  }
}

async function downloadCalendar() {
  if (!group.value) return
  try {
    await download(`/groups/${group.value.id}/calendar.ics`, 'carpool-rides.ics')
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : 'Could not download the calendar'
  }
}

function shift(days: number) {
  from.value = addDays(from.value, days)
}

watch(from, load, { immediate: true })
</script>

<template>
  <div v-if="group" class="stack">
    <div class="row">
      <button type="button" aria-label="Previous week" @click="shift(-7)">←</button>
      <strong>{{ formatDay(from) }} – {{ formatDay(addDays(to, -1)) }}</strong>
      <button type="button" aria-label="Next week" @click="shift(7)">→</button>
      <button type="button" class="link" @click="from = todayIn(group.timezone)">Today</button>
      <span class="spacer" />
      <button type="button" class="link" @click="downloadCalendar">
        Add my rides to a calendar
      </button>
    </div>

    <p v-if="loading" class="muted">Loading…</p>
    <p v-else-if="error" class="error">{{ error }}</p>
    <EmptyState v-else-if="!rides.length" title="No rides this week">
      Trips appear here once an admin adds them on the Trips tab.
    </EmptyState>

    <section v-for="day in days" :key="day.date" class="day">
      <h3>{{ formatDay(day.date) }}</h3>
      <div class="stack">
        <RideCard
          v-for="ride in day.rides"
          :key="ride.id"
          :ride="ride"
          :time-zone="group.timezone"
          :group-id="group.id"
        >
          <RideActions v-if="me" :ride="ride" :me="me" @changed="load" />
        </RideCard>
      </div>
    </section>
  </div>
</template>

<style scoped>
.day h3 {
  font-size: 0.95rem;
  color: var(--muted);
  margin-bottom: 0.5rem;
}
</style>
