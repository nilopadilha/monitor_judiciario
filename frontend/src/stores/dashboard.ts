import { defineStore } from 'pinia'
import { ref } from 'vue'
import { dashboardApi } from '@/shared/api/endpoints'
import type { DashboardSummary } from '@/shared/api/types'

/** Store Pinia para resumo e indicadores do painel (RF-029). */
export const useDashboardStore = defineStore('dashboard', () => {
  const summary = ref<DashboardSummary | null>(null)
  const loading = ref(false)

  async function fetchSummary() {
    loading.value = true
    try {
      const result = await dashboardApi.summary()
      summary.value = result
      return result
    } finally {
      loading.value = false
    }
  }

  return { summary, loading, fetchSummary }
})
