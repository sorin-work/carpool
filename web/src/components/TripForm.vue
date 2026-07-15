<script setup lang="ts">
import { reactive, ref } from 'vue'

import { ApiError } from '@/api/client'
import type { Trip, TripInput } from '@/api/types'
import DayPicker from '@/components/DayPicker.vue'
import { shortTime } from '@/utils/dates'

const props = defineProps<{ trip?: Trip; submit: (input: TripInput) => Promise<void> }>()
const emit = defineEmits<{ cancel: [] }>()

const form = reactive<TripInput>({
  name: props.trip?.name ?? '',
  origin: props.trip?.origin ?? '',
  destination: props.trip?.destination ?? '',
  departureTime: props.trip ? shortTime(props.trip.departureTime) : '07:45',
  days: props.trip?.days ?? ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'],
  distanceKm: props.trip?.distanceKm ?? 5,
  startsOn: props.trip?.startsOn ?? new Date().toISOString().slice(0, 10),
  endsOn: props.trip?.endsOn ?? null,
})
const error = ref('')
const fields = ref<Record<string, string>>({})
const busy = ref(false)

async function save() {
  error.value = ''
  fields.value = {}
  if (!form.days.length) {
    fields.value = { days: 'Pick at least one day' }
    return
  }
  busy.value = true
  try {
    await props.submit({ ...form, endsOn: form.endsOn || null })
  } catch (e) {
    if (e instanceof ApiError) {
      error.value = e.message
      fields.value = e.fields
    } else {
      error.value = 'Could not save the trip'
    }
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <form class="card trip-form" @submit.prevent="save">
    <label>
      Name
      <input v-model="form.name" maxlength="80" placeholder="Morning school run" required />
    </label>
    <div class="two">
      <label>
        From
        <input v-model="form.origin" maxlength="200" placeholder="12 Elm St" required />
      </label>
      <label>
        To
        <input
          v-model="form.destination"
          maxlength="200"
          placeholder="Riverside Primary"
          required
        />
      </label>
    </div>
    <div class="two">
      <label>
        Leaves at
        <input v-model="form.departureTime" type="time" required />
      </label>
      <label>
        Distance (km)
        <input v-model.number="form.distanceKm" type="number" step="0.1" required />
        <small v-if="fields.distanceKm" class="error">{{ fields.distanceKm }}</small>
      </label>
    </div>
    <div>
      <div class="label">Runs on</div>
      <DayPicker v-model="form.days" />
      <small v-if="fields.days" class="error">{{ fields.days }}</small>
    </div>
    <div class="two">
      <label>
        First day
        <input v-model="form.startsOn" type="date" required />
      </label>
      <label>
        Last day <span class="muted">(optional)</span>
        <input v-model="form.endsOn" type="date" />
        <small v-if="fields.endsOn" class="error">{{ fields.endsOn }}</small>
      </label>
    </div>
    <p v-if="trip" class="muted hint">
      Rides already planned move to the new time; anyone on a day the trip no longer runs is told.
    </p>
    <p v-if="error && !Object.keys(fields).length" class="error">{{ error }}</p>
    <div class="row">
      <button type="submit" class="primary" :disabled="busy">
        {{ trip ? 'Save changes' : 'Add trip' }}
      </button>
      <button type="button" @click="emit('cancel')">Cancel</button>
    </div>
  </form>
</template>

<style scoped>
.two {
  display: grid;
  gap: 0.9rem;
  grid-template-columns: repeat(auto-fit, minmax(12rem, 1fr));
}

.hint {
  font-size: 0.9rem;
  margin: 0;
}

.label {
  font-weight: 500;
  margin-bottom: 0.3rem;
}
</style>
