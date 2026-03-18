import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

import { authApi } from '@/api'
import { getToken } from '@/api/client'
import { useAuthStore } from '@/stores/auth'

vi.mock('@/api', () => ({
  authApi: { login: vi.fn(), register: vi.fn(), me: vi.fn() },
}))

const user = {
  id: 1,
  email: 'ana@example.com',
  displayName: 'Ana',
  createdAt: '2026-03-01T00:00:00Z',
}

describe('auth store', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
    vi.mocked(authApi.login).mockReset()
    vi.mocked(authApi.me).mockReset()
  })

  it('keeps the token after signing in', async () => {
    vi.mocked(authApi.login).mockResolvedValue({ token: 'tok', expiresAt: '', user })
    const auth = useAuthStore()

    await auth.login('ana@example.com', 'secret-password')

    expect(auth.isSignedIn).toBe(true)
    expect(auth.user?.displayName).toBe('Ana')
    expect(getToken()).toBe('tok')
  })

  it('restores the profile for a stored token', async () => {
    localStorage.setItem('carpool.token', 'tok')
    vi.mocked(authApi.me).mockResolvedValue(user)
    const auth = useAuthStore()

    await auth.restore()

    expect(auth.user?.email).toBe('ana@example.com')
  })

  it('signs out when the stored token no longer works', async () => {
    localStorage.setItem('carpool.token', 'old')
    vi.mocked(authApi.me).mockRejectedValue(new Error('401'))
    const auth = useAuthStore()

    await auth.restore()

    expect(auth.isSignedIn).toBe(false)
    expect(getToken()).toBeNull()
  })

  it('forgets everything on logout', async () => {
    vi.mocked(authApi.login).mockResolvedValue({ token: 'tok', expiresAt: '', user })
    const auth = useAuthStore()
    await auth.login('ana@example.com', 'secret-password')

    auth.logout()

    expect(auth.user).toBeNull()
    expect(getToken()).toBeNull()
  })
})
