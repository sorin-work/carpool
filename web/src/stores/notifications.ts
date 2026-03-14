import { defineStore } from 'pinia'
import { ref } from 'vue'

import { notificationApi } from '@/api'

export const useNotificationStore = defineStore('notifications', () => {
  const unread = ref(0)

  async function refresh() {
    try {
      unread.value = (await notificationApi.unreadCount()).count
    } catch {
      // The badge is not worth an error message; try again on the next refresh.
    }
  }

  return { unread, refresh }
})
