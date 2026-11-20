<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { Plus } from 'lucide-vue-next'
import Button from '@/shared/ui/Button.vue'
import Card from '@/shared/ui/Card.vue'
import Field from '@/shared/ui/Field.vue'
import Input from '@/shared/ui/Input.vue'
import Modal from '@/shared/ui/Modal.vue'
import Pagination from '@/shared/ui/Pagination.vue'
import ProblemAlert from '@/shared/ui/ProblemAlert.vue'
import { clientsApi, usersApi } from '@/shared/api/endpoints'
import { toProblem } from '@/shared/api/problem'
import type { Client, CreateUserInput, Page, Problem, User } from '@/shared/api/types'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const page = ref<Page<User> | null>(null)
const clients = ref<Client[]>([])
const current = ref(0)
const open = ref(false)
const saving = ref(false)
const problem = ref<Problem | null>(null)
const formProblem = ref<Problem | null>(null)
const feedback = ref('')

const form = reactive<CreateUserInput>({
  name: '',
  email: '',
  password: '',
  role: 'MANAGER',
  clientId: undefined,
})

async function load() {
  problem.value = null
  try {
    page.value = await usersApi.list(current.value, 20)
  } catch (error) {
    problem.value = toProblem(error)
  }
}

async function openCreate() {
  form.name = ''
  form.email = ''
  form.password = ''
  form.role = 'MANAGER'
  form.clientId = undefined
  formProblem.value = null
  try {
    clients.value = (await clientsApi.list({ status: 'ACTIVE', size: 100 })).content
  } catch {
    clients.value = []
  }
  open.value = true
}

async function submit() {
  saving.value = true
  formProblem.value = null
  try {
    await usersApi.create({
      ...form,
      clientId: form.role === 'CUSTOMER' ? form.clientId : undefined,
    })
    open.value = false
    feedback.value = 'Usuário criado com sucesso.'
    await load()
  } catch (error) {
    formProblem.value = toProblem(error)
  } finally {
    saving.value = false
  }
}

async function toggleActive(user: User) {
  const next = !user.active
  const verb = next ? 'reativar' : 'desativar'
  if (!window.confirm(`Deseja ${verb} o usuário ${user.name}?`)) return
  problem.value = null
  try {
    await usersApi.setActive(user.id, next)
    feedback.value = next ? `${user.name} reativado.` : `${user.name} desativado e sessões revogadas.`
    await load()
  } catch (error) {
    problem.value = toProblem(error)
  }
}

watch(current, load)
onMounted(load)
</script>

<template>
  <div class="flex flex-col gap-8">
    <header class="flex flex-wrap items-end justify-between gap-4">
      <div>
        <h1 class="text-title">Usuários</h1>
        <p class="text-caption">Controle de acesso (RBAC) e ciclo de vida de sessões.</p>
      </div>
      <Button @click="openCreate"><Plus aria-hidden="true" /> Novo usuário</Button>
    </header>

    <Card class="flex flex-col gap-4">
      <p v-if="feedback" role="status" class="rounded-md bg-accent p-3 text-[13px]">{{ feedback }}</p>
      <ProblemAlert :problem="problem" />
      <div class="overflow-x-auto">
        <table class="w-full min-w-[640px] border-collapse">
          <thead>
            <tr class="text-left text-label text-muted-foreground">
              <th scope="col" class="border-b border-border p-2">Nome</th>
              <th scope="col" class="border-b border-border p-2">E-mail</th>
              <th scope="col" class="border-b border-border p-2">Perfil</th>
              <th scope="col" class="border-b border-border p-2">Status</th>
              <th scope="col" class="border-b border-border p-2"><span class="sr-only">Ações</span></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="u in page?.content ?? []" :key="u.id">
              <td class="border-b border-border p-2 font-medium">{{ u.name }}</td>
              <td class="border-b border-border p-2 text-caption">{{ u.email }}</td>
              <td class="border-b border-border p-2">{{ u.role }}</td>
              <td class="border-b border-border p-2">{{ u.active ? 'Ativo' : 'Inativo' }}</td>
              <td class="border-b border-border p-2 text-right">
                <Button
                  v-if="u.id !== auth.user?.id"
                  variant="ghost"
                  size="sm"
                  :class="u.active ? 'text-destructive' : ''"
                  @click="toggleActive(u)"
                >
                  {{ u.active ? 'Desativar' : 'Reativar' }}
                </Button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <Pagination
        v-if="page"
        :page="page.page"
        :total-pages="page.totalPages"
        :total-elements="page.totalElements"
        @change="current = $event"
      />
    </Card>

    <Modal v-model:open="open" title="Novo usuário">
      <form class="flex flex-col gap-4" novalidate @submit.prevent="submit">
        <Field id="u-name" label="Nome" :error="formProblem?.errors?.name">
          <Input id="u-name" v-model="form.name" maxlength="120" required />
        </Field>
        <Field id="u-email" label="E-mail" :error="formProblem?.errors?.email">
          <Input id="u-email" v-model="form.email" type="email" required />
        </Field>
        <Field id="u-pass" label="Senha inicial" hint="Mínimo de 12 caracteres com letras e números." :error="formProblem?.errors?.password">
          <Input id="u-pass" v-model="form.password" type="password" minlength="12" required />
        </Field>
        <Field id="u-role" label="Perfil">
          <select id="u-role" v-model="form.role" class="h-11 rounded-md border border-input bg-card px-3">
            <option value="ADMIN">ADMIN</option>
            <option value="MANAGER">MANAGER</option>
            <option value="CUSTOMER">CUSTOMER</option>
          </select>
        </Field>
        <Field v-if="form.role === 'CUSTOMER'" id="u-client" label="Cliente vinculado">
          <select id="u-client" v-model="form.clientId" class="h-11 rounded-md border border-input bg-card px-3" required>
            <option value="" disabled>Selecione um cliente</option>
            <option v-for="c in clients" :key="c.id" :value="c.id">{{ c.name }}</option>
          </select>
        </Field>
        <ProblemAlert :problem="formProblem" />
        <Button type="submit" :loading="saving">Criar usuário</Button>
      </form>
    </Modal>
  </div>
</template>
