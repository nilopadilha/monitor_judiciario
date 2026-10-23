import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import Button from './Button.vue'
import StatusBadge from './StatusBadge.vue'
import ProblemAlert from './ProblemAlert.vue'
import Pagination from './Pagination.vue'

describe('ui', () => {
  it('status badge always shows a word, not only color', () => {
    expect(mount(StatusBadge, { props: { situation: 'NEW_PUBLICATION' } }).text()).toBe('Nova publicação')
    expect(mount(StatusBadge, { props: { situation: 'FAILED' } }).text()).toBe('Falhou')
    expect(mount(StatusBadge, { props: { situation: 'SENT' } }).text()).toBe('Entregue')
  })

  it('button shows loading state and is disabled while loading', async () => {
    const wrapper = mount(Button, { props: { loading: true }, slots: { default: 'Salvar' } })
    expect(wrapper.attributes('disabled')).toBeDefined()
    expect(wrapper.attributes('aria-busy')).toBe('true')
    expect(wrapper.classes()).toContain('h-11')
  })

  it('problem alert lists field errors and support code', () => {
    const wrapper = mount(ProblemAlert, {
      props: { problem: { title: 'Dados inválidos', detail: 'Verifique', errors: { email: 'inválido' }, correlationId: 'c-1' } },
    })
    expect(wrapper.attributes('role')).toBe('alert')
    expect(wrapper.text()).toContain('email: inválido')
    expect(wrapper.text()).toContain('c-1')
    expect(mount(ProblemAlert, { props: { problem: null } }).html()).not.toContain('alert')
  })

  it('pagination emits page changes', async () => {
    const wrapper = mount(Pagination, { props: { page: 0, totalPages: 3, totalElements: 50 } })
    const buttons = wrapper.findAll('button')
    expect(buttons[0]?.attributes('disabled')).toBeDefined()
    await buttons[1]?.trigger('click')
    expect(wrapper.emitted('change')?.[0]).toEqual([1])
  })
})
