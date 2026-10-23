import { createPinia, setActivePinia } from 'pinia'

/** Pinia isolada por teste (sem dependência extra de @pinia/testing). */
export function createTestingPinia() {
  const pinia = createPinia()
  setActivePinia(pinia)
  return pinia
}
