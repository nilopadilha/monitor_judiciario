<script setup lang="ts">
import { computed, toRef } from 'vue'
import { BellDot } from 'lucide-vue-next'
import Button from '@/shared/ui/Button.vue'
import Card from '@/shared/ui/Card.vue'
import ProblemAlert from '@/shared/ui/ProblemAlert.vue'
import Timeline from './Timeline.vue'
import { useProcessDetail } from './useProcessDetail'
import { useAuthStore } from '@/stores/auth'

const props = defineProps<{ processId: string | null }>()
const emit = defineEmits<{ changed: [] }>()
const auth = useAuthStore()

const { process, movements, unread, loading, marking, problem, markAllRead } = useProcessDetail(toRef(props, 'processId'))
const unreadIds = computed(() => unread.value.map((p) => p.id))

async function markRead() {
  await markAllRead()
  emit('changed')
}
</script>

<template>
  <Card as="aside" class="flex flex-col gap-4" aria-labelledby="detalhe-titulo">
    <template v-if="process">
      <div class="flex flex-col gap-1">
        <span class="text-label text-muted-foreground">Processo selecionado</span>
        <h2 id="detalhe-titulo" class="text-process-number">{{ process.number }}</h2>
        <span class="text-heading">{{ process.clientName }}</span>
        <span class="text-caption">
          {{ process.subject ?? 'Sem assunto' }} · {{ process.courtCode }}
        </span>
        <RouterLink :to="`/processos/${process.id}`" class="text-[13px] text-primary underline">Ver processo completo</RouterLink>
      </div>
      <div v-if="unread.length" class="flex flex-wrap items-center justify-between gap-2 rounded-md bg-background p-2">
        <span class="flex items-center gap-1 text-[13px] font-semibold text-novelty-text">
          <BellDot class="size-4" aria-hidden="true" />
          {{ unread.length === 1 ? 'Há publicação nova no DJEN' : `Há ${unread.length} publicações novas no DJEN` }}
        </span>
        <Button v-if="auth.isStaff" variant="outline" :loading="marking" @click="markRead">Marcar como lida</Button>
      </div>
      <p v-if="process.situation === 'FAILED'" class="text-[13px] text-destructive">
        {{ auth.isStaff ? `Última verificação falhou: ${process.lastError}` : 'Não foi possível consultar o DJEN. Tentaremos de novo na próxima execução.' }}
      </p>
      <ProblemAlert :problem="problem" />
      <div class="flex flex-col gap-2">
        <h3 class="text-label text-muted-foreground">Linha do tempo · DJEN × PJe</h3>
        <Timeline :movements="movements" :unread-publication-ids="unreadIds" />
      </div>
    </template>
    <p v-else-if="loading" role="status" class="text-muted-foreground">Carregando…</p>
    <p v-else class="text-muted-foreground">Selecione um processo para ver a linha do tempo.</p>
  </Card>
</template>
