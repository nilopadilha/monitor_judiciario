<script setup lang="ts">
import { reactive, ref } from 'vue'
import Button from '@/shared/ui/Button.vue'
import Field from '@/shared/ui/Field.vue'
import Input from '@/shared/ui/Input.vue'
import ProblemAlert from '@/shared/ui/ProblemAlert.vue'
import { clientsApi } from '@/shared/api/endpoints'
import { toProblem } from '@/shared/api/problem'
import { maskDocument } from '@/shared/lib/format'
import type { Client, ClientInput, Problem } from '@/shared/api/types'

const props = defineProps<{ client?: Client | null }>()
const emit = defineEmits<{ saved: [client: Client] }>()

const form = reactive<ClientInput>({
  name: props.client?.name ?? '',
  document: props.client?.document ?? '',
  email: props.client?.email ?? '',
  phone: props.client?.phone ?? '',
  notifyEmail: props.client?.notifyEmail ?? true,
  notifyWhatsapp: props.client?.notifyWhatsapp ?? false,
})
const saving = ref(false)
const problem = ref<Problem | null>(null)

async function submit() {
  saving.value = true
  problem.value = null
  try {
    const saved = props.client ? await clientsApi.update(props.client.id, form) : await clientsApi.create(form)
    emit('saved', saved)
  } catch (error) {
    problem.value = toProblem(error)
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <form class="flex flex-col gap-4" novalidate @submit.prevent="submit">
    <Field id="name" label="Nome ou razão social" :error="problem?.errors?.name">
      <Input id="name" v-model="form.name" maxlength="200" required />
    </Field>
    <Field id="document" label="CPF ou CNPJ" :error="problem?.errors?.document" :hint="client ? 'O documento não pode ser alterado.' : undefined">
      <Input
        id="document"
        :model-value="form.document"
        inputmode="numeric"
        :disabled="!!client"
        required
        @update:model-value="form.document = maskDocument($event)"
      />
    </Field>
    <Field id="email" label="E-mail" :error="problem?.errors?.email">
      <Input id="email" v-model="form.email" type="email" required />
    </Field>
    <Field id="phone" label="Telefone (WhatsApp)" hint="Com DDD. Obrigatório para notificar por WhatsApp.">
      <Input id="phone" v-model="form.phone" type="tel" inputmode="tel" />
    </Field>
    <fieldset class="flex flex-col gap-2">
      <legend class="text-[13px] font-semibold">Notificar por</legend>
      <label class="flex min-h-11 items-center gap-2"><input v-model="form.notifyEmail" type="checkbox" class="size-4 accent-[var(--brand)]" /> E-mail</label>
      <label class="flex min-h-11 items-center gap-2"><input v-model="form.notifyWhatsapp" type="checkbox" class="size-4 accent-[var(--brand)]" /> WhatsApp</label>
    </fieldset>
    <ProblemAlert :problem="problem" />
    <Button type="submit" :loading="saving">{{ client ? 'Salvar alterações' : 'Cadastrar cliente' }}</Button>
  </form>
</template>
