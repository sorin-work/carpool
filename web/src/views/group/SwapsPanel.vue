<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { computed, onMounted, ref } from 'vue'

import { swapApi } from '@/api'
import { errorMessage } from '@/api/client'
import type { Swap } from '@/api/types'
import EmptyState from '@/components/EmptyState.vue'
import { useGroupStore } from '@/stores/group'
import { formatDay, formatTime } from '@/utils/dates'

const store = useGroupStore()
const { group } = storeToRefs(store)

const swaps = ref<Swap[]>([])
const loading = ref(true)
const error = ref('')

const incoming = computed(() =>
  swaps.value.filter((s) => s.status === 'PENDING' && s.targetId === group.value?.myMemberId),
)
const outgoing = computed(() =>
  swaps.value.filter((s) => s.status === 'PENDING' && s.requesterId === group.value?.myMemberId),
)
const history = computed(() => swaps.value.filter((s) => s.status !== 'PENDING'))

const statusLabel: Record<string, string> = {
  ACCEPTED: 'Accepted',
  DECLINED: 'Declined',
  CANCELLED: 'Withdrawn',
}

async function load() {
  if (!group.value) return
  loading.value = true
  swaps.value = await swapApi.list(group.value.id)
  loading.value = false
}

async function act(action: () => Promise<Swap>) {
  error.value = ''
  try {
    await action()
    await load()
  } catch (e) {
    error.value = errorMessage(e, 'Something went wrong')
  }
}

function when(ref: { date: string; departureAt: string }) {
  return `${formatDay(ref.date)} ${formatTime(ref.departureAt, group.value?.timezone ?? 'UTC')}`
}

onMounted(load)
</script>

<template>
  <div class="stack">
    <p v-if="error" class="error">{{ error }}</p>
    <p v-if="loading" class="muted">Loading…</p>
    <EmptyState v-else-if="!swaps.length" title="No swap requests">
      Drivers can ask someone else to take a ride from the ride's page.
    </EmptyState>

    <section v-if="incoming.length" class="stack">
      <h2>Asked of you</h2>
      <article v-for="s in incoming" :key="s.id" class="card stack">
        <p>
          <strong>{{ s.requesterName }}</strong> asks you to drive
          <strong>{{ s.ride.tripName }}</strong> on {{ when(s.ride) }}
          <template v-if="s.offeredRide">
            and will take your <strong>{{ s.offeredRide.tripName }}</strong> on
            {{ when(s.offeredRide) }}
          </template>
          .
        </p>
        <p v-if="s.message" class="muted">“{{ s.message }}”</p>
        <div class="row">
          <button type="button" class="primary" @click="act(() => swapApi.accept(s.id))">
            Accept
          </button>
          <button type="button" @click="act(() => swapApi.decline(s.id))">Decline</button>
        </div>
      </article>
    </section>

    <section v-if="outgoing.length" class="stack">
      <h2>Waiting for an answer</h2>
      <article v-for="s in outgoing" :key="s.id" class="card row">
        <span> {{ s.targetName }} · {{ s.ride.tripName }} on {{ when(s.ride) }} </span>
        <span class="spacer" />
        <button type="button" class="link" @click="act(() => swapApi.cancel(s.id))">
          Withdraw
        </button>
      </article>
    </section>

    <section v-if="history.length" class="stack">
      <h2>Earlier</h2>
      <ul class="history">
        <li v-for="s in history" :key="s.id" class="row">
          <span>{{ s.requesterName }} → {{ s.targetName }}</span>
          <span class="muted">{{ s.ride.tripName }}, {{ formatDay(s.ride.date) }}</span>
          <span class="spacer" />
          <span class="badge neutral">{{ statusLabel[s.status] }}</span>
        </li>
      </ul>
    </section>
  </div>
</template>

<style scoped>
p {
  margin: 0;
}

.history {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 0.4rem;
}
</style>
