import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { authApi } from '@/shared/api/endpoints'
import { configureAuth } from '@/shared/api/http'
import type { User } from '@/shared/api/types'

const REFRESH_KEY = 'mj.refresh'

function readRefresh(): string | null {
  try {
    return localStorage.getItem(REFRESH_KEY)
  } catch {
    return null
  }
}

function writeRefresh(value: string | null) {
  try {
    if (value) localStorage.setItem(REFRESH_KEY, value)
    else localStorage.removeItem(REFRESH_KEY)
  } catch {
    // armazenamento indisponível: a sessão dura até fechar a aba
  }
}

/**
 * Sessão do usuário. O access token fica só em memória; o refresh token (rotativo, de uso único) sobrevive ao recarregar
 * a página. Ver README › Segurança do frontend.
 */
export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref<string | null>(null)
  const refreshToken = ref<string | null>(readRefresh())
  const user = ref<User | null>(null)
  let initialized: Promise<void> | null = null

  const isAuthenticated = computed(() => !!accessToken.value && !!user.value)
  const role = computed(() => user.value?.role)
  const isStaff = computed(() => role.value === 'ADMIN' || role.value === 'MANAGER')

  function setTokens(access: string, refresh: string) {
    accessToken.value = access
    refreshToken.value = refresh
    writeRefresh(refresh)
  }

  function clear() {
    accessToken.value = null
    refreshToken.value = null
    user.value = null
    writeRefresh(null)
  }

  async function refresh(): Promise<string> {
    if (!refreshToken.value) throw new Error('Sem sessão')
    const pair = await authApi.refresh(refreshToken.value)
    setTokens(pair.accessToken, pair.refreshToken)
    return pair.accessToken
  }

  async function login(email: string, password: string) {
    const pair = await authApi.login(email, password)
    setTokens(pair.accessToken, pair.refreshToken)
    user.value = await authApi.me()
  }

  async function logout() {
    const token = refreshToken.value
    clear()
    if (token) {
      await authApi.revoke(token).catch(() => undefined)
    }
  }

  /** Restaura a sessão a partir do refresh token, uma única vez por carregamento. */
  function init(): Promise<void> {
    initialized ??= (async () => {
      if (!refreshToken.value) return
      try {
        await refresh()
        user.value = await authApi.me()
      } catch {
        clear()
      }
    })()
    return initialized
  }

  configureAuth({
    accessToken: () => accessToken.value,
    refresh,
    onAuthFailure: () => {
      clear()
      if (typeof window !== 'undefined' && !window.location.pathname.startsWith('/login')) {
        window.location.assign(`/login?expired=1`)
      }
    },
  })

  return { accessToken, refreshToken, user, isAuthenticated, role, isStaff, login, logout, refresh, init }
})
