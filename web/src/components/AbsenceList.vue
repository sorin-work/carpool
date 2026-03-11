<script setup lang="ts">
import { onMounted, ref } from 'vue'

import { absenceApi } from '@/api'
import { ApiError } from '@/api/client'
import type { Absence } from '@/api/types'
import { formatDay } from '@/utils/dates'

const props = defineProps<{ groupId: number; myMemberId: number; canManage: boolean }>()
const emit = defineEmits<{ changed: [] }>()

const absences = ref<Absence[]>([])
const startsOn = ref('')
const endsOn = ref('')
const note = ref('')
const error = ref('')

async function load() {
  absences.value = await absenceApi.list(props.groupId)
}

async function add() {
  error.value = ''
  try {
    await absenceApi.add(
      props.groupId,
      startsOn.value,
      endsOn.value || startsOn.value,
      note.value || undefined,
    )
    startsOn.value = ''
    endsOn.value = ''
    note.value = ''
    await load()
    emit('changed')
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : 'Could not save'
  }
}

async function remove(absence: Absence) {
  await absenceApi.remove(absence.id)
  await load()
  emit('changed')
}

onMounted(load)
</script>

<template>
  <section class="card stack">
    <h2>Away days</h2>
    <p class="muted">The rotation will not pick you on these days.</p>
    <ul v-if="absences.length" class="list">
      <li v-for="a in absences" :key="a.id" class="row">
        <strong>{{ a.displayName }}</strong>
        <span>
          {{ formatDay(a.startsOn) }}
          <template v-if="a.endsOn !== a.startsOn"> – {{ formatDay(a.endsOn) }}</template>
        </span>
        <span v-if="a.note" class="muted">{{ a.note }}</span>
        <span class="spacer" />
        <button
          v-if="a.memberId === myMemberId || canManage"
          type="button"
          class="link"
          @click="remove(a)"
        >
          Remove
        </button>
      </li>
    </ul>
    <p v-else class="muted">Nobody is away.</p>
    <form class="row" @submit.prevent="add">
      <label>From <input v-model="startsOn" type="date" required /></label>
      <label>To <input v-model="endsOn" type="date" :min="startsOn" /></label>
      <label>Note <input v-model="note" maxlength="200" placeholder="Holiday" /></label>
      <button type="submit">Add</button>
    </form>
    <p v-if="error" class="error">{{ error }}</p>
  </section>
</template>

<style scoped>
.list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 0.4rem;
}

form.row {
  align-items: end;
}
</style>
