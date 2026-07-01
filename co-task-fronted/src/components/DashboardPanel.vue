<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { AlertTriangle, TrendingUp, CheckCircle, Clock, Loader2 } from 'lucide-vue-next'
import api from '../api/axios'
import { useBoardStore } from '../stores/boardStore'
import type { Risk } from '../types'

const props = defineProps<{ workspaceId: string }>()

const boardStore = useBoardStore()

const risks = ref<Risk[]>([])
const loading = ref(true)
const totalCards = ref(0)
const doneCards = ref(0)
const inProgressCards = ref(0)

let prevListsSnapshot = ''

const loadDashboardData = async () => {
  loading.value = true
  try {
    const [risksRes] = await Promise.all([
      api.get(`/ai/risks/workspace/${props.workspaceId}`).catch(() => ({ data: { risks: [] } })),
    ])

    risks.value = risksRes.data.risks || []

    try {
      const boardsRes = await api.get(`/boards/workspace/${props.workspaceId}`)
      const boardsData = boardsRes.data || []
      let total = 0, done = 0, progress = 0

      for (const board of boardsData) {
        try {
          const detailRes = await api.get(`/boards/${board.id}`)
          const lists = detailRes.data.lists || []
          for (const list of lists) {
            const cards = list.cards || []
            total += cards.length
            const title = list.title.toLowerCase()
            if (title.includes('完成') || title.includes('done')) {
              done += cards.length
            } else if (title.includes('进行') || title.includes('progress')) {
              progress += cards.length
            }
          }
        } catch {
          // skip
        }
      }

      totalCards.value = total
      doneCards.value = done
      inProgressCards.value = progress
    } catch {
      // skip
    }
  } catch (err) {
    console.error('加载仪表盘数据失败', err)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadDashboardData()
})

watch(
  () => boardStore.currentBoard?.lists,
  (newLists) => {
    const snapshot = JSON.stringify(
      newLists?.map((l) => ({ id: l.id, cardCount: l.cards.length })) || []
    )
    if (prevListsSnapshot && prevListsSnapshot !== snapshot) {
      loadDashboardData()
    }
    prevListsSnapshot = snapshot
  },
  { deep: true }
)
</script>

<template>
  <div v-if="loading" class="p-6 flex items-center justify-center">
    <Loader2 class="h-6 w-6 animate-spin text-blue-600" />
  </div>

  <div v-else class="bg-white rounded-2xl shadow-sm border border-slate-200 p-6 space-y-6">
    <h2 class="text-lg font-bold text-slate-800 flex items-center space-x-2">
      <TrendingUp class="h-5 w-5 text-blue-600" />
      <span>项目仪表盘</span>
    </h2>

    <!-- Stats Cards -->
    <div class="grid grid-cols-3 gap-3">
      <div class="bg-slate-50 rounded-xl p-4 text-center">
        <p class="text-2xl font-bold text-slate-800">{{ totalCards }}</p>
        <p class="text-xs text-slate-500 mt-1">总任务数</p>
      </div>
      <div class="bg-emerald-50 rounded-xl p-4 text-center">
        <div class="flex justify-center">
          <CheckCircle class="h-5 w-5 text-emerald-500" />
        </div>
        <p class="text-2xl font-bold text-emerald-700">{{ doneCards }}</p>
        <p class="text-xs text-emerald-600 mt-1">已完成</p>
      </div>
      <div class="bg-blue-50 rounded-xl p-4 text-center">
        <div class="flex justify-center">
          <Clock class="h-5 w-5 text-blue-500" />
        </div>
        <p class="text-2xl font-bold text-blue-700">{{ inProgressCards }}</p>
        <p class="text-xs text-blue-600 mt-1">进行中</p>
      </div>
    </div>

    <!-- Progress Bar -->
    <div>
      <div class="flex justify-between text-xs text-slate-500 mb-1">
        <span>项目进度</span>
        <span>{{ totalCards > 0 ? Math.round((doneCards / totalCards) * 100) : 0 }}%</span>
      </div>
      <div class="h-2 bg-slate-100 rounded-full overflow-hidden">
        <div
          class="h-full bg-emerald-500 rounded-full transition-all duration-500"
          :style="{ width: `${totalCards > 0 ? Math.round((doneCards / totalCards) * 100) : 0}%` }"
        />
      </div>
    </div>

    <!-- Risk Warnings -->
    <div>
      <h3 class="text-sm font-semibold text-slate-700 mb-3 flex items-center space-x-2">
        <AlertTriangle class="h-4 w-4 text-orange-500" />
        <span>风险预警</span>
      </h3>

      <div v-if="risks.filter(r => r.riskLevel === 'HIGH').length === 0" class="text-center py-4">
        <CheckCircle class="h-8 w-8 text-emerald-400 mx-auto mb-2" />
        <p class="text-sm text-emerald-600 font-medium">一切正常</p>
        <p class="text-xs text-slate-400 mt-1">未发现高风险任务</p>
      </div>

      <div v-else class="space-y-2">
        <div
          v-for="(risk, i) in risks.filter(r => r.riskLevel === 'HIGH').slice(0, 5)"
          :key="i"
          class="flex items-center p-3 bg-red-50 border border-red-200 rounded-xl"
        >
          <AlertTriangle class="h-4 w-4 text-red-500 shrink-0 mr-3" />
          <div class="flex-1 min-w-0">
            <p class="text-sm font-medium text-slate-800 truncate">{{ risk.cardTitle }}</p>
            <p class="text-xs text-red-600 mt-0.5">
              距截止日期还有 {{ risk.daysUntilDue }} 天 · 当前状态: {{ risk.currentStatus }}
            </p>
          </div>
          <span class="text-xs font-bold text-red-700 bg-red-100 px-2 py-0.5 rounded-full shrink-0">高风险</span>
        </div>
      </div>
    </div>
  </div>
</template>
