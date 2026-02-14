import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

import { groupApi } from '@/api'
import type { GroupDetail, Member } from '@/api/types'

/** The group currently open in the group pages. */
export const useGroupStore = defineStore('group', () => {
  const group = ref<GroupDetail | null>(null)
  const loading = ref(false)
  const error = ref('')

  const me = computed<Member | undefined>(() =>
    group.value?.members.find((m) => m.id === group.value?.myMemberId),
  )
  const isManager = computed(
    () => group.value?.myRole === 'OWNER' || group.value?.myRole === 'ADMIN',
  )
  const drivers = computed(() => group.value?.members.filter((m) => m.canDrive) ?? [])

  async function load(id: number) {
    loading.value = true
    error.value = ''
    try {
      group.value = await groupApi.get(id)
    } catch {
      group.value = null
      error.value = 'This group does not exist or you are not a member.'
    } finally {
      loading.value = false
    }
  }

  function replace(detail: GroupDetail) {
    group.value = detail
  }

  function updateMember(member: Member) {
    if (group.value) {
      group.value.members = group.value.members.map((m) => (m.id === member.id ? member : m))
    }
  }

  return { group, loading, error, me, isManager, drivers, load, replace, updateMember }
})
