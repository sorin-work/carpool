<script setup lang="ts">
import { ref } from 'vue'

import { tripApi } from '@/api'
import { errorMessage } from '@/api/client'
import type { Trip } from '@/api/types'

const props = defineProps<{ trip: Trip }>()
const emit = defineEmits<{ done: [cancelled: number]; close: [] }>()

const from = ref('')
const to = ref('')
const reason = ref('')
const error = ref('')
const busy = ref(false)

async function submit() {
  error.value = ''
  busy.value = true
  try {
    const result = await tripApi.cancelDays(
      props.trip.id,
      from.value,
      to.value || from.value,
      reason.value || undefined,
    )
    emit('done', result.cancelled)
  } catch (e) {
    error.value = errorMessage(e, 'Could not cancel the rides')
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <form class="cancel-days" @submit.prevent="submit">
    <div class="row">
      <label>From <input v-model="from" type="date" required /></label>
      <label>To <input v-model="to" type="date" :min="from" /></label>
      <label class="grow">
        Reason
        <input v-model="reason" maxlength="300" placeholder="School holiday" />
      </label>
    </div>
    <p v-if="error" class="error">{{ error }}</p>
    <div class="row">
      <button type="submit" class="danger" :disabled="busy">Cancel these rides</button>
      <button type="button" @click="emit('close')">Close</button>
    </div>
  </form>
</template>

<style scoped>
.cancel-days {
  margin-top: 0.75rem;
  padding-top: 0.75rem;
  border-top: 1px solid var(--border);
}

.row {
  align-items: end;
}

.grow {
  flex: 1;
}
</style>
