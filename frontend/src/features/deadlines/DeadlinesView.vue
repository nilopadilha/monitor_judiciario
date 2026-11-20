<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { BellRing, Plus } from 'lucide-vue-next'
import Button from '@/shared/ui/Button.vue'
import Card from '@/shared/ui/Card.vue'
import Field from '@/shared/ui/Field.vue'
import Input from '@/shared/ui/Input.vue'
import Modal from '@/shared/ui/Modal.vue'
import Pagination from '@/shared/ui/Pagination.vue'
import ProblemAlert from '@/shared/ui/ProblemAlert.vue'
import { deadlinesApi, lawyersApi, processesApi } from '@/shared/api/endpoints'
import { toProblem } from '@/shared/api/problem'
import { formatDate } from '@/shared/lib/format'
import type {
  CreateDeadlineInput,
  Deadline,
  DeadlineStatus,
  DeadlineSummary,
  DeadlineUrgency,
  Lawyer,
  Page,
  Problem,
  Process,
} from '@/shared/api/types'

const URGENCY_LABEL: Record<DeadlineUrgency, string> = {
  OVERDUE: 'Vencido',
  TODAY: 'Vence hoje',
  SOON: 'Urgente (≤ 3d)',
  UPCOMING: 'Próximo (≤ 7d)',
  NORMAL: 'No prazo',
  CLOSED: 'Encerrado',
}

const URGENCY_CLASS: Record<DeadlineUrgency, string> = {
  OVERDUE: 'bg-destructive/15 text-destructive font-semibold',
  TODAY: 'bg-destructive/15 text-destructive font-semibold',
  SOON: 'bg-amber-500/15 text-amber-800 dark:text-amber-300 font-semibold',
  UPCOMING: 'bg-accent text-foreground',
  NORMAL: 'bg-muted text-muted-foreground',
  CLOSED: 'bg-muted text-muted-foreground',
}

const summary = ref<DeadlineSummary | null>(null)
const page = ref<Page<Deadline> | null>(null)
const lawyers = ref<Lawyer[]>([])
const processes = ref<Process[]>([])
const lawyerFilter = ref('')
const statusFilter = ref<DeadlineStatus | ''>('OPEN')
const current = ref(0)
const open = ref(false)
const saving = ref(false)
const alerting = ref(false)
const problem = ref<Problem | null>(null)
const formProblem = ref<Problem | null>(null)
const feedback = ref('')

const form = reactive<CreateDeadlineInput>({
  processId: '',
  lawyerId: undefined,
  title: '',
  description: '',
  dueDate: '',
  daysCount: 15,
  businessDays: true,
})

async function load() {
  problem.value = null
  try {
    const [sum, list] = await Promise.all([
      deadlinesApi.summary(),
      deadlinesApi.list({
        lawyerId: lawyerFilter.value || undefined,
        status: statusFilter.value || undefined,
        page: current.value,
        size: 20,
      }),
    ])
    summary.value = sum
    page.value = list
  } catch (error) {
    problem.value = toProblem(error)
  }
}

async function openCreate() {
  form.title = ''
  form.description = ''
  form.dueDate = ''
  form.daysCount = 15
  form.businessDays = true
  formProblem.value = null
  try {
    processes.value = (await processesApi.list({ size: 100 })).content
    form.processId = processes.value[0]?.id ?? ''
  } catch {
    processes.value = []
  }
  open.value = true
}

async function submit() {
  saving.value = true
  formProblem.value = null
  try {
    await deadlinesApi.create({
      processId: form.processId,
      lawyerId: form.lawyerId || undefined,
      title: form.title,
      description: form.description || undefined,
      dueDate: form.dueDate || undefined,
      daysCount: form.dueDate ? undefined : form.daysCount,
      businessDays: form.businessDays,
    })
    open.value = false
    feedback.value = 'Prazo processual cadastrado.'
    await load()
  } catch (error) {
    formProblem.value = toProblem(error)
  } finally {
    saving.value = false
  }
}

