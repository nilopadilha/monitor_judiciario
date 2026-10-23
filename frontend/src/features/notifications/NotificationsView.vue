<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import Card from '@/shared/ui/Card.vue'
import Pagination from '@/shared/ui/Pagination.vue'
import ProblemAlert from '@/shared/ui/ProblemAlert.vue'
import StatusBadge from '@/shared/ui/StatusBadge.vue'
import { notificationsApi } from '@/shared/api/endpoints'
import { toProblem } from '@/shared/api/problem'
import { formatDateTime } from '@/shared/lib/format'
import type { Channel, Notification, Page, Problem } from '@/shared/api/types'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()

const page = ref<Page<Notification> | null>(null)
const channel = ref<Channel | ''>('')
const status = ref<'SENT' | 'FAILED' | ''>('')
const current = ref(0)
const problem = ref<Problem | null>(null)

async function load() {
  problem.value = null
  try {
    page.value = await notificationsApi.list({ channel: channel.value || undefined, status: status.value || undefined, page: current.value })
  } catch (error) {
    problem.value = toProblem(error)
  }
}

watch([channel, status], () => { current.value = 0; load() })
watch(current, load)
onMounted(load)
</script>

<template>
  <div class="flex flex-col gap-8">
    <h1 class="text-title">Notificações</h1>
    <Card class="flex flex-col gap-4">
      <div class="flex flex-wrap gap-2">
        <label class="sr-only" for="canal">Canal</label>
        <select id="canal" v-model="channel" class="h-11 rounded-md border border-input bg-card px-3">
          <option value="">Todos os canais</option>
          <option value="EMAIL">E-mail</option>
          <option value="WHATSAPP">WhatsApp</option>
        </select>
        <label class="sr-only" for="st">Status</label>
        <select id="st" v-model="status" class="h-11 rounded-md border border-input bg-card px-3">
          <option value="">Todos os status</option>
          <option value="SENT">Entregues</option>
          <option value="FAILED">Com falha</option>
        </select>
      </div>
      <ProblemAlert :problem="problem" />
      <ul>
        <li v-for="n in page?.content ?? []" :key="n.id" class="flex flex-wrap items-center gap-x-4 gap-y-1 border-t border-border py-3">
          <span class="w-36 text-caption">{{ formatDateTime(n.createdAt) }}</span>
          <span class="w-20 font-medium">{{ n.channel === 'EMAIL' ? 'E-mail' : 'WhatsApp' }}</span>
          <span class="min-w-0 flex-[1_1_240px]">
            {{ n.clientName }} <span class="font-mono text-[13px] text-muted-foreground">{{ n.processNumber }}</span>
            <span class="block text-caption">{{ n.subject }} · para {{ n.recipient }}</span>
          </span>
          <StatusBadge :situation="n.status === 'SENT' ? 'SENT' : n.status === 'FAILED' ? 'FAILED_DELIVERY' : 'PENDING'" />
          <span v-if="n.errorMessage && auth.isStaff" class="basis-full text-[13px] text-destructive">{{ n.errorMessage }}</span>
        </li>
        <li v-if="page && page.content.length === 0" class="border-t border-border py-3 text-muted-foreground">Nenhuma notificação neste filtro.</li>
      </ul>
      <Pagination v-if="page" :page="page.page" :total-pages="page.totalPages" :total-elements="page.totalElements" @change="current = $event" />
    </Card>
  </div>
</template>
