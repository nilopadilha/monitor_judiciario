<script setup lang="ts">
import { computed } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { SVGRenderer } from 'echarts/renderers'
import { formatDate } from '@/shared/lib/format'

use([BarChart, GridComponent, TooltipComponent, SVGRenderer])

const props = defineProps<{ series: { date: string; count: number }[] }>()

const total = computed(() => props.series.reduce((sum, d) => sum + d.count, 0))

/** Cores do design system: série principal em brand; eixos em ink-muted (ver README do design system). */
const option = computed(() => ({
  grid: { left: 32, right: 8, top: 16, bottom: 28 },
  tooltip: { trigger: 'axis', valueFormatter: (v: number) => `${v} publicação(ões)` },
  xAxis: {
    type: 'category',
    data: props.series.map((d) => formatDate(d.date).slice(0, 5)),
    axisLine: { lineStyle: { color: '#dcd8cc' } },
    axisLabel: { color: '#555f6d', fontFamily: 'IBM Plex Sans', fontSize: 12, interval: 6 },
    axisTick: { show: false },
  },
  yAxis: {
    type: 'value',
    minInterval: 1,
    splitLine: { lineStyle: { color: '#dcd8cc' } },
    axisLabel: { color: '#555f6d', fontFamily: 'IBM Plex Sans', fontSize: 12 },
  },
  series: [{ type: 'bar', data: props.series.map((d) => d.count), itemStyle: { color: '#1e4b5c', borderRadius: [2, 2, 0, 0] }, barMaxWidth: 16 }],
}))
</script>

<template>
  <figure class="flex flex-col gap-2">
    <figcaption class="flex items-baseline justify-between gap-2">
      <span class="text-heading">Publicações nos últimos 30 dias</span>
      <span class="text-caption">{{ total }} no período</span>
    </figcaption>
    <div class="h-[220px] w-full" role="img" :aria-label="`Gráfico de barras: ${total} publicações nos últimos 30 dias`">
      <VChart :option="option" autoresize style="display: block; width: 100%; height: 100%" />
    </div>
  </figure>
</template>
