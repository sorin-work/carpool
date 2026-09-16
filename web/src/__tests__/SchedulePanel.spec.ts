import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'

import { absenceApi, rideApi } from '@/api'
import { useGroupStore } from '@/stores/group'
import SchedulePanel from '@/views/group/SchedulePanel.vue'

import { member, ride } from './fixtures'

vi.mock('@/api', () => ({
  rideApi: { schedule: vi.fn() },
  absenceApi: { list: vi.fn().mockResolvedValue([]) },
}))

async function mountPanel(week = '2026-03-02') {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/groups/:groupId', component: SchedulePanel }],
  })
  router.push(`/groups/1?week=${week}`)
  await router.isReady()
  const wrapper = mount(SchedulePanel, {
    global: { plugins: [router], stubs: { RouterLink: { template: '<a><slot /></a>' } } },
  })
  await flushPromises()
  return { wrapper, router }
}

describe('SchedulePanel', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    useGroupStore().replace({
      id: 1,
      name: 'School run',
      timezone: 'Europe/Bucharest',
      inviteCode: 'ABCDEFGH',
      costPerKm: 0,
      currency: 'EUR',
      myRole: 'MEMBER',
      myMemberId: 7,
      members: [member()],
    })
    vi.mocked(rideApi.schedule).mockReset()
  })

  it('groups rides by day', async () => {
    vi.mocked(rideApi.schedule).mockResolvedValue([
      ride({ id: 1, date: '2026-03-03', departureAt: '2026-03-03T05:45:00Z' }),
      ride({ id: 2, date: '2026-03-03', departureAt: '2026-03-03T13:30:00Z', tripName: 'Pickup' }),
      ride({ id: 3, date: '2026-03-04', departureAt: '2026-03-04T05:45:00Z' }),
    ])
    const { wrapper } = await mountPanel()

    expect(rideApi.schedule).toHaveBeenCalledWith(1, '2026-03-02', '2026-03-09')
    const days = wrapper.findAll('section.day')
    expect(days).toHaveLength(2)
    expect(days[0]!.findAll('article')).toHaveLength(2)
  })

  it('moves a week forward and remembers it in the url', async () => {
    vi.mocked(rideApi.schedule).mockResolvedValue([])
    const { wrapper, router } = await mountPanel()

    await wrapper.get('button[aria-label="Next week"]').trigger('click')
    await flushPromises()

    expect(rideApi.schedule).toHaveBeenLastCalledWith(1, '2026-03-09', '2026-03-16')
    expect(router.currentRoute.value.query.week).toBe('2026-03-09')
  })

  it('lists who is away this week', async () => {
    vi.mocked(rideApi.schedule).mockResolvedValue([])
    vi.mocked(absenceApi.list).mockResolvedValueOnce([
      { id: 1, memberId: 2, displayName: 'Elena', startsOn: '2026-03-05', endsOn: '2026-03-12' },
      { id: 2, memberId: 3, displayName: 'Radu', startsOn: '2026-04-01', endsOn: '2026-04-02' },
    ])
    const { wrapper } = await mountPanel()

    expect(wrapper.get('.away').text()).toContain('Elena')
    expect(wrapper.get('.away').text()).not.toContain('Radu')
  })

  it('says when there is nothing this week', async () => {
    vi.mocked(rideApi.schedule).mockResolvedValue([])
    const { wrapper } = await mountPanel()

    expect(wrapper.text()).toContain('No rides this week')
  })
})
