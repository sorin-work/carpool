<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { notificationApi } from '@/api'
import type { Notification } from '@/api/types'
import EmptyState from '@/components/EmptyState.vue'
import { useNotificationStore } from '@/stores/notifications'
import { renderMessage } from '@/utils/message'

const router = useRouter()
const badge = useNotificationStore()

const items = ref<Notification[]>([])
const page = ref(0)
const hasNext = ref(false)
const loading = ref(true)

async function load(next = 0) {
  loading.value = true
  const result = await notificationApi.list(next)
  items.value = next === 0 ? result.items : [...items.value, ...result.items]
  page.value = result.page
  hasNext.value = result.hasNext
  loading.value = false
}

async function open(n: Notification) {
  if (!n.read) {
    await notificationApi.markRead(n.id)
    n.read = true
    badge.refresh()
  }
  if (n.link) {
    router.push(n.link)
  }
}

async function readAll() {
  await notificationApi.markAllRead()
  items.value.forEach((n) => (n.read = true))
  badge.refresh()
}

function when(instant: string) {
  return new Date(instant).toLocaleString('en-GB', {
    weekday: 'short',
    day: 'numeric',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
  })
}

onMounted(() => load())
</script>

<template>
  <section class="stack">
    <div class="row">
      <h1>Notifications</h1>
      <span class="spacer" />
      <button v-if="items.some((n) => !n.read)" type="button" class="link" @click="readAll">
        Mark all as read
      </button>
    </div>

    <EmptyState v-if="!loading && !items.length" title="Nothing new">
      Cancellations, seat confirmations and swap requests show up here.
    </EmptyState>

    <ul class="list">
      <li v-for="n in items" :key="n.id">
        <button type="button" class="item card" :class="{ unread: !n.read }" @click="open(n)">
          <!-- eslint-disable-next-line vue/no-v-html -->
          <span class="message" v-html="renderMessage(n.message)" />
          <span class="muted when">{{ when(n.createdAt) }}</span>
        </button>
      </li>
    </ul>

    <button v-if="hasNext" type="button" :disabled="loading" @click="load(page + 1)">
      Load more
    </button>
  </section>
</template>

<style scoped>
.list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 0.5rem;
}

.item {
  width: 100%;
  text-align: left;
  display: grid;
  gap: 0.2rem;
  padding: 0.8rem 1rem;
}

.item.unread {
  border-left: 4px solid var(--accent);
}

.when {
  font-size: 0.85rem;
}
</style>
