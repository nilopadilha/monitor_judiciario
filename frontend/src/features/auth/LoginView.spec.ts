import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createTestingPinia } from './testing'

const push = vi.fn()
vi.mock('vue-router', () => ({ useRoute: () => ({ query: { redirect: '/processos' } }), useRouter: () => ({ push }) }))

import LoginView from './LoginView.vue'
import { useAuthStore } from '@/stores/auth'

describe('LoginView', () => {
  beforeEach(() => push.mockReset())

  it('logs in and follows a safe redirect', async () => {
    const pinia = createTestingPinia()
    const wrapper = mount(LoginView, { global: { plugins: [pinia] } })
    const auth = useAuthStore()
    auth.login = vi.fn().mockResolvedValue(undefined)

    await wrapper.find('#email').setValue('gestor@x.com')
    await wrapper.find('#password').setValue('senha')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(auth.login).toHaveBeenCalledWith('gestor@x.com', 'senha')
    expect(push).toHaveBeenCalledWith('/processos')
  })

  it('shows the API problem on failure', async () => {
    const pinia = createTestingPinia()
    const wrapper = mount(LoginView, { global: { plugins: [pinia] } })
    useAuthStore().login = vi.fn().mockRejectedValue(new Error('E-mail ou senha inválidos.'))

    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.find('[role="alert"]').text()).toContain('E-mail ou senha inválidos.')
    expect(push).not.toHaveBeenCalled()
  })
})
