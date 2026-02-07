import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

import { authApi } from '@/api'
import { getToken, setToken } from '@/api/client'
import type { AuthResponse, User } from '@/api/types'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(getToken())
  const user = ref<User | null>(null)

  const isSignedIn = computed(() => token.value !== null)

  function accept(response: AuthResponse) {
    token.value = response.token
    user.value = response.user
    setToken(response.token)
  }

  async function login(email: string, password: string) {
    accept(await authApi.login(email, password))
  }

  async function register(email: string, displayName: string, password: string) {
    accept(await authApi.register(email, displayName, password))
  }

  /** Loads the profile for a token restored from storage. */
  async function restore() {
    if (token.value && !user.value) {
      try {
        user.value = await authApi.me()
      } catch {
        logout()
      }
    }
  }

  function logout() {
    token.value = null
    user.value = null
    setToken(null)
  }

  return { token, user, isSignedIn, login, register, restore, logout }
})
