import { describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'

import { rideApi } from '@/api'
import { ApiError } from '@/api/client'
import type { Booking, TripInput, Weekday } from '@/api/types'
import DayPicker from '@/components/DayPicker.vue'
import EmptyState from '@/components/EmptyState.vue'
import RideActions from '@/components/RideActions.vue'
import RideCard from '@/components/RideCard.vue'
import TripForm from '@/components/TripForm.vue'

import { member, ride, stubs } from './fixtures'

vi.mock('@/api', () => ({
  rideApi: {
    requestSeat: vi.fn().mockResolvedValue({}),
    cancelSeat: vi.fn().mockResolvedValue(undefined),
    drive: vi.fn().mockResolvedValue({}),
    stopDriving: vi.fn().mockResolvedValue({}),
  },
}))

describe('DayPicker', () => {
  it('toggles days and keeps them in week order', async () => {
    const wrapper = mount(DayPicker, {
      props: {
        modelValue: ['FRIDAY'] as Weekday[],
        'onUpdate:modelValue': (v: Weekday[]) => wrapper.setProps({ modelValue: v }),
      },
    })

    await wrapper.findAll('button')[0]!.trigger('click')
    expect(wrapper.props('modelValue')).toEqual(['MONDAY', 'FRIDAY'])

    await wrapper.findAll('button')[4]!.trigger('click')
    expect(wrapper.props('modelValue')).toEqual(['MONDAY'])
  })

  it('marks selected days as pressed', () => {
    const wrapper = mount(DayPicker, { props: { modelValue: ['TUESDAY'] as Weekday[] } })
    expect(wrapper.findAll('button')[1]!.attributes('aria-pressed')).toBe('true')
    expect(wrapper.findAll('button')[0]!.attributes('aria-pressed')).toBe('false')
  })
})

describe('RideCard', () => {
  it('shows the driver and free seats', () => {
    const wrapper = mount(RideCard, {
      props: {
        ride: ride({ driver: { memberId: 1, displayName: 'Ana' } }),
        timeZone: 'Europe/Bucharest',
        groupId: 1,
      },
      global: { stubs },
    })
    expect(wrapper.text()).toContain('07:45')
    expect(wrapper.text()).toContain('Ana drives')
    expect(wrapper.text()).toContain('2 of 3 seats free')
  })

  it('asks for a driver when there is none', () => {
    const wrapper = mount(RideCard, {
      props: { ride: ride(), timeZone: 'Europe/Bucharest', groupId: 1 },
      global: { stubs },
    })
    expect(wrapper.text()).toContain('Needs a driver')
  })

  it('marks cancelled rides', () => {
    const wrapper = mount(RideCard, {
      props: { ride: ride({ status: 'CANCELLED' }), timeZone: 'UTC', groupId: 1 },
      global: { stubs },
    })
    expect(wrapper.text()).toContain('Cancelled')
    expect(wrapper.classes()).toContain('cancelled')
  })

  it('shows my booking', () => {
    const wrapper = mount(RideCard, {
      props: { ride: ride({ myBooking: 'WAITLISTED' }), timeZone: 'UTC', groupId: 1 },
      global: { stubs },
    })
    expect(wrapper.text()).toContain('On the waitlist')
  })
})

describe('RideActions', () => {
  it('asks for a seat', async () => {
    const wrapper = mount(RideActions, { props: { ride: ride(), me: member() } })

    await wrapper.get('button.primary').trigger('click')
    await flushPromises()

    expect(rideApi.requestSeat).toHaveBeenCalledWith(41)
    expect(wrapper.emitted('changed')).toHaveLength(1)
  })

  it('sends one request for a double click', async () => {
    vi.mocked(rideApi.requestSeat).mockClear()
    let finish: () => void = () => {}
    vi.mocked(rideApi.requestSeat).mockReturnValueOnce(
      new Promise<Booking>((resolve) => (finish = () => resolve({} as Booking))),
    )
    const wrapper = mount(RideActions, { props: { ride: ride(), me: member() } })

    const button = wrapper.get('button.primary')
    await button.trigger('click')
    await button.trigger('click')
    expect(button.attributes('disabled')).toBeDefined()
    finish()
    await flushPromises()

    expect(rideApi.requestSeat).toHaveBeenCalledTimes(1)
  })

  it('offers drivers the wheel when nobody drives', () => {
    const wrapper = mount(RideActions, {
      props: { ride: ride(), me: member({ canDrive: true, carSeats: 3 }) },
    })
    expect(wrapper.text()).toContain("I'll drive")
  })

  it('lets the driver step down', () => {
    const wrapper = mount(RideActions, {
      props: {
        ride: ride({ driver: { memberId: 7, displayName: 'Ben' } }),
        me: member({ canDrive: true, carSeats: 3 }),
      },
    })
    expect(wrapper.text()).toContain("I can't drive")
    expect(wrapper.text()).not.toContain('Ask for a seat')
  })

  it('shows nothing for rides that left', () => {
    const wrapper = mount(RideActions, {
      props: { ride: ride({ departureAt: '2020-01-01T07:00:00Z' }), me: member() },
    })
    expect(wrapper.find('button').exists()).toBe(false)
  })

  it('shows the server error', async () => {
    vi.mocked(rideApi.requestSeat).mockRejectedValueOnce(
      new ApiError(409, 'This ride has already left'),
    )
    const wrapper = mount(RideActions, { props: { ride: ride(), me: member() } })

    await wrapper.get('button.primary').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('This ride has already left')
  })
})

describe('TripForm', () => {
  it('submits the trip', async () => {
    const submit = vi.fn<(input: TripInput) => Promise<void>>().mockResolvedValue()
    const wrapper = mount(TripForm, { props: { submit } })

    await wrapper.get('input[placeholder="Morning school run"]').setValue('School run')
    await wrapper.get('input[placeholder="12 Elm St"]').setValue('Elm St')
    await wrapper.get('input[placeholder="Riverside Primary"]').setValue('School')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(submit).toHaveBeenCalledOnce()
    const input = submit.mock.calls[0]![0]
    expect(input.name).toBe('School run')
    expect(input.days).toHaveLength(5)
    expect(input.endsOn).toBeNull()
  })

  it('shows field errors from the server', async () => {
    const submit = vi
      .fn<(input: TripInput) => Promise<void>>()
      .mockRejectedValue(
        new ApiError(400, 'Validation failed', { distanceKm: 'must be greater than 0' }),
      )
    const wrapper = mount(TripForm, { props: { submit } })

    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('must be greater than 0')
  })

  it('needs at least one day', async () => {
    const submit = vi.fn<(input: TripInput) => Promise<void>>()
    const wrapper = mount(TripForm, {
      props: {
        submit,
        trip: {
          id: 1,
          groupId: 1,
          name: 'x',
          origin: 'a',
          destination: 'b',
          departureTime: '07:45:00',
          days: [],
          distanceKm: 2,
          startsOn: '2026-03-02',
          active: true,
        },
      },
    })

    await wrapper.get('form').trigger('submit')

    expect(submit).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('Pick at least one day')
  })
})

describe('EmptyState', () => {
  it('renders the title and the hint', () => {
    const wrapper = mount(EmptyState, {
      props: { title: 'Nothing here' },
      slots: { default: 'Add one.' },
    })
    expect(wrapper.text()).toContain('Nothing here')
    expect(wrapper.text()).toContain('Add one.')
  })
})
