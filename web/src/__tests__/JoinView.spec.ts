import { describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'

import { groupApi } from '@/api'
import { ApiError } from '@/api/client'
import JoinView from '@/views/JoinView.vue'

vi.mock('@/api', () => ({ groupApi: { preview: vi.fn(), join: vi.fn() } }))

async function mountAt(code: string) {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/join/:code', component: JoinView },
      { path: '/groups/:id', component: { template: '<p>group</p>' } },
      { path: '/groups', component: { template: '<p>groups</p>' } },
    ],
  })
  router.push(`/join/${code}`)
  await router.isReady()
  const wrapper = mount(JoinView, { global: { plugins: [router] } })
  await flushPromises()
  return { wrapper, router }
}

describe('JoinView', () => {
  it('shows the group and joins on request', async () => {
    vi.mocked(groupApi.preview).mockResolvedValue({
      name: 'Office',
      memberCount: 3,
      alreadyMember: false,
    })
    vi.mocked(groupApi.join).mockResolvedValue({ id: 5 } as Awaited<
      ReturnType<typeof groupApi.join>
    >)
    const { wrapper, router } = await mountAt('ABCD2345')

    expect(wrapper.text()).toContain('Office')
    expect(wrapper.text()).toContain('3 members')
    expect(groupApi.join).not.toHaveBeenCalled()

    await wrapper.get('button').trigger('click')
    await flushPromises()

    expect(groupApi.join).toHaveBeenCalledWith('ABCD2345')
    expect(router.currentRoute.value.path).toBe('/groups/5')
  })

  it('explains a dead link', async () => {
    vi.mocked(groupApi.preview).mockRejectedValue(new ApiError(404, 'Invite code not found'))
    const { wrapper } = await mountAt('OLDCODE2')

    expect(wrapper.text()).toContain('no longer valid')
  })
})
