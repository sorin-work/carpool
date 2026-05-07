import { describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'

import { rideApi } from '@/api'
import UpcomingRides from '@/components/UpcomingRides.vue'

import { ride, stubs } from './fixtures'

vi.mock('@/api', () => ({ rideApi: { mine: vi.fn() } }))

describe('UpcomingRides', () => {
  it('lists the rides I drive and ride', async () => {
    vi.mocked(rideApi.mine).mockResolvedValue([
      {
        groupId: 1,
        groupName: 'School run',
        timezone: 'Europe/Bucharest',
        driving: true,
        ride: ride(),
      },
      {
        groupId: 2,
        groupName: 'Office',
        timezone: 'Europe/Bucharest',
        driving: false,
        ride: ride({ id: 42, tripName: 'Commute', myBooking: 'CONFIRMED' }),
      },
    ])
    const wrapper = mount(UpcomingRides, { global: { stubs } })
    await flushPromises()

    expect(wrapper.findAll('li')).toHaveLength(2)
    expect(wrapper.text()).toContain('You drive')
    expect(wrapper.text()).toContain('seat confirmed')
    expect(wrapper.text()).toContain('07:45')
  })

  it('stays hidden when nothing is planned', async () => {
    vi.mocked(rideApi.mine).mockResolvedValue([])
    const wrapper = mount(UpcomingRides, { global: { stubs } })
    await flushPromises()

    expect(wrapper.find('section').exists()).toBe(false)
  })
})
