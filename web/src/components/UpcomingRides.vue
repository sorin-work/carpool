<script setup lang="ts">
import { onMounted, ref } from 'vue'

import { rideApi } from '@/api'
import type { MyRide } from '@/api/types'
import { formatDay, formatTime } from '@/utils/dates'

const rides = ref<MyRide[]>([])
const loaded = ref(false)

onMounted(async () => {
  try {
    rides.value = await rideApi.mine(7)
  } finally {
    loaded.value = true
  }
})

const bookingLabel: Record<string, string> = {
  PENDING: 'waiting for a driver',
  CONFIRMED: 'seat confirmed',
  WAITLISTED: 'on the waitlist',
}
</script>

<template>
  <section v-if="loaded && rides.length" class="card upcoming">
    <h2>Coming up</h2>
    <ul>
      <li v-for="item in rides" :key="item.ride.id" class="row">
        <span class="when">
          {{ formatDay(item.ride.date) }} {{ formatTime(item.ride.departureAt, item.timezone) }}
        </span>
        <RouterLink :to="`/groups/${item.groupId}/rides/${item.ride.id}`">
          {{ item.ride.tripName }}
        </RouterLink>
        <span class="muted">{{ item.groupName }}</span>
        <span class="spacer" />
        <span v-if="item.driving" class="badge">You drive</span>
        <span v-else-if="item.ride.myBooking" class="badge neutral">
          {{ bookingLabel[item.ride.myBooking] }}
        </span>
      </li>
    </ul>
  </section>
</template>

<style scoped>
ul {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 0.5rem;
}

.when {
  font-variant-numeric: tabular-nums;
  min-width: 8.5rem;
  font-weight: 600;
}
</style>
