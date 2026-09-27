import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'

import { rotationApi } from '@/api'
import { useGroupStore } from '@/stores/group'
import RotationPanel from '@/views/group/RotationPanel.vue'

import { member } from './fixtures'

vi.mock('@/api', () => ({
  rotationApi: { preview: vi.fn(), apply: vi.fn() },
  absenceApi: { list: vi.fn().mockResolvedValue([]) },
}))

function setGroup(role: 'ADMIN' | 'MEMBER', canDrive = true) {
  useGroupStore().replace({
    id: 1,
    name: 'School run',
    timezone: 'Europe/Bucharest',
    inviteCode: 'ABCDEFGH',
    costPerKm: 0,
    currency: 'EUR',
    myRole: role,
    myMemberId: 7,
    members: [member({ canDrive, carSeats: canDrive ? 3 : 0 })],
  })
}

const proposals = [
  {
    rideId: 1,
    tripName: 'School run',
    date: '2026-03-03',
    departureAt: '2026-03-03T05:45:00Z',
    memberId: 7,
    displayName: 'Ben',
  },
  { rideId: 2, tripName: 'School run', date: '2026-03-04', departureAt: '2026-03-04T05:45:00Z' },
]

describe('RotationPanel', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.mocked(rotationApi.preview).mockReset().mockResolvedValue(proposals)
    vi.mocked(rotationApi.apply).mockReset()
  })

  it('shows the proposal with a count per driver', async () => {
    setGroup('MEMBER')
    const wrapper = mount(RotationPanel)
    await flushPromises()

    expect(wrapper.findAll('tbody tr')).toHaveLength(2)
    expect(wrapper.text()).toContain('Ben: 1')
    expect(wrapper.text()).toContain('1 without a driver')
    expect(wrapper.text()).not.toContain('Assign these drivers')
  })

  it('lets admins apply it once', async () => {
    setGroup('ADMIN')
    let finish: () => void = () => {}
    vi.mocked(rotationApi.apply).mockReturnValue(
      new Promise((resolve) => (finish = () => resolve([]))),
    )
    const wrapper = mount(RotationPanel)
    await flushPromises()

    const button = wrapper.get('button.primary')
    await button.trigger('click')
    await button.trigger('click')
    finish()
    await flushPromises()

    expect(rotationApi.apply).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('Drivers assigned and notified.')
  })

  it('explains when nobody has a car', async () => {
    setGroup('ADMIN', false)
    const wrapper = mount(RotationPanel)
    await flushPromises()

    expect(wrapper.text()).toContain('Nobody can drive yet')
  })
})
