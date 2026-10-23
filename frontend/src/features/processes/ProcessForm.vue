<script setup lang="ts">
import { onMounted, ref } from 'vue'
import Button from '@/shared/ui/Button.vue'
import Field from '@/shared/ui/Field.vue'
import Input from '@/shared/ui/Input.vue'
import ProblemAlert from '@/shared/ui/ProblemAlert.vue'
import { clientsApi, processesApi } from '@/shared/api/endpoints'
import { toProblem } from '@/shared/api/problem'
import { maskProcessNumber } from '@/shared/lib/format'
import type { Client, Problem, Process } from '@/shared/api/types'

const emit = defineEmits<{ created: [process: Process] }>()

const clients = ref<Client[]>([])
const clientId = ref('')
const number = ref('')
const subject = ref('')
const saving = ref(false)
const problem = ref<Problem | null>(null)

onMounted(async () => {
  clients.value = (await clientsApi.list({ status: 'ACTIVE', size: 100 })).content
  clientId.value = clients.value[0]?.id ?? ''
})

function onNumber(value: string) {
  number.value = maskProcessNumber(value)
}

async function submit() {
  saving.value = true
  problem.value = null
  try {
    const created = await processesApi.create({ clientId: clientId.value, number: number.value, subject: subject.value || undefined })
    emit('created', created)
  } catch (error) {
    problem.value = toProblem(error)
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <form class="flex flex-col gap-4" novalidate @submit.prevent="submit">
    <Field id="client" label="Cliente" :hint="clients.length === 0 ? 'Cadastre um cliente antes.' : undefined">
      <select id="client" v-model="clientId" class="h-11 rounded-md border border-input bg-card px-3" required>
        <option v-for="c in clients" :key="c.id" :value="c.id">{{ c.name }}</option>
      </select>
    </Field>
    <Field id="number" label="Número CNJ" hint="O tribunal é identificado pelo próprio número.">
      <Input
        id="number"
        :model-value="number"
        class="font-mono"
        inputmode="numeric"
        placeholder="0000000-00.0000.8.00.0000"
        required
        @update:model-value="onNumber"
      />
    </Field>
    <Field id="subject" label="Assunto (opcional)">
      <Input id="subject" v-model="subject" maxlength="300" />
    </Field>
    <ProblemAlert :problem="problem" />
    <Button type="submit" :loading="saving" :disabled="!clientId">Cadastrar processo</Button>
  </form>
</template>
