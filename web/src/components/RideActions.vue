<script setup lang="ts">
import { computed, ref } from 'vue'

import { rideApi } from '@/api'
import { errorMessage } from '@/api/client'
import type { Member, Ride } from '@/api/types'

const props = defineProps<{ ride: Ride; me: Member }>()
const emit = defineEmits<{ changed: [] }>()

const error = ref('')
const busy = ref(false)

const departed = computed(() => new Date(props.ride.departureAt).getTime() <= Date.now())
const open = computed(() => props.ride.status === 'SCHEDULED' && !departed.value)
const iDrive = computed(() => props.ride.driver?.memberId === props.me.id)

async function run(action: () => Promise<unknown>) {
  // Ignore clicks while the previous request is still on its way.
  if (busy.value) return
  busy.value = true
  error.value = ''
  try {
    await action()
    emit('changed')
  } catch (e) {
    error.value = errorMessage(e, 'Something went wrong')
  } finally {
    busy.value = false
  }
}

const requestSeat = () => run(() => rideApi.requestSeat(props.ride.id))
const cancelSeat = () => run(() => rideApi.cancelSeat(props.ride.id))
const drive = () => run(() => rideApi.drive(props.ride.id))
const stopDriving = () => run(() => rideApi.stopDriving(props.ride.id))
</script>

<template>
  <div v-if="open" class="actions">
    <div class="row">
      <template v-if="!iDrive">
        <button
          v-if="!ride.myBooking"
          type="button"
          class="primary"
          :disabled="busy"
          @click="requestSeat"
        >
          Ask for a seat
        </button>
        <button v-else type="button" :disabled="busy" @click="cancelSeat">Give up my seat</button>
      </template>
      <button v-if="!ride.driver && me.canDrive" type="button" :disabled="busy" @click="drive">
        I'll drive
      </button>
      <button v-if="iDrive" type="button" :disabled="busy" @click="stopDriving">
        I can't drive
      </button>
    </div>
    <p v-if="error" class="error">{{ error }}</p>
  </div>
</template>

<style scoped>
.actions {
  margin-top: 0.5rem;
}

.actions p {
  margin: 0.4rem 0 0;
}
</style>
