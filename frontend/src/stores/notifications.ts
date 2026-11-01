import { defineStore } from 'pinia'
import { ref } from 'vue'
import { notificationsApi } from '@/shared/api/endpoints'
import type { Channel, Notification, Page } from '@/shared/api/types'

/** Store Pinia para histórico e envio manual de notificações (RF-026 a RF-028). */
export const useNotificationsStore = defineStore('notifications', () => {
  const page = ref<Page<Notification> | null>(null)
  const loading = ref(false)

  async function fetchNotifications(params: { processId?: string; channel?: Channel; status?: string; page?: number; size?: number } = {}) {
    loading.value = true
    try {
      const result = await notificationsApi.list(params)
      page.value = result
      return result
    } finally {
      loading.value = false
    }
  }

  async function sendManual(input: { processId: string; channel: Channel; message: string }) {
    return notificationsApi.send(input)
  }

  return { page, loading, fetchNotifications, sendManual }
})