async function changeStatus(d: Deadline, next: DeadlineStatus) {
  problem.value = null
  try {
    await deadlinesApi.updateStatus(d.id, next)
    feedback.value = next === 'COMPLETED' ? 'Prazo marcado como cumprido.' : 'Status do prazo atualizado.'
    await load()
  } catch (error) {
    problem.value = toProblem(error)
  }
}

async function triggerAlerts() {
  alerting.value = true
  problem.value = null
  try {
    const res = await deadlinesApi.triggerAlerts()
    feedback.value = `${res.alertsSent} alerta(s) de prazo enviados aos advogados responsáveis.`
  } catch (error) {
    problem.value = toProblem(error)
  } finally {
    alerting.value = false
  }
}

watch([lawyerFilter, statusFilter], () => {
  current.value = 0
  load()
})
watch(current, load)
onMounted(async () => {
  try {
    lawyers.value = (await lawyersApi.list({ active: true, size: 100 })).content
  } catch {
    lawyers.value = []
  }
  await load()
})
</script>

<template>
  <div class="flex flex-col gap-8">
    <header class="flex flex-wrap items-end justify-between gap-4">
      <div>
        <h1 class="text-title">Prazos Processuais</h1>
        <p class="text-caption">Prazos detectados automaticamente nas publicações do DJEN e agendados pelo escritório.</p>
      </div>
      <div class="flex flex-wrap gap-2">
        <Button variant="outline" :loading="alerting" @click="triggerAlerts">
          <BellRing class="size-4" aria-hidden="true" /> Enviar alertas de prazo
        </Button>
        <Button @click="openCreate"><Plus aria-hidden="true" /> Novo prazo</Button>
      </div>
    </header>

    <section v-if="summary" class="grid grid-cols-2 gap-4 md:grid-cols-5" aria-label="Resumo de prazos">
      <Card class="flex flex-col gap-1">
        <span class="text-label text-muted-foreground">Vencidos</span>
        <strong class="text-display text-destructive">{{ summary.overdue }}</strong>
      </Card>
      <Card class="flex flex-col gap-1">
        <span class="text-label text-muted-foreground">Vencem hoje</span>
        <strong class="text-display text-destructive">{{ summary.dueToday }}</strong>
      </Card>
      <Card class="flex flex-col gap-1">
        <span class="text-label text-muted-foreground">Próximos 3 dias</span>
        <strong class="text-display">{{ summary.dueIn3Days }}</strong>
      </Card>
      <Card class="flex flex-col gap-1">
        <span class="text-label text-muted-foreground">Próximos 7 dias</span>
        <strong class="text-display">{{ summary.dueIn7Days }}</strong>
      </Card>
      <Card class="flex flex-col gap-1">
        <span class="text-label text-muted-foreground">Em aberto</span>
        <strong class="text-display">{{ summary.openTotal }}</strong>
      </Card>
    </section>

    <Card class="flex flex-col gap-4">
      <div class="flex flex-wrap gap-2">
        <label class="sr-only" for="d-lawyer">Advogado</label>
        <select id="d-lawyer" v-model="lawyerFilter" class="h-11 rounded-md border border-input bg-card px-3">
          <option value="">Todos os advogados</option>
          <option v-for="l in lawyers" :key="l.id" :value="l.id">{{ l.name }} ({{ l.oabFormatted }})</option>
        </select>
        <label class="sr-only" for="d-status">Status</label>
        <select id="d-status" v-model="statusFilter" class="h-11 rounded-md border border-input bg-card px-3">
          <option value="OPEN">Em aberto</option>
          <option value="COMPLETED">Cumpridos</option>
          <option value="CANCELED">Cancelados</option>
          <option value="">Todos</option>
        </select>
      </div>
      <p v-if="feedback" role="status" class="rounded-md bg-accent p-3 text-[13px]">{{ feedback }}</p>
      <ProblemAlert :problem="problem" />
      <div class="overflow-x-auto">
        <table class="w-full min-w-[780px] border-collapse">
          <thead>
            <tr class="text-left text-label text-muted-foreground">
              <th scope="col" class="border-b border-border p-2">Data fatal</th>
              <th scope="col" class="border-b border-border p-2">Urgência</th>
              <th scope="col" class="border-b border-border p-2">Prazo</th>
              <th scope="col" class="border-b border-border p-2">Processo / Cliente</th>
              <th scope="col" class="border-b border-border p-2">Advogado</th>
              <th scope="col" class="border-b border-border p-2"><span class="sr-only">Ações</span></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="d in page?.content ?? []" :key="d.id">
              <td class="border-b border-border p-2 font-mono text-[14px] whitespace-nowrap">{{ formatDate(d.dueDate) }}</td>
              <td class="border-b border-border p-2">
                <span :class="['inline-block rounded px-2 py-0.5 text-[12px]', URGENCY_CLASS[d.urgency]]">
                  {{ URGENCY_LABEL[d.urgency] }}
                </span>
              </td>
              <td class="border-b border-border p-2">
                <span class="font-medium">{{ d.title }}</span>
                <span v-if="d.autoDetected" class="ml-2 rounded bg-accent px-1.5 py-0.5 text-[11px]">Automático</span>
              </td>
              <td class="border-b border-border p-2">
                <RouterLink :to="`/processos/${d.processId}`" class="text-process-number text-primary underline">
                  {{ d.processNumber }}
                </RouterLink>
                <div class="text-caption">{{ d.clientName }} · {{ d.courtCode }}</div>
              </td>
              <td class="border-b border-border p-2 text-caption">{{ d.lawyerName ?? '—' }}</td>
              <td class="border-b border-border p-2 text-right whitespace-nowrap">
                <Button v-if="d.status === 'OPEN'" variant="outline" size="sm" @click="changeStatus(d, 'COMPLETED')">
                  Cumprido
                </Button>
                <Button v-if="d.status === 'OPEN'" variant="ghost" size="sm" class="text-destructive" @click="changeStatus(d, 'CANCELED')">
                  Cancelar
                </Button>
                <Button v-if="d.status !== 'OPEN'" variant="ghost" size="sm" @click="changeStatus(d, 'OPEN')">
                  Reabrir
                </Button>
              </td>
            </tr>
          </tbody>
        </table>
        <p v-if="page && page.content.length === 0" class="px-2 py-4 text-muted-foreground">Nenhum prazo encontrado.</p>
      </div>
      <Pagination
        v-if="page"
        :page="page.page"
        :total-pages="page.totalPages"
        :total-elements="page.totalElements"
        @change="current = $event"
      />
    </Card>

    <Modal v-model:open="open" title="Novo prazo processual">
      <form class="flex flex-col gap-4" novalidate @submit.prevent="submit">
        <Field id="dl-process" label="Processo">
          <select id="dl-process" v-model="form.processId" class="h-11 rounded-md border border-input bg-card px-3" required>
            <option v-for="p in processes" :key="p.id" :value="p.id">{{ p.number }} — {{ p.clientName }}</option>
          </select>
        </Field>
        <Field id="dl-title" label="Descrição do prazo">
          <Input id="dl-title" v-model="form.title" placeholder="Ex.: Protocolar contestação" maxlength="200" required />
        </Field>
        <Field id="dl-due" label="Data fatal (opcional se informar dias)">
          <Input id="dl-due" v-model="form.dueDate" type="date" />
        </Field>
        <div v-if="!form.dueDate" class="flex items-end gap-3">
          <Field id="dl-days" label="Quantidade de dias" class="w-36">
            <Input id="dl-days" :model-value="String(form.daysCount ?? 15)" type="number" min="1" max="180" @update:model-value="form.daysCount = Number($event)" />
          </Field>
          <label class="flex min-h-11 items-center gap-2">
            <input v-model="form.businessDays" type="checkbox" class="size-4 accent-[var(--brand)]" /> Dias úteis (CPC)
          </label>
        </div>
        <ProblemAlert :problem="formProblem" />
        <Button type="submit" :loading="saving" :disabled="!form.processId">Salvar prazo</Button>
      </form>
    </Modal>
  </div>
</template>
