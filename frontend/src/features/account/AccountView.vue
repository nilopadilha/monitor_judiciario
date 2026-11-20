<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import Button from '@/shared/ui/Button.vue'
import Card from '@/shared/ui/Card.vue'
import Field from '@/shared/ui/Field.vue'
import Input from '@/shared/ui/Input.vue'
import ProblemAlert from '@/shared/ui/ProblemAlert.vue'
import { authApi, lgpdApi } from '@/shared/api/endpoints'
import { toProblem } from '@/shared/api/problem'
import type { Problem } from '@/shared/api/types'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()

const currentPassword = ref('')
const newPassword = ref('')
const savingPassword = ref(false)
const passwordProblem = ref<Problem | null>(null)
const passwordMessage = ref('')

const notifyEmail = ref(true)
const notifyWhatsapp = ref(false)
const savingPrefs = ref(false)
const exporting = ref(false)
const lgpdProblem = ref<Problem | null>(null)
const lgpdMessage = ref('')

async function updatePassword() {
  savingPassword.value = true
  passwordProblem.value = null
  passwordMessage.value = ''
  try {
    await authApi.changePassword(currentPassword.value, newPassword.value)
    passwordMessage.value = 'Senha atualizada. Por segurança, faça login novamente.'
    currentPassword.value = ''
    newPassword.value = ''
    await auth.logout()
    await router.push('/login')
  } catch (error) {
    passwordProblem.value = toProblem(error)
  } finally {
    savingPassword.value = false
  }
}

async function savePreferences() {
  savingPrefs.value = true
  lgpdProblem.value = null
  lgpdMessage.value = ''
  try {
    await lgpdApi.updateMyPreferences(notifyEmail.value, notifyWhatsapp.value)
    lgpdMessage.value = 'Preferências de notificação atualizadas com sucesso.'
  } catch (error) {
    lgpdProblem.value = toProblem(error)
  } finally {
    savingPrefs.value = false
  }
}

async function exportMyData() {
  exporting.value = true
  lgpdProblem.value = null
  lgpdMessage.value = ''
  try {
    const data = await lgpdApi.exportMine()
    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = 'meus-dados-lgpd.json'
    link.click()
    URL.revokeObjectURL(url)
    lgpdMessage.value = 'Arquivo de portabilidade LGPD baixado.'
  } catch (error) {
    lgpdProblem.value = toProblem(error)
  } finally {
    exporting.value = false
  }
}
</script>

<template>
  <div class="flex flex-col gap-8">
    <header>
      <h1 class="text-title">Minha Conta e Privacidade</h1>
      <p class="text-caption">{{ auth.user?.name }} · {{ auth.user?.email }} ({{ auth.user?.role }})</p>
    </header>

    <div class="flex flex-wrap items-start gap-6">
      <Card class="flex min-w-0 flex-[1_1_360px] flex-col gap-4">
        <h2 class="text-heading">Segurança — Alterar senha</h2>
        <form class="flex flex-col gap-4" novalidate @submit.prevent="updatePassword">
          <Field id="cur-pass" label="Senha atual">
            <Input id="cur-pass" v-model="currentPassword" type="password" autocomplete="current-password" required />
          </Field>
          <Field id="new-pass" label="Nova senha" hint="Mínimo de 12 caracteres, com letras e números.">
            <Input id="new-pass" v-model="newPassword" type="password" autocomplete="new-password" minlength="12" required />
          </Field>
          <ProblemAlert :problem="passwordProblem" />
          <p v-if="passwordMessage" role="status" class="rounded-md bg-accent p-3 text-[13px]">{{ passwordMessage }}</p>
          <Button type="submit" :loading="savingPassword">Atualizar senha</Button>
        </form>
      </Card>

      <Card v-if="auth.role === 'CUSTOMER'" class="flex min-w-0 flex-[1_1_360px] flex-col gap-4">
        <h2 class="text-heading">Privacidade e LGPD (Art. 18)</h2>
        <p class="text-caption">
          Gerencie os canais pelos quais deseja receber alertas de novas publicações ou baixe uma cópia estruturada dos seus dados.
        </p>
        <fieldset class="flex flex-col gap-2">
          <legend class="text-[13px] font-semibold">Canais de notificação</legend>
          <label class="flex min-h-11 items-center gap-2">
            <input v-model="notifyEmail" type="checkbox" class="size-4 accent-[var(--brand)]" /> E-mail
          </label>
          <label class="flex min-h-11 items-center gap-2">
            <input v-model="notifyWhatsapp" type="checkbox" class="size-4 accent-[var(--brand)]" /> WhatsApp
          </label>
        </fieldset>
        <ProblemAlert :problem="lgpdProblem" />
        <p v-if="lgpdMessage" role="status" class="rounded-md bg-accent p-3 text-[13px]">{{ lgpdMessage }}</p>
        <div class="flex flex-wrap gap-2">
          <Button :loading="savingPrefs" @click="savePreferences">Salvar preferências</Button>
          <Button variant="outline" :loading="exporting" @click="exportMyData">Exportar meus dados (JSON)</Button>
        </div>
      </Card>
    </div>
  </div>
</template>
