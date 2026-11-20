<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { watchDebounced } from '@vueuse/core'
import { Plus, Search } from 'lucide-vue-next'
import Button from '@/shared/ui/Button.vue'
import Card from '@/shared/ui/Card.vue'
import Field from '@/shared/ui/Field.vue'
import Input from '@/shared/ui/Input.vue'
import Modal from '@/shared/ui/Modal.vue'
import Pagination from '@/shared/ui/Pagination.vue'
import ProblemAlert from '@/shared/ui/ProblemAlert.vue'
import { lawyersApi } from '@/shared/api/endpoints'
import { toProblem } from '@/shared/api/problem'
import type { Lawyer, LawyerInput, Page, PracticeArea, Problem } from '@/shared/api/types'

const AREAS: { value: PracticeArea; label: string }[] = [
  { value: 'CIVEL', label: 'Cível' },
  { value: 'TRABALHISTA', label: 'Trabalhista' },
  { value: 'TRIBUTARIO', label: 'Tributário' },
  { value: 'FAMILIA', label: 'Família e Sucessões' },
  { value: 'CRIMINAL', label: 'Criminal' },
  { value: 'PREVIDENCIARIO', label: 'Previdenciário' },
  { value: 'EMPRESARIAL', label: 'Empresarial' },
  { value: 'CONSUMIDOR', label: 'Direito do Consumidor' },
  { value: 'ADMINISTRATIVO', label: 'Administrativo' },
  { value: 'OUTROS', label: 'Outros' },
]

const page = ref<Page<Lawyer> | null>(null)
const query = ref('')
const area = ref<PracticeArea | ''>('')
const current = ref(0)
const open = ref(false)
const editing = ref<Lawyer | null>(null)
const saving = ref(false)
const discoveringId = ref<string | null>(null)
const problem = ref<Problem | null>(null)
const formProblem = ref<Problem | null>(null)
const feedback = ref('')

const form = reactive<LawyerInput>({
  name: '',
  oabNumber: '',
  oabUf: 'SP',
  practiceArea: 'CIVEL',
  email: '',
  phone: '',
  notifyEmail: true,
  notifyWhatsapp: false,
})

async function load() {
  problem.value = null
  try {
    page.value = await lawyersApi.list({
      q: query.value,
      practiceArea: area.value || undefined,
      page: current.value,
    })
  } catch (error) {
    problem.value = toProblem(error)
  }
}

function openForm(lawyer: Lawyer | null) {
  editing.value = lawyer
  form.name = lawyer?.name ?? ''
  form.oabNumber = lawyer?.oabNumber ?? ''
  form.oabUf = lawyer?.oabUf ?? 'SP'
  form.practiceArea = lawyer?.practiceArea ?? 'CIVEL'
  form.email = lawyer?.email ?? ''
  form.phone = lawyer?.phone ?? ''
  form.notifyEmail = lawyer?.notifyEmail ?? true
  form.notifyWhatsapp = lawyer?.notifyWhatsapp ?? false
  formProblem.value = null
  open.value = true
}

async function submit() {
  saving.value = true
  formProblem.value = null
  try {
    if (editing.value) {
      await lawyersApi.update(editing.value.id, form)
      feedback.value = 'Advogado atualizado com sucesso.'
    } else {
      await lawyersApi.create(form)
      feedback.value = 'Advogado cadastrado. A busca automática de processos pela OAB no DJEN foi iniciada.'
    }
    open.value = false
    await load()
  } catch (error) {
    formProblem.value = toProblem(error)
  } finally {
    saving.value = false
  }
}

async function discover(lawyer: Lawyer) {
  discoveringId.value = lawyer.id
  problem.value = null
  try {
    const res = await lawyersApi.discoverProcesses(lawyer.id)
    feedback.value = `${lawyer.oabFormatted}: ${res.communicationsFound} publicações analisadas, ${res.processesCreated} processos criados, ${res.processesLinked} vinculados.`
  } catch (error) {
    problem.value = toProblem(error)
  } finally {
    discoveringId.value = null
  }
}

async function deactivate(lawyer: Lawyer) {
  if (!window.confirm(`Inativar ${lawyer.name}?`)) return
  try {
    await lawyersApi.deactivate(lawyer.id)
    feedback.value = `${lawyer.name} inativado.`
    await load()
  } catch (error) {
    problem.value = toProblem(error)
  }
}

watchDebounced(query, () => { current.value = 0; load() }, { debounce: 300 })
watch(area, () => { current.value = 0; load() })
watch(current, load)
onMounted(load)
</script>

