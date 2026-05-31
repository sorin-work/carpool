<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { computed, ref, watch } from 'vue'

import { costApi } from '@/api'
import type { CostReport } from '@/api/types'
import EmptyState from '@/components/EmptyState.vue'
import { useGroupStore } from '@/stores/group'
import { todayIn } from '@/utils/dates'
import { formatCents } from '@/utils/money'

const store = useGroupStore()
const { group } = storeToRefs(store)

const month = ref(todayIn(group.value?.timezone ?? 'UTC').slice(0, 7))
const report = ref<CostReport | null>(null)

const range = computed(() => {
  const [y, m] = month.value.split('-').map(Number)
  const next = m === 12 ? `${y! + 1}-01` : `${y}-${String(m! + 1).padStart(2, '0')}`
  return { from: `${month.value}-01`, to: `${next}-01` }
})

async function load() {
  if (!group.value || !month.value) return
  report.value = await costApi.get(group.value.id, range.value.from, range.value.to)
}

function money(cents: number) {
  return formatCents(cents, report.value?.currency ?? 'EUR')
}

watch(month, load, { immediate: true })
</script>

<template>
  <div v-if="report && group" class="stack">
    <div class="row">
      <h2>Fuel costs</h2>
      <span class="spacer" />
      <input v-model="month" type="month" aria-label="Month" />
    </div>

    <p v-if="!Number(report.costPerKm)" class="card">
      No fuel cost is set for this group, so nobody owes anything.
      <template v-if="store.isManager">
        Set one in <RouterLink :to="`/groups/${group.id}/settings`">Settings</RouterLink>.
      </template>
    </p>
    <p v-else class="muted">
      {{ money(Number(report.costPerKm) * 100) }} per km, shared by everyone in the car. Rides this
      month cost {{ money(report.totalCents) }} in total.
    </p>

    <EmptyState v-if="!report.totalCents" title="Nothing to share">
      No rides with passengers took place this month.
    </EmptyState>

    <template v-else>
      <div class="table-scroll">
        <table class="card">
          <thead>
            <tr>
              <th>Member</th>
              <th class="num">Earned driving</th>
              <th class="num">Owed for rides</th>
              <th class="num">Balance</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="b in report.balances" :key="b.memberId">
              <td>{{ b.displayName }}</td>
              <td class="num">{{ money(b.earnedCents) }}</td>
              <td class="num">{{ money(b.spentCents) }}</td>
              <td class="num" :class="{ pos: b.balanceCents > 0, neg: b.balanceCents < 0 }">
                {{ money(b.balanceCents) }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <section class="card">
        <h3>To settle up</h3>
        <p v-if="!report.settlements.length" class="muted">Everyone is even.</p>
        <ul v-else class="transfers">
          <li v-for="t in report.settlements" :key="`${t.fromMemberId}-${t.toMemberId}`">
            <strong>{{ t.fromName }}</strong> pays <strong>{{ t.toName }}</strong>
            {{ money(t.amountCents) }}
          </li>
        </ul>
      </section>
    </template>
  </div>
</template>

<style scoped>
table.card {
  padding: 0;
}

.pos {
  color: var(--accent-strong);
  font-weight: 600;
}

.neg {
  color: var(--danger);
  font-weight: 600;
}

.transfers {
  margin: 0;
  padding-left: 1.2rem;
  display: grid;
  gap: 0.3rem;
}
</style>
