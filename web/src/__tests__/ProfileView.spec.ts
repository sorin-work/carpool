import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'

import { authApi } from '@/api'
import { ApiError } from '@/api/client'
import { useAuthStore } from '@/stores/auth'
import ProfileView from '@/views/ProfileView.vue'

vi.mock('@/api', () => ({
  authApi: { updateMe: vi.fn(), changePassword: vi.fn() },
}))

describe('ProfileView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    useAuthStore().user = { id: 1, email: 'ana@example.com', displayName: 'Ana', createdAt: '' }
  })

  it('renames the user everywhere', async () => {
    vi.mocked(authApi.updateMe).mockResolvedValue({
      id: 1,
      email: 'ana@example.com',
      displayName: 'Ana P.',
      createdAt: '',
    })
    const wrapper = mount(ProfileView)

    await wrapper.findAll('input')[0]!.setValue('Ana P.')
    await wrapper.findAll('form')[0]!.trigger('submit')
    await flushPromises()

    expect(authApi.updateMe).toHaveBeenCalledWith('Ana P.')
    expect(useAuthStore().user?.displayName).toBe('Ana P.')
  })

  it('shows a wrong current password', async () => {
    vi.mocked(authApi.changePassword).mockRejectedValue(
      new ApiError(400, 'Your current password is not correct'),
    )
    const wrapper = mount(ProfileView)

    await wrapper.findAll('form')[1]!.trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('Your current password is not correct')
  })
})
