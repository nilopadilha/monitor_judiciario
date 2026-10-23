<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Button from '@/shared/ui/Button.vue'
import Field from '@/shared/ui/Field.vue'
import Input from '@/shared/ui/Input.vue'
import ProblemAlert from '@/shared/ui/ProblemAlert.vue'
import { toProblem } from '@/shared/api/problem'
import type { Problem } from '@/shared/api/types'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const email = ref('')
const password = ref('')
const loading = ref(false)
const problem = ref<Problem | null>(null)
const expired = route.query.expired === '1'

async function submit() {
  loading.value = true
  problem.value = null
  try {
    await auth.login(email.value, password.value)
    const redirect = typeof route.query.redirect === 'string' && route.query.redirect.startsWith('/') ? route.query.redirect : '/'
    await router.push(redirect)
  } catch (error) {
    problem.value = toProblem(error)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="flex min-h-screen items-center justify-center p-4">
    <div class="flex w-full max-w-[400px] flex-col gap-8">
      <div class="flex flex-col gap-2">
        <h1 class="text-display">Monitor Judicial</h1>
        <p class="text-caption">Acompanhe as publicações do DJEN dos processos dos seus clientes.</p>
      </div>
      <p v-if="expired" role="status" class="rounded-md bg-accent p-3 text-[13px]">Sua sessão expirou. Entre novamente.</p>
      <form class="flex flex-col gap-4 rounded-lg border border-border bg-card p-6" novalidate @submit.prevent="submit">
        <Field id="email" label="E-mail">
          <Input id="email" v-model="email" type="email" autocomplete="username" required />
        </Field>
        <Field id="password" label="Senha">
          <Input id="password" v-model="password" type="password" autocomplete="current-password" required />
        </Field>
        <ProblemAlert :problem="problem" />
        <Button type="submit" :loading="loading">Entrar</Button>
      </form>
    </div>
  </main>
</template>
