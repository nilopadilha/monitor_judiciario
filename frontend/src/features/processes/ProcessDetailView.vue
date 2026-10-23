<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Button from '@/shared/ui/Button.vue'
import Card from '@/shared/ui/Card.vue'
import Field from '@/shared/ui/Field.vue'
import Modal from '@/shared/ui/Modal.vue'
import ProblemAlert from '@/shared/ui/ProblemAlert.vue'
import StatusBadge from '@/shared/ui/StatusBadge.vue'
import Timeline from './Timeline.vue'
import { useProcessDetail } from './useProcessDetail'
import { notificationsApi, processesApi, publicationsApi } from '@/shared/api/endpoints'
import { toProblem } from '@/shared/api/problem'
import { formatDate, formatDateTime } from '@/shared/lib/format'
import type { Channel, Problem, Publication } from '@/shared/api/types'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const id = computed(() => route.params.id as string)
const { process, movements, unread, loading, marking, problem, load, markAllRead } = useProcessDetail(id)

const publications = ref<Publication[]>([])
const actionProblem = ref<Problem | null>(null)
const busy = ref(false)
const notifying = ref(false)
const channel = ref<Channel>('EMAIL')
const message = ref('')
const sentMessage = ref('')

const unreadIds = computed(() => unread.value.map((p) => p.id))

async function loadPublications() {
  publications.value = (await publicationsApi.list({ processId: id.value, size: 50 })).content
}

async function togglePause() {
  if (!process.value) return
  busy.value = true
  actionProblem.value = null
  try {
    await processesApi.update(process.value.id, {
      subject: process.value.subject,
      status: process.value.status === 'PAUSED' ? 'ACTIVE' : 'PAUSED',
    })
    await load()
  } catch (error) {
    actionProblem.value = toProblem(error)
  } finally {
    busy.value = false
  }
}

async function archive() {
  if (!process.value || !window.confirm('Arquivar encerra o monitoramento deste processo. O histórico é mantido. Continuar?')) return
  busy.value = true
  try {
    await processesApi.archive(process.value.id)
    await router.push('/processos')
  } catch (error) {
    actionProblem.value = toProblem(error)
  } finally {
    busy.value = false
  }
}

async function sendNotification() {
  actionProblem.value = null
  try {
    const result = await notificationsApi.send({ processId: id.value, channel: channel.value, message: message.value })
    sentMessage.value = result.status === 'SENT' ? 'Notificação enviada.' : `Não foi possível enviar: ${result.errorMessage}`
    notifying.value = false
    message.value = ''
  } catch (error) {
    actionProblem.value = toProblem(error)
  }
}

async function markRead() {
  await markAllRead()
  await loadPublications()
}

onMounted(loadPublications)
</script>

<template>
  <div class="flex flex-col gap-8">
    <RouterLink to="/processos" class="text-[13px] text-primary underline">← Processos</RouterLink>
    <ProblemAlert :problem="problem" />
    <p v-if="loading && !process" role="status">Carregando…</p>
    <template v-if="process">
      <header class="flex flex-wrap items-end justify-between gap-4">
        <div class="flex flex-col gap-1">
          <span class="text-label text-muted-foreground">{{ process.courtCode }} · {{ process.clientName }}</span>
          <h1 class="text-process-number text-[20px] leading-[28px]">{{ process.number }}</h1>
          <span class="text-caption">
            {{ process.subject ?? 'Sem assunto' }} · Última verificação: {{ formatDateTime(process.lastCheckedAt) }}
          </span>
        </div>
        <div class="flex flex-wrap items-center gap-2">
          <StatusBadge :situation="process.situation" />
          <template v-if="auth.isStaff && process.status !== 'ARCHIVED'">
            <Button variant="outline" :loading="busy" @click="togglePause">
              {{ process.status === 'PAUSED' ? 'Retomar monitoramento' : 'Pausar monitoramento' }}
            </Button>
            <Button variant="outline" @click="notifying = true">Notificar cliente</Button>
            <Button variant="destructive" :disabled="busy" @click="archive">Arquivar</Button>
          </template>
        </div>
      </header>
      <ProblemAlert :problem="actionProblem" />
      <p v-if="sentMessage" role="status" class="rounded-md bg-accent p-3 text-[13px]">{{ sentMessage }}</p>
      <p v-if="process.situation === 'FAILED'" class="text-destructive">
        {{ auth.isStaff ? `Última verificação falhou: ${process.lastError}` : 'Não foi possível consultar o DJEN. Tentaremos de novo na próxima execução.' }}
      </p>

      <div class="flex flex-wrap items-start gap-4">
        <Card class="flex min-w-0 flex-[1_1_320px] flex-col gap-2">
          <div class="flex flex-wrap items-center justify-between gap-2">
            <h2 class="text-heading">Linha do tempo</h2>
            <Button v-if="auth.isStaff && unread.length" variant="outline" size="sm" :loading="marking" @click="markRead">
              Marcar {{ unread.length }} como lida(s)
            </Button>
          </div>
          <Timeline :movements="movements" :unread-publication-ids="unreadIds" />
        </Card>
        <Card class="flex min-w-0 flex-[2_1_480px] flex-col gap-2">
          <h2 class="text-heading">Publicações no DJEN</h2>
          <article v-for="p in publications" :key="p.id" class="flex flex-col gap-1 border-t border-border py-3">
            <div class="flex flex-wrap items-center gap-2 text-caption">
              <span>{{ formatDate(p.publicationDate) }}</span>
              <span>· {{ p.documentType ?? p.communicationType }}</span>
              <span v-if="p.organName">· {{ p.organName }}</span>
              <StatusBadge v-if="!p.read" situation="NEW_PUBLICATION" />
            </div>
            <p class="whitespace-pre-line">{{ p.content }}</p>
            <a v-if="p.link" :href="p.link" target="_blank" rel="noopener noreferrer" class="text-[13px] text-primary underline">
              Abrir no DJEN
            </a>
          </article>
          <p v-if="publications.length === 0" class="border-t border-border py-2 text-muted-foreground">
            Nenhuma publicação encontrada até agora.
          </p>
        </Card>
      </div>
    </template>

    <Modal v-model:open="notifying" title="Notificar cliente" description="A mensagem é enviada pelo canal escolhido, se estiver habilitado no cadastro do cliente.">
      <form class="flex flex-col gap-4" @submit.prevent="sendNotification">
        <Field id="channel" label="Canal">
          <select id="channel" v-model="channel" class="h-11 rounded-md border border-input bg-card px-3">
            <option value="EMAIL">E-mail</option>
            <option value="WHATSAPP">WhatsApp</option>
          </select>
        </Field>
        <Field id="message" label="Mensagem">
          <textarea id="message" v-model="message" rows="5" maxlength="2000" required class="rounded-md border border-input bg-card p-3" />
        </Field>
        <Button type="submit" :disabled="!message.trim()">Enviar</Button>
      </form>
    </Modal>
  </div>
</template>
