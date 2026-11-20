<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RefreshCw } from 'lucide-vue-next'
import Button from '@/shared/ui/Button.vue'
import Card from '@/shared/ui/Card.vue'
import Input from '@/shared/ui/Input.vue'
import ProblemAlert from '@/shared/ui/ProblemAlert.vue'
import { tribunalsApi } from '@/shared/api/endpoints'
import { toProblem } from '@/shared/api/problem'
import type { Problem, Tribunal } from '@/shared/api/types'

const tribunals = ref<Tribunal[]>([])
const query = ref('')
const syncing = ref(false)
const busyCode = ref<string | null>(null)
const problem = ref<Problem | null>(null)
const feedback = ref('')

const filtered = computed(() => {
  const q = query.value.trim().toLowerCase()
  if (!q) return tribunals.value
  return tribunals.value.filter(
    (t) => t.code.toLowerCase().includes(q) || t.name.toLowerCase().includes(q) || t.state.toLowerCase().includes(q),
  )
})

async function load() {
  problem.value = null
  try {
    tribunals.value = await tribunalsApi.list()
  } catch (error) {
    problem.value = toProblem(error)
  }
}

async function toggle(tribunal: Tribunal) {
  busyCode.value = tribunal.code
  problem.value = null
  try {
    const updated = await tribunalsApi.setActive(tribunal.code, !tribunal.active)
    tribunals.value = tribunals.value.map((t) => (t.code === updated.code ? updated : t))
    feedback.value = `${updated.code} ${updated.active ? 'ativado' : 'desativado'} para monitoramento.`
  } catch (error) {
    problem.value = toProblem(error)
  } finally {
    busyCode.value = null
  }
}

async function synchronize() {
  syncing.value = true
  problem.value = null
  try {
    const res = await tribunalsApi.synchronize()
    feedback.value = `Sincronização DJEN concluída: ${res.received} recebidos, ${res.created} criados, ${res.updated} atualizados.`
    await load()
  } catch (error) {
    problem.value = toProblem(error)
  } finally {
    syncing.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="flex flex-col gap-8">
    <header class="flex flex-wrap items-end justify-between gap-4">
      <div>
        <h1 class="text-title">Tribunais</h1>
        <p class="text-caption">Tribunais habilitados para monitoramento no DJEN e DataJud.</p>
      </div>
      <Button :loading="syncing" @click="synchronize">
        <RefreshCw class="size-4" aria-hidden="true" /> Sincronizar com DJEN
      </Button>
    </header>

    <Card class="flex flex-col gap-4">
      <div class="max-w-[320px]">
        <label class="sr-only" for="tribunal-q">Filtrar tribunais</label>
        <Input id="tribunal-q" v-model="query" placeholder="Filtrar por sigla, nome ou UF" />
      </div>
      <p v-if="feedback" role="status" class="rounded-md bg-accent p-3 text-[13px]">{{ feedback }}</p>
      <ProblemAlert :problem="problem" />
      <div class="overflow-x-auto">
        <table class="w-full min-w-[600px] border-collapse">
          <thead>
            <tr class="text-left text-label text-muted-foreground">
              <th scope="col" class="border-b border-border p-2">Sigla</th>
              <th scope="col" class="border-b border-border p-2">Nome</th>
              <th scope="col" class="border-b border-border p-2">UF</th>
              <th scope="col" class="border-b border-border p-2">Status</th>
              <th scope="col" class="border-b border-border p-2"><span class="sr-only">Ação</span></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="t in filtered" :key="t.code">
              <td class="border-b border-border p-2 font-mono font-semibold">{{ t.code }}</td>
              <td class="border-b border-border p-2">{{ t.name }}</td>
              <td class="border-b border-border p-2">{{ t.state }}</td>
              <td class="border-b border-border p-2">{{ t.active ? 'Ativo' : 'Inativo' }}</td>
              <td class="border-b border-border p-2 text-right">
                <Button variant="ghost" size="sm" :loading="busyCode === t.code" @click="toggle(t)">
                  {{ t.active ? 'Desativar' : 'Ativar' }}
                </Button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </Card>
  </div>
</template>
