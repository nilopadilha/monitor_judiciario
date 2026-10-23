<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { Activity, Bell, FileText, LayoutDashboard, LogOut, Users } from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()

const items = computed(() =>
  [
    { to: '/', label: 'Painel', icon: LayoutDashboard, show: true },
    { to: '/processos', label: 'Processos', icon: FileText, show: true },
    { to: '/clientes', label: 'Clientes', icon: Users, show: auth.isStaff },
    { to: '/notificacoes', label: 'Notificações', icon: Bell, show: true },
    { to: '/monitoramento', label: 'Monitoramento', icon: Activity, show: auth.isStaff },
  ].filter((item) => item.show),
)

async function logout() {
  await auth.logout()
  await router.push('/login')
}
</script>

<template>
  <div class="flex min-h-screen flex-wrap">
    <nav
      aria-label="Navegação principal"
      class="flex max-w-full flex-[1_1_220px] flex-col gap-8 border-r border-border px-4 py-8 max-md:border-r-0 max-md:border-b"
    >
      <RouterLink to="/" class="text-title px-2">Monitor Judicial</RouterLink>
      <ul class="flex flex-col gap-1 max-md:flex-row max-md:flex-wrap">
        <li v-for="item in items" :key="item.to">
          <RouterLink
            :to="item.to"
            class="flex min-h-11 items-center gap-2 rounded-md px-2 text-foreground hover:bg-accent"
            exact-active-class="bg-accent font-semibold text-primary"
          >
            <component :is="item.icon" class="size-5" aria-hidden="true" />
            {{ item.label }}
          </RouterLink>
        </li>
      </ul>
      <div class="mt-auto flex flex-col gap-2 border-t border-border px-2 pt-4 max-md:mt-0">
        <span class="text-[13px] font-semibold">{{ auth.user?.name }}</span>
        <span class="text-caption break-all">{{ auth.user?.email }}</span>
        <button
          type="button"
          class="flex min-h-11 items-center gap-2 rounded-md text-left text-primary hover:underline"
          @click="logout"
        >
          <LogOut class="size-4" aria-hidden="true" /> Sair
        </button>
      </div>
    </nav>
    <main class="min-w-0 flex-[999_1_560px] p-8 max-md:p-4">
      <RouterView />
    </main>
  </div>
</template>
