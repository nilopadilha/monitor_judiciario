import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { authApi } from '@/shared/api/endpoints'
import { configureAuth } from '@/shared/api/http'
import type { User } from '@/shared/api/types'

const SESSION_HINT_KEY = 'mj.session'
const LEGACY_REFRESH_KEY = 'mj.refresh'

function hasSessionHint(): boolean {
  try {
    localStorage.removeItem(LEGACY_REFRESH_KEY)
    return localStorage.getItem(SESSION_HINT_KEY) === '1'
  } catch {
    return false
  }
}

function writeSessionHint(active: boolean) {
  try {
    localStorage.removeItem(LEGACY_REFRESH_KEY)
    if (active) localStorage.setItem(SESSION_HINT_KEY, '1')
    else localStorage.removeItem(SESSION_HINT_KEY)
  } catch {
    // armazenamento indisponível: a sessão dura até fechar a aba
  }
}

/**
 * Sessão do usuário. O access token fica só em memória e o refresh token rotativo viaja exclusivamente em cookie
 * HttpOnly emitido pela API (nunca armazenado em localStorage/JavaScript).
 */
export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref<string | null>(null)
  const user = ref<User | null>(null)
  let initialized: Promise<void> | null = null

  const isAuthenticated = computed(() => !!accessToken.value && !!user.value)
  const role = computed(() => user.value?.role)
  const isStaff = computed(() => role.value === 'ADMIN' || role.value === 'MANAGER')

  function setAccessToken(access: string) {
    accessToken.value = access
    writeSessionHint(true)
  }

  function clear() {
    accessToken.value = null
    user.value = null
    writeSessionHint(false)
  }

  async function refresh(): Promise<string> {
    const pair = await authApi.refresh()
    setAccessToken(pair.accessToken)
    return pair.accessToken
  }

  async function login(email: string, password: string) {
    const pair = await authApi.login(email, password)
    setAccessToken(pair.accessToken)
    user.value = await authApi.me()
  }

  async function logout() {
    clear()
    await authApi.revoke().catch(() => undefined)
  }

  /** Restaura a sessão a partir do cookie HttpOnly de refresh, uma única vez por carregamento. */
  function init(): Promise<void> {
    initialized ??= (async () => {
      if (!hasSessionHint()) return
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

  return { accessToken, user, isAuthenticated, role, isStaff, login, logout, refresh, init }
})
