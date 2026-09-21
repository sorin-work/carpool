<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

import { rideApi, swapApi } from '@/api'
import { errorMessage } from '@/api/client'
import type { Booking, Ride } from '@/api/types'
import MemberAvatar from '@/components/MemberAvatar.vue'
import RideActions from '@/components/RideActions.vue'
import { useGroupStore } from '@/stores/group'
import { addDays, formatDay, formatTime } from '@/utils/dates'
import { formatCents } from '@/utils/money'

const route = useRoute()
const store = useGroupStore()
const { group, me, isManager, drivers } = storeToRefs(store)

const ride = ref<Ride | null>(null)
const passengers = ref<Booking[]>([])
const error = ref('')
const reason = ref('')
const swapTarget = ref<number | ''>('')
const swapOffered = ref<number | ''>('')
const swapMessage = ref('')
const swapSent = ref(false)
const targetRides = ref<Ride[]>([])

const rideId = computed(() => Number(route.params.rideId))
const iDrive = computed(() => ride.value?.driver?.memberId === me.value?.id)
const open = computed(
  () =>
    ride.value?.status === 'SCHEDULED' && new Date(ride.value.departureAt).getTime() > Date.now(),
)
const canCancel = computed(() => open.value && (iDrive.value || isManager.value))
/** What each person in the car pays, if the group shares fuel costs. */
const sharePerPerson = computed(() => {
  const rate = Number(group.value?.costPerKm ?? 0)
  if (!ride.value || !rate) return null
  const people = Math.max(ride.value.seatsTaken, 0) + 1
  return Math.round((ride.value.distanceKm * rate * 100) / people)
})
const otherDrivers = computed(() => drivers.value.filter((d) => d.id !== me.value?.id))

const statusLabel: Record<string, string> = {
  PENDING: 'Waiting for a driver',
  CONFIRMED: 'Confirmed',
  WAITLISTED: 'Waitlist',
}

async function load() {
  error.value = ''
  try {
    ride.value = await rideApi.get(rideId.value)
    passengers.value = await rideApi.passengers(rideId.value)
  } catch (e) {
    error.value = errorMessage(e, 'Could not load the ride')
  }
}

async function act(action: () => Promise<Ride>) {
  error.value = ''
  try {
    ride.value = await action()
    passengers.value = await rideApi.passengers(rideId.value)
  } catch (e) {
    error.value = errorMessage(e, 'Something went wrong')
  }
}

const cancelRide = () => act(() => rideApi.cancel(rideId.value, reason.value || undefined))
const reinstate = () => act(() => rideApi.reinstate(rideId.value))
const pickedDriver = ref<number | ''>('')
async function assign() {
  if (!pickedDriver.value) return
  const memberId = pickedDriver.value
  await act(() => rideApi.assignDriver(rideId.value, memberId))
  pickedDriver.value = ''
}

const editingNote = ref(false)
const noteDraft = ref('')
function editNote() {
  noteDraft.value = ride.value?.note ?? ''
  editingNote.value = true
}
async function saveNote() {
  await act(() => rideApi.setNote(rideId.value, noteDraft.value))
  editingNote.value = false
}

watch(swapTarget, async (target) => {
  swapOffered.value = ''
  targetRides.value = []
  if (!target || !group.value || !ride.value) return
  const upcoming = await rideApi.schedule(
    group.value.id,
    ride.value.date,
    addDays(ride.value.date, 14),
  )
  targetRides.value = upcoming.filter(
    (r) =>
      r.driver?.memberId === target &&
      r.status === 'SCHEDULED' &&
      r.id !== ride.value?.id &&
      new Date(r.departureAt).getTime() > Date.now(),
  )
})

async function askSwap() {
  if (!swapTarget.value) return
  error.value = ''
  try {
    await swapApi.create(
      rideId.value,
      swapTarget.value,
      swapOffered.value || undefined,
      swapMessage.value || undefined,
    )
    swapSent.value = true
  } catch (e) {
    error.value = errorMessage(e, 'Could not send the swap request')
  }
}

watch(rideId, load, { immediate: true })
</script>