<template>
  <div class="flex flex-col gap-8">
    <header class="flex flex-wrap items-end justify-between gap-4">
      <div>
        <h1 class="text-title">Advogados do Escritório</h1>
        <p class="text-caption">Cadastre OAB, área do direito e receba alertas de movimentações e prazos dos processos.</p>
      </div>
      <Button @click="openForm(null)"><Plus aria-hidden="true" /> Cadastrar advogado</Button>
    </header>

    <Card class="flex flex-col gap-4">
      <div class="flex flex-wrap gap-2">
        <label class="sr-only" for="lawyer-q">Buscar advogado</label>
        <Input id="lawyer-q" v-model="query" class="max-w-[320px]" placeholder="Buscar por nome ou OAB" />
        <label class="sr-only" for="lawyer-area">Área do direito</label>
        <select id="lawyer-area" v-model="area" class="h-11 rounded-md border border-input bg-card px-3">
          <option value="">Todas as áreas</option>
          <option v-for="a in AREAS" :key="a.value" :value="a.value">{{ a.label }}</option>
        </select>
      </div>
      <p v-if="feedback" role="status" class="rounded-md bg-accent p-3 text-[13px]">{{ feedback }}</p>
      <ProblemAlert :problem="problem" />
      <div class="overflow-x-auto">
        <table class="w-full min-w-[740px] border-collapse">
          <thead>
            <tr class="text-left text-label text-muted-foreground">
              <th scope="col" class="border-b border-border p-2">Advogado</th>
              <th scope="col" class="border-b border-border p-2">OAB</th>
              <th scope="col" class="border-b border-border p-2">Área</th>
              <th scope="col" class="border-b border-border p-2">Contato</th>
              <th scope="col" class="border-b border-border p-2"><span class="sr-only">Ações</span></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="l in page?.content ?? []" :key="l.id">
              <td class="border-b border-border p-2 font-medium">{{ l.name }}</td>
              <td class="border-b border-border p-2 font-mono text-[14px]">{{ l.oabFormatted }}</td>
              <td class="border-b border-border p-2">{{ l.practiceAreaLabel }}</td>
              <td class="border-b border-border p-2 text-caption">{{ l.email }}<br />{{ l.phone }}</td>
              <td class="border-b border-border p-2 text-right whitespace-nowrap">
                <Button variant="outline" size="sm" :loading="discoveringId === l.id" @click="discover(l)">
                  <Search class="size-3.5" aria-hidden="true" /> Buscar no DJEN
                </Button>
                <Button variant="ghost" size="sm" @click="openForm(l)">Editar</Button>
                <Button v-if="l.active" variant="ghost" size="sm" class="text-destructive" @click="deactivate(l)">Inativar</Button>
              </td>
            </tr>
          </tbody>
        </table>
        <p v-if="page && page.content.length === 0" class="px-2 py-4 text-muted-foreground">Nenhum advogado cadastrado.</p>
      </div>
      <Pagination
        v-if="page"
        :page="page.page"
        :total-pages="page.totalPages"
        :total-elements="page.totalElements"
        @change="current = $event"
      />
    </Card>

    <Modal v-model:open="open" :title="editing ? 'Editar advogado' : 'Cadastrar advogado'">
      <form class="flex flex-col gap-4" novalidate @submit.prevent="submit">
        <Field id="l-name" label="Nome completo" :error="formProblem?.errors?.name">
          <Input id="l-name" v-model="form.name" maxlength="200" required />
        </Field>
        <div class="flex gap-2">
          <Field id="l-oab" label="Número OAB" class="flex-1" :error="formProblem?.errors?.oabNumber">
            <Input id="l-oab" v-model="form.oabNumber" :disabled="!!editing" required />
          </Field>
          <Field id="l-uf" label="UF" class="w-24" :error="formProblem?.errors?.oabUf">
            <Input id="l-uf" v-model="form.oabUf" maxlength="2" :disabled="!!editing" required />
          </Field>
        </div>
        <Field id="l-area" label="Área do direito">
          <select id="l-area" v-model="form.practiceArea" class="h-11 rounded-md border border-input bg-card px-3">
            <option v-for="a in AREAS" :key="a.value" :value="a.value">{{ a.label }}</option>
          </select>
        </Field>
        <Field id="l-email" label="E-mail" :error="formProblem?.errors?.email">
          <Input id="l-email" v-model="form.email" type="email" required />
        </Field>
        <Field id="l-phone" label="Telefone (WhatsApp)">
          <Input id="l-phone" v-model="form.phone" type="tel" />
        </Field>
        <fieldset class="flex flex-col gap-2">
          <legend class="text-[13px] font-semibold">Alertar advogado por</legend>
          <label class="flex min-h-11 items-center gap-2"><input v-model="form.notifyEmail" type="checkbox" class="size-4 accent-[var(--brand)]" /> E-mail</label>
          <label class="flex min-h-11 items-center gap-2"><input v-model="form.notifyWhatsapp" type="checkbox" class="size-4 accent-[var(--brand)]" /> WhatsApp</label>
        </fieldset>
        <ProblemAlert :problem="formProblem" />
        <Button type="submit" :loading="saving">{{ editing ? 'Salvar alterações' : 'Cadastrar advogado' }}</Button>
      </form>
    </Modal>
  </div>
</template>
