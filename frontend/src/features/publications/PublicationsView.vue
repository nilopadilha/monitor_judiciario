<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import Button from '@/shared/ui/Button.vue'
import Card from '@/shared/ui/Card.vue'
import Modal from '@/shared/ui/Modal.vue'
import Pagination from '@/shared/ui/Pagination.vue'
import ProblemAlert from '@/shared/ui/ProblemAlert.vue'
import StatusBadge from '@/shared/ui/StatusBadge.vue'
import { publicationsApi } from '@/shared/api/endpoints'
import { toProblem } from '@/shared/api/problem'
import { formatDate } from '@/shared/lib/format'
import type { Page, Problem, Publication } from '@/shared/api/types'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const page = ref<Page<Publication> | null>(null)
const onlyUnread = ref(true)
const current = ref(0)
const loading = ref(false)
const problem = ref<Problem | null>(null)
const selected = ref<Publication | null>(null)
const open = ref(false)
const markingId = ref<string | null>(null)

async function load() {
  loading.value = true
  problem.value = null
  try {
    page.value = await publicationsApi.list({
      unread: onlyUnread.value ? true : undefined,
      page: current.value,
      size: 20,
    })
  } catch (error) {
    problem.value = toProblem(error)
  } finally {
    loading.value = false
  }
}

function inspect(pub: Publication) {
  selected.value = pub
  open.value = true
}

async function markRead(pub: Publication) {
  markingId.value = pub.id
  problem.value = null
  try {
    await publicationsApi.markRead(pub.id)
    if (selected.value?.id === pub.id) {
      selected.value = { ...pub, read: true }
    }
    await load()
  } catch (error) {
    problem.value = toProblem(error)
  } finally {
    markingId.value = null
  }
}

watch(onlyUnread, () => {
  current.value = 0
  load()
})
watch(current, load)
onMounted(load)
</script>

<template>
  <div class="flex flex-col gap-8">
    <header class="flex flex-wrap items-end justify-between gap-4">
      <div>
        <h1 class="text-title">Publicações</h1>
        <p class="text-caption">Caixa de entrada unificada de publicações DJEN e movimentações complementares.</p>
      </div>
      <div class="flex gap-2" role="group" aria-label="Filtro de leitura">
        <Button :variant="onlyUnread ? 'default' : 'outline'" @click="onlyUnread = true">Não lidas</Button>
        <Button :variant="!onlyUnread ? 'default' : 'outline'" @click="onlyUnread = false">Todas</Button>
      </div>
    </header>

    <Card class="flex flex-col gap-4">
      <ProblemAlert :problem="problem" />
      <p v-if="loading && !page" role="status" class="text-muted-foreground">Carregando publicações…</p>
      <div v-else class="flex flex-col divide-y divide-border">
        <article v-for="pub in page?.content ?? []" :key="pub.id" class="flex flex-col gap-2 py-4 first:pt-0">
          <div class="flex flex-wrap items-center justify-between gap-2">
            <div class="flex flex-wrap items-center gap-2 text-caption">
              <RouterLink :to="`/processos/${pub.processId}`" class="text-process-number text-primary underline">
                {{ pub.processNumber ?? pub.processId }}
              </RouterLink>
              <span v-if="pub.courtCode">· {{ pub.courtCode }}</span>
              <span>· {{ formatDate(pub.publicationDate) }}</span>
              <span class="rounded bg-accent px-1.5 py-0.5 text-[11px] font-semibold">{{ pub.source }}</span>
              <span v-if="pub.documentType || pub.communicationType">· {{ pub.documentType ?? pub.communicationType }}</span>
              <StatusBadge v-if="!pub.read" situation="NEW_PUBLICATION" />
            </div>
            <div class="flex gap-2">
              <Button variant="ghost" size="sm" @click="inspect(pub)">Ler teor</Button>
              <Button
                v-if="auth.isStaff && !pub.read"
                variant="outline"
                size="sm"
                :loading="markingId === pub.id"
                @click="markRead(pub)"
              >
                Marcar como lida
              </Button>
            </div>
          </div>
          <p class="line-clamp-3 text-[14px] text-foreground">{{ pub.content }}</p>
        </article>
        <p v-if="page && page.content.length === 0" class="py-4 text-muted-foreground">
          {{ onlyUnread ? 'Nenhuma publicação pendente de leitura.' : 'Nenhuma publicação registrada até agora.' }}
        </p>
      </div>
      <Pagination
        v-if="page"
        :page="page.page"
        :total-pages="page.totalPages"
        :total-elements="page.totalElements"
        @change="current = $event"
      />
    </Card>

    <Modal v-model:open="open" :title="selected?.processNumber ?? 'Publicação'">
      <div v-if="selected" class="flex flex-col gap-3">
        <p class="text-caption">
          {{ formatDate(selected.publicationDate) }} · {{ selected.source }}
          <span v-if="selected.organName">· {{ selected.organName }}</span>
        </p>
        <p class="max-h-[50vh] overflow-y-auto whitespace-pre-line rounded-md border border-border bg-muted/30 p-3 text-[14px]">
          {{ selected.content }}
        </p>
        <div class="flex flex-wrap items-center justify-between gap-2">
          <a
            v-if="selected.link"
            :href="selected.link"
            target="_blank"
            rel="noopener noreferrer"
            class="text-[13px] text-primary underline"
          >
            Abrir na fonte oficial
          </a>
          <Button v-if="auth.isStaff && !selected.read" size="sm" :loading="markingId === selected.id" @click="markRead(selected)">
            Marcar como lida
          </Button>
        </div>
      </div>
    </Modal>
  </div>
</template>
