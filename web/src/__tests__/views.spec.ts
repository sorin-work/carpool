import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'

import { authApi, notificationApi } from '@/api'
import { ApiError } from '@/api/client'
import type { Notification } from '@/api/types'
import LoginView from '@/views/LoginView.vue'
import NotificationsView from '@/views/NotificationsView.vue'

vi.mock('@/api', () => ({
  authApi: { login: vi.fn() },
  notificationApi: {
    list: vi.fn(),
    markRead: vi.fn().mockResolvedValue({}),
    markAllRead: vi.fn().mockResolvedValue({ updated: 1 }),
    unreadCount: vi.fn().mockResolvedValue({ count: 0 }),
  },
}))

function makeRouter() {
  const page = { template: '<p />' }
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/login', component: LoginView },
      { path: '/groups', component: page },
      { path: '/groups/:id/rides/:rideId', component: page },
      { path: '/register', component: page },
    ],
  })
}

describe('LoginView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
  })

  it('signs in and goes to the groups page', async () => {
    vi.mocked(authApi.login).mockResolvedValue({
      token: 't',
      expiresAt: '',
      user: { id: 1, email: 'a@example.com', displayName: 'Ana', createdAt: '' },
    })
    const router = makeRouter()
    router.push('/login')
    await router.isReady()
    const wrapper = mount(LoginView, { global: { plugins: [router] } })

    await wrapper.get('input[type=email]').setValue('a@example.com')
    await wrapper.get('input[type=password]').setValue('long-password')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(authApi.login).toHaveBeenCalledWith('a@example.com', 'long-password')
    expect(router.currentRoute.value.path).toBe('/groups')
  })

  it('does not follow a next link to another site', async () => {
    vi.mocked(authApi.login).mockResolvedValue({
      token: 't',
      expiresAt: '',
      user: { id: 1, email: 'a@example.com', displayName: 'Ana', createdAt: '' },
    })
    const router = makeRouter()
    router.push('/login?next=//evil.example/phish')
    await router.isReady()
    const wrapper = mount(LoginView, { global: { plugins: [router] } })

    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(router.currentRoute.value.path).toBe('/groups')
  })

  it('shows why sign-in failed', async () => {
    vi.mocked(authApi.login).mockRejectedValue(new ApiError(401, 'Invalid email or password'))
    const router = makeRouter()
    router.push('/login')
    await router.isReady()
    const wrapper = mount(LoginView, { global: { plugins: [router] } })

    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(wrapper.get('[role=alert]').text()).toBe('Invalid email or password')
  })
})

describe('NotificationsView', () => {
  beforeEach(() => setActivePinia(createPinia()))

  const page = (items: Notification[], hasNext = false) => ({
    items,
    page: 0,
    size: 20,
    totalItems: items.length,
    totalPages: 1,
    hasNext,
  })

  it('lists notifications with names in bold', async () => {
    vi.mocked(notificationApi.list).mockResolvedValue(
      page([
        {
          id: 1,
          type: 'SEAT_CONFIRMED',
          message: 'You have a seat with **Ana**.',
          createdAt: '2026-03-02T06:00:00Z',
          read: false,
        },
      ]),
    )
    const wrapper = mount(NotificationsView, { global: { plugins: [makeRouter()] } })
    await flushPromises()

    expect(wrapper.find('strong').text()).toBe('Ana')
    expect(wrapper.find('.unread').exists()).toBe(true)
  })

  it('opens the linked page and marks it read', async () => {
    vi.mocked(notificationApi.list).mockResolvedValue(
      page([
        {
          id: 5,
          type: 'RIDE_CANCELLED',
          message: 'Cancelled',
          link: '/groups/1/rides/9',
          createdAt: '2026-03-02T06:00:00Z',
          read: false,
        },
      ]),
    )
    const router = makeRouter()
    const wrapper = mount(NotificationsView, { global: { plugins: [router] } })
    await flushPromises()

    await wrapper.get('button.item').trigger('click')
    await flushPromises()

    expect(notificationApi.markRead).toHaveBeenCalledWith(5)
    expect(router.currentRoute.value.path).toBe('/groups/1/rides/9')
  })

  it('never renders html from the message', async () => {
    vi.mocked(notificationApi.list).mockResolvedValue(
      page([
        {
          id: 9,
          type: 'RIDE_CANCELLED',
          message: '**<img src=x onerror=alert(1)>** was cancelled: <b>bold</b>',
          createdAt: '2026-03-02T06:00:00Z',
          read: false,
        },
      ]),
    )
    const wrapper = mount(NotificationsView, { global: { plugins: [makeRouter()] } })
    await flushPromises()

    expect(wrapper.find('img').exists()).toBe(false)
    expect(wrapper.find('.message b').exists()).toBe(false)
    expect(wrapper.get('.message').text()).toContain('<b>bold</b>')
  })

  it('has a friendly empty state', async () => {
    vi.mocked(notificationApi.list).mockResolvedValue(page([]))
    const wrapper = mount(NotificationsView, { global: { plugins: [makeRouter()] } })
    await flushPromises()

    expect(wrapper.text()).toContain('Nothing new')
  })

  it('loads the next page', async () => {
    vi.mocked(notificationApi.list)
      .mockResolvedValueOnce(
        page(
          [{ id: 2, type: 'X', message: 'Second', createdAt: '2026-03-02T06:00:00Z', read: true }],
          true,
        ),
      )
      .mockResolvedValueOnce({
        ...page([
          { id: 1, type: 'X', message: 'First', createdAt: '2026-03-01T06:00:00Z', read: true },
        ]),
        page: 1,
      })
    const wrapper = mount(NotificationsView, { global: { plugins: [makeRouter()] } })
    await flushPromises()

    await wrapper.get('button:not(.item)').trigger('click')
    await flushPromises()

    expect(notificationApi.list).toHaveBeenLastCalledWith(1)
    expect(wrapper.text()).toContain('First')
    expect(wrapper.text()).toContain('Second')
  })
})
