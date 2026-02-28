<script setup lang="ts">
import type { Weekday } from '@/api/types'
import { WEEKDAYS, WEEKDAY_SHORT } from '@/utils/dates'

const model = defineModel<Weekday[]>({ required: true })

function toggle(day: Weekday) {
  model.value = model.value.includes(day)
    ? model.value.filter((d) => d !== day)
    : WEEKDAYS.filter((d) => d === day || model.value.includes(d))
}
</script>

<template>
  <div class="days" role="group" aria-label="Days of the week">
    <button
      v-for="day in WEEKDAYS"
      :key="day"
      type="button"
      :class="{ on: model.includes(day) }"
      :aria-pressed="model.includes(day)"
      @click="toggle(day)"
    >
      {{ WEEKDAY_SHORT[day] }}
    </button>
  </div>
</template>

<style scoped>
.days {
  display: flex;
  gap: 0.3rem;
  flex-wrap: wrap;
}

.days button {
  min-width: 3rem;
  padding: 0.35rem 0.5rem;
}

.days button.on {
  background: var(--accent);
  border-color: var(--accent);
  color: #fff;
}
</style>
