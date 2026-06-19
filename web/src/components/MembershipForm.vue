<script setup lang="ts">
import { ref, watch } from 'vue'

import { groupApi } from '@/api'
import { errorMessage } from '@/api/client'
import type { Member } from '@/api/types'

const props = defineProps<{ groupId: number; member: Member }>()
const emit = defineEmits<{ saved: [member: Member] }>()

const canDrive = ref(props.member.canDrive)
const carSeats = ref(props.member.carSeats || 3)
const pickupNote = ref(props.member.pickupNote ?? '')
const error = ref('')
const saved = ref(false)
const busy = ref(false)

watch(
  () => props.member,
  (member) => {
    canDrive.value = member.canDrive
    carSeats.value = member.carSeats || 3
    pickupNote.value = member.pickupNote ?? ''
  },
)

async function save() {
  error.value = ''
  saved.value = false
  busy.value = true
  try {
    const member = await groupApi.updateMyMembership(props.groupId, {
      canDrive: canDrive.value,
      carSeats: canDrive.value ? carSeats.value : 0,
      pickupNote: pickupNote.value || undefined,
    })
    saved.value = true
    emit('saved', member)
  } catch (e) {
    error.value = errorMessage(e, 'Could not save')
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <form class="card" @submit.prevent="save">
    <h2>My car</h2>
    <label class="check">
      <input v-model="canDrive" type="checkbox" />
      I can drive for this group
    </label>
    <label v-if="canDrive">
      Free seats (not counting the driver)
      <input v-model.number="carSeats" type="number" min="1" max="8" required />
    </label>
    <label>
      Pickup note <span class="muted">(optional)</span>
      <input v-model="pickupNote" maxlength="200" placeholder="Corner of Elm St and 3rd" />
    </label>
    <p v-if="error" class="error">{{ error }}</p>
    <div class="row">
      <button type="submit" class="primary" :disabled="busy">Save</button>
      <span v-if="saved" class="muted">Saved</span>
    </div>
  </form>
</template>

<style scoped>
.check {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-weight: 400;
}
</style>
