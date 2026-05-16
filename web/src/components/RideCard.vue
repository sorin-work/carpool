<script setup lang="ts">
import { computed } from 'vue'

import type { Ride } from '@/api/types'
import { formatTime } from '@/utils/dates'

const props = defineProps<{ ride: Ride; timeZone: string; groupId: number }>()

const seatsLeft = computed(() => Math.max(props.ride.seatsOffered - props.ride.seatsTaken, 0))

const bookingLabel: Record<string, string> = {
  PENDING: 'Seat requested',
  CONFIRMED: 'You have a seat',
  WAITLISTED: 'On the waitlist',
}
</script>

<template>
  <article class="ride card" :class="{ cancelled: ride.status === 'CANCELLED' }">
    <div class="time">{{ formatTime(ride.departureAt, timeZone) }}</div>
    <div class="body">
      <RouterLink :to="`/groups/${groupId}/rides/${ride.id}`" class="title">
        {{ ride.tripName }}
      </RouterLink>
      <div class="muted">{{ ride.origin }} → {{ ride.destination }}</div>
      <div v-if="ride.note" class="note">📌 {{ ride.note }}</div>
      <div class="row meta">
        <span v-if="ride.status === 'CANCELLED'" class="badge danger">Cancelled</span>
        <template v-else>
          <span v-if="ride.driver" class="badge">{{ ride.driver.displayName }} drives</span>
          <span v-else class="badge warning">Needs a driver</span>
          <span v-if="ride.driver" class="muted">
            {{ seatsLeft }} of {{ ride.seatsOffered }} seats free
            <template v-if="ride.waitlisted"> · {{ ride.waitlisted }} waiting</template>
          </span>
        </template>
        <span v-if="ride.myBooking" class="badge neutral">{{ bookingLabel[ride.myBooking] }}</span>
      </div>
      <slot />
    </div>
  </article>
</template>

<style scoped>
.ride {
  display: flex;
  gap: 1rem;
  padding: 0.9rem 1rem;
}

.time {
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  min-width: 3.2rem;
}

.body {
  flex: 1;
  display: grid;
  gap: 0.25rem;
}

.title {
  font-weight: 600;
  color: var(--text);
  text-decoration: none;
}

.title:hover {
  color: var(--accent);
}

.note {
  font-size: 0.9rem;
}

.meta {
  margin-top: 0.15rem;
}

.cancelled .title,
.cancelled .time {
  text-decoration: line-through;
  color: var(--muted);
}
</style>
