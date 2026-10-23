<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { RefreshCw } from 'lucide-vue-next'
import Button from '@/shared/ui/Button.vue'
import Card from '@/shared/ui/Card.vue'
import ProblemAlert from '@/shared/ui/ProblemAlert.vue'
import StatusBadge from '@/shared/ui/StatusBadge.vue'
import KpiGrid from './components/KpiGrid.vue'
import PublicationsChart from './components/PublicationsChart.vue'
import ProcessTable from '@/features/processes/ProcessTable.vue'
import ProcessPanel from '@/features/processes/ProcessPanel.vue'
import { dashboardApi, monitoringApi, notificationsApi, processesApi } from '@/shared/api/endpoints'
import { toProblem } from '@/shared/api/problem'
import { formatDateTime } from '@/shared/lib/format'
import type { DashboardSummary, Notification, Problem, Process, Situation } from '@/shared/api/types'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()

type Filter = 'ALL' | Extract<Situation, 'NEW_PUBLICATION' | 'FAILED'>
const filters: { id: Filter; label: string }[] = [
  { id: 'ALL', label: 'Todos' },
  { id: 'NEW_PUBLICATION', label: 'Com novidade' },
  { id: 'FAILED', label: 'Com falha' },
]

const summary = ref<DashboardSummary | null>(null)
const processes = ref<Process[]>([])
const notifications = ref<Notification[]>([])
const filter = ref<Filter>('ALL')
const selectedId = ref<string | null>(null)
const loadingProcesses = ref(false)
const running = ref(false)
const problem = ref<Problem | null>(null)
let poll: ReturnType<typeof setInterval> | undefined

const lastRun = computed(() =>
  summary.value?.lastExecution ? formatDateTime(summary.value.lastExecution.startedAt) : 'ainda não executado',
)
const filterCount = (id: Filter) =>
  id === 'ALL'
    ? (summary.value?.monitoredProcesses ?? 0) + (summary.value?.pausedProcesses ?? 0)
    : id === 'NEW_PUBLICATION'
      ? (summary.value?.processesWithNews ?? 0)
      : (summary.value?.processesWithFailedCheck ?? 0)

async function loadSummary() {
  summary.value = await dashboardApi.summary()
}

async function loadProcesses() {
  loadingProcesses.value = true
  try {
    const page = await processesApi.list({ situation: filter.value === 'ALL' ? undefined : filter.value, size: 20 })
    processes.value = page.content
    if (!selectedId.value || !page.content.some((p) => p.id === selectedId.value)) {
      selectedId.value = page.content[0]?.id ?? null
    }
  } finally {
    loadingProcesses.value = false
  }
}

async function loadNotifications() {
  notifications.value = (await notificationsApi.list({ size: 8 })).content
}

async function refreshAll() {
  problem.value = null
  try {
    await Promise.all([loadSummary(), loadProcesses(), loadNotifications()])
  } catch (error) {
    problem.value = toProblem(error)
  }
}

/** Dispara a execução (202) e acompanha até terminar, atualizando o painel. */
async function runMonitoring() {
  running.value = true
  problem.value = null
  try {
    const execution = await monitoringApi.start()
    poll = setInterval(async () => {
      const current = await monitoringApi.get(execution.id)
      if (current.status !== 'RUNNING') {
        clearInterval(poll)
        running.value = false
        await refreshAll()
      }
    }, 1500)
  } catch (error) {
    running.value = false
    problem.value = toProblem(error)
  }
}

watch(filter, loadProcesses)
onMounted(refreshAll)
onUnmounted(() => clearInterval(poll))
</script>

<template>
  <div class="flex flex-col gap-8">
    <header class="flex flex-wrap items-end justify-between gap-4">
      <div class="flex flex-col gap-1">
        <h1 class="text-title">Painel</h1>
        <p v-if="auth.isStaff" class="text-caption">Última execução do monitoramento: {{ lastRun }}</p>
        <p v-else class="text-caption">Os processos são verificados todos os dias às 06:00.</p>
      </div>
      <Button v-if="auth.isStaff" :loading="running" @click="runMonitoring">
        <RefreshCw v-if="!running" aria-hidden="true" />
        {{ running ? 'Consultando DJEN…' : 'Executar monitoramento agora' }}
      </Button>
    </header>

    <ProblemAlert :problem="problem" />
    <KpiGrid :summary="summary" />

    <div class="flex flex-wrap items-start gap-4">
      <Card class="flex min-w-0 flex-[2_1_560px] flex-col gap-4" aria-labelledby="processos-titulo">
        <div class="flex flex-wrap items-center justify-between gap-2">
          <h2 id="processos-titulo" class="text-heading">Processos monitorados</h2>
          <div role="group" aria-label="Filtrar processos" class="flex flex-wrap gap-1">
            <Button
              v-for="f in filters"
              :key="f.id"
              size="sm"
              :variant="filter === f.id ? 'default' : 'outline'"
              :aria-pressed="filter === f.id"
              class="h-11"
              @click="filter = f.id"
            >
              {{ f.label }} · {{ filterCount(f.id) }}
            </Button>
          </div>
        </div>
        <ProcessTable :processes="processes" :selected-id="selectedId" :loading="loadingProcesses" @select="selectedId = $event.id" />
      </Card>
      <ProcessPanel class="min-w-0 flex-[1_1_320px]" :process-id="selectedId" @changed="refreshAll" />
    </div>

    <div class="flex flex-wrap items-start gap-4">
      <Card class="min-w-0 flex-[2_1_560px]">
        <PublicationsChart :series="summary?.publicationsPerDay ?? []" />
      </Card>
      <Card class="flex min-w-0 flex-[1_1_320px] flex-col gap-2" aria-labelledby="notif-titulo">
        <h2 id="notif-titulo" class="text-heading">Últimas notificações</h2>
        <ul>
          <li v-for="n in notifications" :key="n.id" class="flex flex-wrap items-center gap-x-4 gap-y-1 border-t border-border py-2">
            <span class="w-20 font-medium">{{ n.channel === 'EMAIL' ? 'E-mail' : 'WhatsApp' }}</span>
            <span class="min-w-0 flex-[1_1_140px]">
              {{ n.clientName }}
              <span class="block font-mono text-[13px] text-muted-foreground">{{ n.processNumber }}</span>
            </span>
            <StatusBadge :situation="n.status === 'SENT' ? 'SENT' : n.status === 'FAILED' ? 'FAILED_DELIVERY' : 'PENDING'" />
            <span v-if="n.errorMessage && auth.isStaff" class="basis-full text-[13px] text-destructive">{{ n.errorMessage }}</span>
          </li>
          <li v-if="notifications.length === 0" class="border-t border-border py-2 text-muted-foreground">Nenhuma notificação enviada ainda.</li>
        </ul>
        <RouterLink to="/notificacoes" class="text-[13px] text-primary underline">Ver todas</RouterLink>
      </Card>
    </div>
  </div>
</template>
