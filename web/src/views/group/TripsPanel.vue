<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { onMounted, ref } from 'vue'

import { tripApi } from '@/api'
import type { Trip, TripInput } from '@/api/types'
import CancelDaysForm from '@/components/CancelDaysForm.vue'
import EmptyState from '@/components/EmptyState.vue'
import TripForm from '@/components/TripForm.vue'
import { useGroupStore } from '@/stores/group'
import { describeDays, formatDay, shortTime } from '@/utils/dates'

const store = useGroupStore()
const { group, isManager } = storeToRefs(store)

const trips = ref<Trip[]>([])
const loading = ref(true)
const adding = ref(false)
const editing = ref<number | null>(null)
const cancelling = ref<number | null>(null)
const notice = ref('')

function cancelled(count: number) {
  cancelling.value = null
  notice.value = count === 1 ? '1 ride cancelled.' : `${count} rides cancelled.`
}

async function load() {
  if (!group.value) return
  loading.value = true
  trips.value = await tripApi.list(group.value.id)
  loading.value = false
}

async function create(input: TripInput) {
  await tripApi.create(group.value!.id, input)
  adding.value = false
  await load()
}

function updater(trip: Trip) {
  return async (input: TripInput) => {
    await tripApi.update(trip.id, input)
    editing.value = null
    await load()
  }
}

async function toggle(trip: Trip) {
  const updated = trip.active ? await tripApi.pause(trip.id) : await tripApi.resume(trip.id)
  trips.value = trips.value.map((t) => (t.id === updated.id ? updated : t))
}

async function remove(trip: Trip) {
  if (!confirm(`Delete "${trip.name}" and all its rides?`)) return
  await tripApi.remove(trip.id)
  await load()
}

onMounted(load)
</script>

<template>
  <div class="stack">
    <div class="row">
      <h2>Trips</h2>
      <span class="spacer" />
      <button v-if="isManager && !adding" type="button" class="primary" @click="adding = true">
        Add trip
      </button>
    </div>

    <p v-if="notice" class="badge">{{ notice }}</p>

    <TripForm v-if="adding" :submit="create" @cancel="adding = false" />

    <p v-if="loading" class="muted">Loading…</p>
    <EmptyState v-else-if="!trips.length && !adding" title="No trips yet">
      <template v-if="isManager">Add the journeys your group makes every week.</template>
      <template v-else>An admin has not added any trips yet.</template>
    </EmptyState>

    <template v-for="trip in trips" :key="trip.id">
      <TripForm
        v-if="editing === trip.id"
        :trip="trip"
        :submit="updater(trip)"
        @cancel="editing = null"
      />
      <article v-else class="card trip" :class="{ paused: !trip.active }">
        <div class="row">
          <h3>{{ trip.name }}</h3>
          <span v-if="!trip.active" class="badge warning">Paused</span>
          <span class="spacer" />
          <template v-if="isManager">
            <button type="button" class="link" @click="editing = trip.id">Edit</button>
            <button type="button" class="link" @click="cancelling = trip.id">Cancel days</button>
            <button type="button" class="link" @click="toggle(trip)">
              {{ trip.active ? 'Pause' : 'Resume' }}
            </button>
            <button type="button" class="link danger-link" @click="remove(trip)">Delete</button>
          </template>
        </div>
        <p>{{ trip.origin }} → {{ trip.destination }} · {{ trip.distanceKm }} km</p>
        <p class="muted">
          {{ describeDays(trip.days) }} at {{ shortTime(trip.departureTime) }} · from
          {{ formatDay(trip.startsOn) }}
          <template v-if="trip.endsOn"> until {{ formatDay(trip.endsOn) }}</template>
        </p>
        <CancelDaysForm
          v-if="cancelling === trip.id"
          :trip="trip"
          @done="cancelled"
          @close="cancelling = null"
        />
      </article>
    </template>
  </div>
</template>

<style scoped>
.trip h3 {
  margin: 0;
}

.trip p {
  margin: 0.35rem 0 0;
}

.paused {
  opacity: 0.75;
}

.danger-link {
  color: var(--danger);
}
</style>
