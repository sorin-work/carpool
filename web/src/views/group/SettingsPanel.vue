<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import { groupApi } from '@/api'
import { ApiError, errorMessage } from '@/api/client'
import { useGroupStore } from '@/stores/group'

const store = useGroupStore()
const router = useRouter()
const { group, isManager } = storeToRefs(store)

const form = reactive({
  name: group.value?.name ?? '',
  description: group.value?.description ?? '',
  timezone: group.value?.timezone ?? 'Europe/Bucharest',
  costPerKm: Number(group.value?.costPerKm ?? 0),
  currency: group.value?.currency ?? 'EUR',
})
const error = ref('')
const fields = ref<Record<string, string>>({})
const saved = ref(false)

// Intl.supportedValuesOf is not in the ES2020 lib typings yet.
const intl = Intl as typeof Intl & { supportedValuesOf?: (key: 'timeZone') => string[] }
const zones = intl.supportedValuesOf?.('timeZone') ?? [form.timezone]

async function save() {
  if (!group.value) return
  error.value = ''
  fields.value = {}
  saved.value = false
  try {
    store.replace(
      await groupApi.update(group.value.id, {
        name: form.name,
        description: form.description,
        timezone: form.timezone,
        costPerKm: form.costPerKm,
        currency: form.currency.toUpperCase(),
      }),
    )
    saved.value = true
  } catch (e) {
    if (e instanceof ApiError) {
      error.value = e.message
      fields.value = e.fields
    } else {
      error.value = 'Could not save the settings'
    }
  }
}

async function deleteGroup() {
  if (!group.value) return
  const typed = prompt(`This deletes every trip, ride and balance. Type the group name to confirm:`)
  if (typed?.trim() !== group.value.name) return
  error.value = ''
  try {
    await groupApi.remove(group.value.id)
    await router.push('/groups')
  } catch (e) {
    error.value = errorMessage(e, 'Could not delete the group')
  }
}

async function leave() {
  if (!group.value || !confirm(`Leave ${group.value.name}?`)) return
  error.value = ''
  try {
    await groupApi.leave(group.value.id)
    await router.push('/groups')
  } catch (e) {
    error.value = errorMessage(e, 'Could not leave the group')
  }
}
</script>

<template>
  <div v-if="group" class="stack">
    <form v-if="isManager" class="card" @submit.prevent="save">
      <h2>Group settings</h2>
      <label>
        Name
        <input v-model="form.name" maxlength="80" required />
      </label>
      <label>
        Description
        <input v-model="form.description" maxlength="500" />
      </label>
      <label>
        Time zone
        <select v-model="form.timezone">
          <option v-for="zone in zones" :key="zone" :value="zone">{{ zone }}</option>
        </select>
      </label>
      <div class="row">
        <label>
          Fuel cost per km
          <input v-model.number="form.costPerKm" type="number" min="0" max="10" step="0.001" />
          <small v-if="fields.costPerKm" class="error">{{ fields.costPerKm }}</small>
        </label>
        <label>
          Currency
          <input v-model="form.currency" maxlength="3" size="4" />
          <small v-if="fields.currency" class="error">{{ fields.currency }}</small>
        </label>
      </div>
      <p v-if="error && !Object.keys(fields).length" class="error">{{ error }}</p>
      <div class="row">
        <button type="submit" class="primary">Save</button>
        <span v-if="saved" class="muted">Saved</span>
      </div>
    </form>

    <section class="card">
      <h2>Leave the group</h2>
      <p class="muted">
        <template v-if="group.myRole === 'OWNER'">
          As the owner you need to hand the group to someone else first, unless you are the last
          member.
        </template>
        <template v-else>You can rejoin later with the invite code.</template>
      </p>
      <button type="button" class="danger" @click="leave">Leave group</button>
    </section>

    <section v-if="group.myRole === 'OWNER'" class="card">
      <h2>Delete the group</h2>
      <p class="muted">Removes the group for everyone, with all its trips, rides and costs.</p>
      <button type="button" class="danger" @click="deleteGroup">Delete group</button>
    </section>
  </div>
</template>
