import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'

import { costApi } from '@/api'
import type { CostReport } from '@/api/types'
import { useGroupStore } from '@/stores/group'
import CostsPanel from '@/views/group/CostsPanel.vue'

import { member, stubs } from './fixtures'

vi.mock('@/api', () => ({
  costApi: { get: vi.fn(), recordPayment: vi.fn().mockResolvedValue({}) },
}))

const report = (overrides: Partial<CostReport> = {}): CostReport => ({
  from: '2026-03-01',
  to: '2026-04-01',
  currency: 'EUR',
  costPerKm: 0.24,
  totalCents: 480,
  balances: [
    {
      memberId: 1,
      displayName: 'Ana',
      earnedCents: 280,
      spentCents: 0,
      settledCents: 0,
      balanceCents: 280,
    },
    {
      memberId: 7,
      displayName: 'Ben',
      earnedCents: 0,
      spentCents: 200,
      settledCents: 0,
      balanceCents: -200,
    },
  ],
  settlements: [
    { fromMemberId: 7, fromName: 'Ben', toMemberId: 1, toName: 'Ana', amountCents: 200 },
  ],
  payments: [],
  ...overrides,
})

describe('CostsPanel', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    useGroupStore().replace({
      id: 1,
      name: 'Office',
      timezone: 'Europe/Bucharest',
      inviteCode: 'ABCDEFGH',
      costPerKm: 0.24,
      currency: 'EUR',
      myRole: 'MEMBER',
      myMemberId: 7,
      members: [member()],
    })
  })

  it('shows balances and who pays whom', async () => {
    vi.mocked(costApi.get).mockResolvedValue(report())
    const wrapper = mount(CostsPanel, { global: { stubs } })
    await flushPromises()

    expect(wrapper.text()).toContain('€4.80')
    expect(wrapper.find('td.neg').text()).toBe('-€2.00')
    expect(wrapper.text()).toContain('Ben pays Ana €2.00')
  })

  it('records a payment I am part of', async () => {
    vi.mocked(costApi.get).mockResolvedValue(report())
    const wrapper = mount(CostsPanel, { global: { stubs } })
    await flushPromises()

    await wrapper.get('.transfers button').trigger('click')
    await flushPromises()

    expect(costApi.recordPayment).toHaveBeenCalledWith(1, 7, 1, 200)
  })

  it('explains when nothing is owed', async () => {
    vi.mocked(costApi.get).mockResolvedValue(
      report({ totalCents: 0, balances: [], settlements: [] }),
    )
    const wrapper = mount(CostsPanel, { global: { stubs } })
    await flushPromises()

    expect(wrapper.text()).toContain('Nothing to share')
  })
})
