<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { computed, ref, watch } from 'vue'

import { statsApi } from '@/api'
import { download } from '@/api/client'
import type { GroupStats, MemberRide } from '@/api/types'
import EmptyState from '@/components/EmptyState.vue'
import MemberAvatar from '@/components/MemberAvatar.vue'
import { useGroupStore } from '@/stores/group'
import { addDays, formatDay, todayIn } from '@/utils/dates'
import { formatKm } from '@/utils/money'

const store = useGroupStore()
const { group } = storeToRefs(store)

const period = ref(90)
const stats = ref<GroupStats | null>(null)

const openMember = ref<number | null>(null)
const memberRides = ref<MemberRide[]>([])

async function toggleMember(memberId: number) {
  if (openMember.value === memberId) {
    openMember.value = null
    return
  }
  if (!group.value || !stats.value) return
  openMember.value = memberId
  memberRides.value = await statsApi.memberRides(
    group.value.id,
    memberId,
    stats.value.from,
    stats.value.to,
  )
}

const maxDrives = computed(() => Math.max(1, ...(stats.value?.members.map((m) => m.drives) ?? [])))

async function load() {
  if (!group.value) return
  const to = addDays(todayIn(group.value.timezone), 1)
  stats.value = await statsApi.get(group.value.id, addDays(to, -period.value), to)
}

function exportCsv() {
  if (!group.value || !stats.value) return
  const { from, to } = stats.value
  download(
    `/groups/${group.value.id}/rides.csv?from=${from}&to=${to}`,
    `rides-${from}-to-${to}.csv`,
  )
}

watch(period, load, { immediate: true })
</script>

<template>
  <div v-if="stats" class="stack">
    <div class="row">
      <h2>Who drove</h2>
      <span class="spacer" />
      <button type="button" class="link" :disabled="!stats.rides" @click="exportCsv">
        Download CSV
      </button>
      <select v-model.number="period" aria-label="Period">
        <option :value="30">Last 30 days</option>
        <option :value="90">Last 90 days</option>
        <option :value="365">Last year</option>
      </select>
    </div>
    <p class="muted">
      {{ stats.rides }} rides
      <template v-if="stats.cancelled">and {{ stats.cancelled }} cancelled</template>
      between {{ formatDay(stats.from) }} and {{ formatDay(addDays(stats.to, -1)) }}.
    </p>

    <EmptyState v-if="!stats.rides" title="No rides yet">
      Stats appear once rides have taken place.
    </EmptyState>
    <div v-else class="table-scroll">
      <table class="card">
        <thead>
          <tr>
            <th>Member</th>
            <th class="num">Drives</th>
            <th class="num">Driven</th>
            <th class="num">Rides</th>
            <th class="num">Share</th>
            <th>Last drove</th>
            <th class="num">Next 2 weeks</th>
          </tr>
        </thead>
        <tbody>
          <template v-for="m in stats.members" :key="m.memberId">
            <tr>
              <td>
                <button
                  type="button"
                  class="link row who"
                  :aria-expanded="openMember === m.memberId"
                  @click="toggleMember(m.memberId)"
                >
                  <MemberAvatar :name="m.displayName" :size="26" />
                  {{ m.displayName }}
                </button>
              </td>
              <td class="num">
                <span class="bar" :style="{ width: `${(m.drives / maxDrives) * 4}rem` }" />
                {{ m.drives }}
              </td>
              <td class="num">{{ formatKm(m.kmDriven) }}</td>
              <td class="num">{{ m.ridesTaken }}</td>
              <td class="num">{{ m.driveShare }}%</td>
              <td>{{ m.lastDrove ? formatDay(m.lastDrove) : '—' }}</td>
              <td class="num">{{ m.upcomingDrives || '—' }}</td>
            </tr>
            <tr v-if="openMember === m.memberId" class="detail">
              <td colspan="7">
                <p v-if="!memberRides.length" class="muted">No rides in this period.</p>
                <ul v-else>
                  <li v-for="r in memberRides" :key="r.rideId">
                    {{ formatDay(r.date) }} · {{ r.tripName }} · {{ r.driver ? 'drove' : 'rode' }}
                    {{ formatKm(r.distanceKm) }}
                  </li>
                </ul>
              </td>
            </tr>
          </template>
        </tbody>
      </table>
    </div>
  </div>
</template>

<style scoped>
table.card {
  padding: 0;
}

.who {
  color: var(--text);
  text-align: left;
}

.detail ul {
  margin: 0;
  padding-left: 1.2rem;
  font-size: 0.9rem;
}

.bar {
  display: inline-block;
  height: 0.5rem;
  border-radius: 4px;
  background: var(--accent);
  opacity: 0.6;
  margin-right: 0.4rem;
  vertical-align: middle;
}
</style>