<template>
  <div v-if="ride && group" class="stack">
    <RouterLink :to="`/groups/${group.id}`" class="muted">← Schedule</RouterLink>
    <div class="card stack">
      <div class="row">
        <h2>{{ ride.tripName }}</h2>
        <span v-if="ride.status === 'CANCELLED'" class="badge danger">Cancelled</span>
      </div>
      <p>
        {{ formatDay(ride.date) }} at {{ formatTime(ride.departureAt, group.timezone) }} ·
        {{ ride.origin }} → {{ ride.destination }} · {{ ride.distanceKm }} km
      </p>
      <p v-if="ride.cancelReason" class="muted">Reason: {{ ride.cancelReason }}</p>
      <form v-if="editingNote" class="row" @submit.prevent="saveNote">
        <input v-model="noteDraft" maxlength="300" placeholder="Leaving from the back gate" />
        <button type="submit" class="primary">Save note</button>
        <button type="button" @click="editingNote = false">Cancel</button>
      </form>
      <p v-else-if="ride.note || iDrive || isManager" class="note-line">
        <span v-if="ride.note">📌 {{ ride.note }}</span>
        <button v-if="iDrive || isManager" type="button" class="link" @click="editNote">
          {{ ride.note ? 'Edit note' : 'Add a note for riders' }}
        </button>
      </p>
      <p>
        <strong>Driver:</strong>
        {{ ride.driver ? ride.driver.displayName : 'nobody yet' }}
        <template v-if="ride.driver">· {{ ride.seatsOffered }} seats</template>
      </p>
      <p v-if="sharePerPerson !== null && ride.seatsTaken" class="muted">
        Fuel: about {{ formatCents(sharePerPerson, group.currency) }} per rider.
      </p>
      <RideActions v-if="me" :ride="ride" :me="me" @changed="load" />
      <form v-if="isManager && open" class="row" @submit.prevent="assign">
        <select v-model="pickedDriver" aria-label="Driver">
          <option value="" disabled>Choose a driver…</option>
          <option
            v-for="d in drivers"
            :key="d.id"
            :value="d.id"
            :disabled="d.id === ride.driver?.memberId"
          >
            {{ d.displayName }} ({{ d.carSeats }} seats)
          </option>
        </select>
        <button type="submit" :disabled="!pickedDriver">Assign</button>
      </form>
    </div>

    <p v-if="error" class="error">{{ error }}</p>

    <section class="card">
      <h3>Passengers</h3>
      <p v-if="!passengers.length" class="muted">Nobody has asked for a seat yet.</p>
      <ul v-else class="people">
        <li v-for="p in passengers" :key="p.id" class="row">
          <MemberAvatar :name="p.displayName" :size="28" />
          <span>
            {{ p.displayName }}
            <span v-if="p.pickupNote" class="muted note">· {{ p.pickupNote }}</span>
          </span>
          <span class="spacer" />
          <span
            class="badge"
            :class="{ warning: p.status === 'WAITLISTED', neutral: p.status === 'PENDING' }"
          >
            {{ statusLabel[p.status] }}
          </span>
        </li>
      </ul>
    </section>

    <section v-if="iDrive && open" class="card">
      <h3>Ask someone to swap</h3>
      <p v-if="swapSent" class="muted">Request sent. You'll get a notification when they answer.</p>
      <form v-else @submit.prevent="askSwap">
        <label>
          Who should drive instead?
          <select v-model="swapTarget" required>
            <option value="" disabled>Pick a driver</option>
            <option v-for="d in otherDrivers" :key="d.id" :value="d.id">{{ d.displayName }}</option>
          </select>
        </label>
        <label v-if="targetRides.length">
          Take one of their rides in return <span class="muted">(optional)</span>
          <select v-model="swapOffered">
            <option value="">No, just hand this one over</option>
            <option v-for="r in targetRides" :key="r.id" :value="r.id">
              {{ r.tripName }}, {{ formatDay(r.date) }}
            </option>
          </select>
        </label>
        <label>
          Message <span class="muted">(optional)</span>
          <input v-model="swapMessage" maxlength="300" placeholder="Dentist that morning" />
        </label>
        <button type="submit" class="primary">Send request</button>
      </form>
    </section>

    <section v-if="canCancel" class="card">
      <h3>Cancel this ride</h3>
      <p class="muted">Everyone on the ride gets a notification.</p>
      <form @submit.prevent="cancelRide">
        <label>
          Reason <span class="muted">(optional)</span>
          <input v-model="reason" maxlength="300" placeholder="School closed" />
        </label>
        <button type="submit" class="danger">Cancel ride</button>
      </form>
    </section>

    <button
      v-if="ride.status === 'CANCELLED' && isManager"
      type="button"
      class="primary"
      @click="reinstate"
    >
      Put the ride back on
    </button>
  </div>
  <p v-else-if="error" class="error">{{ error }}</p>
</template>

<style scoped>
.people {
  list-style: none;
  padding: 0;
  margin: 0;
  display: grid;
  gap: 0.5rem;
}

p {
  margin: 0;
}

.note {
  font-size: 0.9rem;
}

.note-line {
  display: flex;
  gap: 0.75rem;
  align-items: center;
}
</style>
