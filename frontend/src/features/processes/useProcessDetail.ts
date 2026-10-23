import { ref, watch, type Ref } from 'vue'
import { processesApi, publicationsApi } from '@/shared/api/endpoints'
import { toProblem } from '@/shared/api/problem'
import type { Movement, Problem, Process, Publication } from '@/shared/api/types'

/** Carrega processo, linha do tempo e publicações não lidas; permite marcar todas como lidas. */
export function useProcessDetail(processId: Ref<string | null | undefined>) {
  const process = ref<Process | null>(null)
  const movements = ref<Movement[]>([])
  const unread = ref<Publication[]>([])
  const loading = ref(false)
  const marking = ref(false)
  const problem = ref<Problem | null>(null)

  async function load() {
    const id = processId.value
    if (!id) {
      process.value = null
      movements.value = []
      unread.value = []
      return
    }
    loading.value = true
    problem.value = null
    try {
      const [p, m, u] = await Promise.all([
        processesApi.get(id),
        processesApi.movements(id),
        publicationsApi.list({ processId: id, unread: true, size: 100 }),
      ])
      process.value = p
      movements.value = m.content
      unread.value = u.content
    } catch (error) {
      problem.value = toProblem(error)
    } finally {
      loading.value = false
    }
  }

  async function markAllRead() {
    marking.value = true
    problem.value = null
    try {
      await Promise.all(unread.value.map((p) => publicationsApi.markRead(p.id)))
      await load()
    } catch (error) {
      problem.value = toProblem(error)
    } finally {
      marking.value = false
    }
  }

  watch(processId, load, { immediate: true })

  return { process, movements, unread, loading, marking, problem, load, markAllRead }
}
