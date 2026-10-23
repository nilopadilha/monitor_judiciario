<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Plus } from 'lucide-vue-next'
import Button from '@/shared/ui/Button.vue'
import Card from '@/shared/ui/Card.vue'
import Input from '@/shared/ui/Input.vue'
import Modal from '@/shared/ui/Modal.vue'
import Pagination from '@/shared/ui/Pagination.vue'
import ProblemAlert from '@/shared/ui/ProblemAlert.vue'
import ProcessForm from './ProcessForm.vue'
import ProcessTable from './ProcessTable.vue'
import { processesApi } from '@/shared/api/endpoints'
import { toProblem } from '@/shared/api/problem'
import type { Page, Problem, Process, Situation } from '@/shared/api/types'
import { useAuthStore } from '@/stores/auth'
import { watchDebounced } from '@vueuse/core'

const auth = useAuthStore()
const router = useRouter()
const page = ref<Page<Process> | null>(null)
const number = ref('')
const situation = ref<Situation | ''>('')
const current = ref(0)
const loading = ref(false)
const creating = ref(false)
const problem = ref<Problem | null>(null)

async function load() {
  loading.value = true
  problem.value = null
  try {
    page.value = await processesApi.list({ number: number.value, situation: situation.value || undefined, page: current.value })
  } catch (error) {
    problem.value = toProblem(error)
  } finally {
    loading.value = false
  }
}

function onCreated(process: Process) {
  creating.value = false
  router.push(`/processos/${process.id}`)
}

watchDebounced(number, () => { current.value = 0; load() }, { debounce: 300 })
watch(situation, () => { current.value = 0; load() })
watch(current, load)
onMounted(load)
</script>

<template>
  <div class="flex flex-col gap-8">
    <header class="flex flex-wrap items-end justify-between gap-4">
      <h1 class="text-title">Processos</h1>
      <Button v-if="auth.isStaff" @click="creating = true"><Plus aria-hidden="true" /> Cadastrar processo</Button>
    </header>
    <Card class="flex flex-col gap-4">
      <div class="flex flex-wrap gap-2">
        <label class="sr-only" for="busca">Buscar por número</label>
        <Input id="busca" v-model="number" class="max-w-[320px] font-mono" placeholder="Buscar por número CNJ" inputmode="numeric" />
        <label class="sr-only" for="situacao">Situação</label>
        <select id="situacao" v-model="situation" class="h-11 rounded-md border border-input bg-card px-3">
          <option value="">Todas as situações</option>
          <option value="NEW_PUBLICATION">Com novidade</option>
          <option value="FAILED">Com falha</option>
          <option value="PAUSED">Pausados</option>
          <option value="ARCHIVED">Arquivados</option>
        </select>
      </div>
      <ProblemAlert :problem="problem" />
      <ProcessTable :processes="page?.content ?? []" :loading="loading" @select="router.push(`/processos/${$event.id}`)" />
      <Pagination v-if="page" :page="page.page" :total-pages="page.totalPages" :total-elements="page.totalElements" @change="current = $event" />
    </Card>
    <Modal v-model:open="creating" title="Cadastrar processo" description="Informe o número no padrão CNJ. O monitoramento começa na próxima execução.">
      <ProcessForm @created="onCreated" />
    </Modal>
  </div>
</template>
