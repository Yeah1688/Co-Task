<script setup lang="ts">
import { ref, computed } from 'vue'
import { Brain, Bot, FileText, AlertTriangle, Loader2, X, Plus, Copy, Check, Search } from 'lucide-vue-next'
import api from '../api/axios'
import { useBoardStore } from '../stores/boardStore'
import type { AiTask, Risk, SubTask } from '../types'

const props = defineProps<{
  workspaceId?: string
  cardId?: string
  cardTitle?: string
  onClose?: () => void
}>()

const activeTab = ref<'decompose' | 'report' | 'risks'>('decompose')
const loading = ref(false)
const result = ref('')
const subtasks = ref<SubTask[]>([])
const risks = ref<Risk[]>([])
const taskStatus = ref('')
const copied = ref(false)
const createdCards = ref<Set<number>>(new Set())
const cardSearch = ref('')

const selectedCardId = ref<string | null>(null)
const boardStore = useBoardStore()

const cardId = computed(() => props.cardId || selectedCardId.value)
const cardTitle = computed(() =>
  props.cardTitle ||
  boardStore.currentBoard?.lists
    .flatMap((l) => l.cards)
    .find((c) => c.id === selectedCardId.value)?.title
)

const allCards = computed(() => {
  if (!boardStore.currentBoard) return []
  return boardStore.currentBoard.lists.flatMap((l) =>
    l.cards.map((c) => ({ id: c.id, title: c.title, listTitle: l.title }))
  )
})

const filteredCards = computed(() => {
  if (!cardSearch.value.trim()) return allCards.value
  const q = cardSearch.value.toLowerCase()
  return allCards.value.filter((c) => c.title.toLowerCase().includes(q))
})

const pollResult = async (taskId: string, callback: (task: AiTask) => void): Promise<void> => {
  const maxAttempts = 30
  for (let i = 0; i < maxAttempts; i++) {
    await new Promise((resolve) => setTimeout(resolve, 1500))
    try {
      const res = await api.get(`/ai/task/${taskId}`)
      const task: AiTask = res.data
      callback(task)
      if (task.status === 'COMPLETED' || task.status === 'FAILED') {
        return
      }
    } catch {
      // continue polling
    }
  }
  taskStatus.value = 'AI 响应超时，请稍后重试'
  loading.value = false
}

const handleDecompose = async () => {
  if (!cardId.value) return
  loading.value = true
  result.value = ''
  subtasks.value = []
  taskStatus.value = 'AI 正在分析任务并拆解为子任务...'

  try {
    const res = await api.post('/ai/decompose', { cardId: cardId.value })
    const taskId = res.data.taskId

    await pollResult(taskId, (task) => {
      if (task.status === 'COMPLETED') {
        try {
          const parsed = JSON.parse(task.result || '[]')
          subtasks.value = Array.isArray(parsed) ? parsed : []
          taskStatus.value = 'AI 拆解完成！'
        } catch {
          result.value = task.result || '拆解结果无法解析'
          taskStatus.value = 'AI 拆解完成'
        }
        loading.value = false
      } else if (task.status === 'FAILED') {
        taskStatus.value = 'AI 拆解失败: ' + (task.errorMessage || '未知错误')
        loading.value = false
      } else {
        taskStatus.value = 'AI 正在处理中...'
      }
    })
  } catch (err: any) {
    taskStatus.value = '请求失败: ' + (err.response?.data?.message || err.message)
    loading.value = false
  }
}

const handleWeeklyReport = async () => {
  if (!props.workspaceId) return
  loading.value = true
  result.value = ''
  taskStatus.value = 'AI 正在扫描看板数据并生成周报...'

  try {
    const res = await api.post('/ai/summary/weekly-report', { workspaceId: props.workspaceId })
    const taskId = res.data.taskId

    await pollResult(taskId, (task) => {
      if (task.status === 'COMPLETED') {
        result.value = task.result || ''
        taskStatus.value = '周报生成完成！'
        loading.value = false
      } else if (task.status === 'FAILED') {
        taskStatus.value = '周报生成失败: ' + (task.errorMessage || '未知错误')
        loading.value = false
      } else {
        taskStatus.value = 'AI 正在生成周报...'
      }
    })
  } catch (err: any) {
    taskStatus.value = '请求失败: ' + (err.response?.data?.message || err.message)
    loading.value = false
  }
}

const handleRiskAnalysis = async () => {
  if (!props.workspaceId) return
  loading.value = true
  risks.value = []
  taskStatus.value = 'AI 正在扫描风险卡片...'

  try {
    const res = await api.get(`/ai/risks/workspace/${props.workspaceId}`)
    risks.value = res.data.risks || []
    taskStatus.value = `发现 ${res.data.totalRisks || 0} 个风险项`
    loading.value = false
  } catch (err: any) {
    taskStatus.value = '风险分析失败: ' + (err.response?.data?.message || err.message)
    loading.value = false
  }
}

const copyResult = () => {
  navigator.clipboard.writeText(result.value)
  copied.value = true
  setTimeout(() => (copied.value = false), 2000)
}

const createSubtaskCard = (index: number, _subtask: SubTask) => {
  createdCards.value = new Set([...createdCards.value, index])
}
</script>

<template>
  <div class="bg-white rounded-2xl shadow-xl border border-slate-200 w-full max-w-lg overflow-hidden">
    <!-- Header -->
    <div class="p-4 border-b border-slate-100 flex items-center justify-between bg-gradient-to-r from-blue-50 to-indigo-50">
      <div class="flex items-center space-x-2">
        <Brain class="h-5 w-5 text-blue-600" />
        <h3 class="font-bold text-slate-800">AI 智能助手</h3>
      </div>
      <button v-if="onClose" @click="onClose" class="p-1 hover:bg-white/80 rounded-lg text-slate-400">
        <X class="h-4 w-4" />
      </button>
    </div>

    <!-- Tabs -->
    <div class="flex border-b border-slate-100">
      <button
        @click="activeTab = 'decompose'; result = ''; subtasks = []; taskStatus = ''"
        :class="['flex-1 py-2.5 text-xs font-semibold transition-colors', activeTab === 'decompose' ? 'text-blue-600 border-b-2 border-blue-600 bg-blue-50/50' : 'text-slate-500 hover:text-slate-700']"
      >
        <Bot class="h-3.5 w-3.5 inline mr-1" />任务拆解
      </button>
      <button
        @click="activeTab = 'report'; result = ''; taskStatus = ''"
        :class="['flex-1 py-2.5 text-xs font-semibold transition-colors', activeTab === 'report' ? 'text-blue-600 border-b-2 border-blue-600 bg-blue-50/50' : 'text-slate-500 hover:text-slate-700']"
      >
        <FileText class="h-3.5 w-3.5 inline mr-1" />周报生成
      </button>
      <button
        @click="activeTab = 'risks'; risks = []; taskStatus = ''"
        :class="['flex-1 py-2.5 text-xs font-semibold transition-colors', activeTab === 'risks' ? 'text-blue-600 border-b-2 border-blue-600 bg-blue-50/50' : 'text-slate-500 hover:text-slate-700']"
      >
        <AlertTriangle class="h-3.5 w-3.5 inline mr-1" />风险预警
      </button>
    </div>

    <!-- Content -->
    <div class="p-4 space-y-4 max-h-[400px] overflow-y-auto">
      <!-- Decompose Tab -->
      <div v-if="activeTab === 'decompose'" class="space-y-3">
        <template v-if="cardId">
          <div class="flex items-center justify-between">
            <p class="text-xs text-slate-500">
              将为任务 <span class="font-semibold text-slate-700">"{{ cardTitle }}"</span> 拆解为 3-5 个子任务
            </p>
            <button
              v-if="selectedCardId && !props.cardId"
              @click="selectedCardId = null; subtasks = []; taskStatus = ''"
              class="text-[10px] text-blue-500 hover:text-blue-700 underline shrink-0 ml-2"
            >
              换一张卡片
            </button>
          </div>
          <button
            @click="handleDecompose"
            :disabled="loading"
            class="w-full py-2.5 bg-blue-600 hover:bg-blue-700 text-white font-medium rounded-xl text-sm flex items-center justify-center space-x-2 transition-colors disabled:opacity-50"
          >
            <Loader2 v-if="loading" class="h-4 w-4 animate-spin" />
            <Brain v-else class="h-4 w-4" />
            <span>{{ loading ? '拆解中...' : 'AI 一键拆解' }}</span>
          </button>
          <p v-if="taskStatus" class="text-xs text-slate-500 italic">{{ taskStatus }}</p>
          <div v-if="subtasks.length > 0" class="space-y-2">
            <p class="text-xs font-semibold text-slate-600">生成的子任务:</p>
            <div v-for="(st, i) in subtasks" :key="i" class="p-3 bg-slate-50 rounded-xl border border-slate-100">
              <div class="flex items-start justify-between">
                <div class="flex-1">
                  <p class="text-sm font-semibold text-slate-700">{{ st.title }}</p>
                  <p class="text-xs text-slate-500 mt-0.5">{{ st.description }}</p>
                </div>
                <button
                  @click="createSubtaskCard(i, st)"
                  :disabled="createdCards.has(i)"
                  :class="['ml-2 p-1.5 rounded-lg shrink-0 transition-colors', createdCards.has(i) ? 'bg-emerald-50 text-emerald-500' : 'bg-blue-50 text-blue-500 hover:bg-blue-100']"
                >
                  <Check v-if="createdCards.has(i)" class="h-3.5 w-3.5" />
                  <Plus v-else class="h-3.5 w-3.5" />
                </button>
              </div>
            </div>
          </div>
        </template>
        <template v-else>
          <div class="space-y-3">
            <div class="p-3 bg-amber-50 rounded-xl border border-amber-200">
              <p class="text-xs text-amber-700 font-medium mb-1">请先选择要拆解的任务卡片</p>
              <p class="text-xs text-amber-500">您可以从下方列表中选择当前看板中的卡片</p>
            </div>

            <div class="relative">
              <Search class="absolute left-2.5 top-2 h-3.5 w-3.5 text-slate-400" />
              <input
                type="text"
                placeholder="搜索卡片..."
                v-model="cardSearch"
                class="w-full pl-8 pr-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-xs focus:outline-none focus:border-blue-400"
              />
            </div>

            <div class="max-h-48 overflow-y-auto space-y-1">
              <p v-if="filteredCards.length === 0" class="text-xs text-slate-400 text-center py-4">
                {{ allCards.length === 0 ? '当前看板暂无卡片' : '未找到匹配的卡片' }}
              </p>
              <button
                v-for="card in filteredCards"
                :key="card.id"
                @click="selectedCardId = card.id; cardSearch = ''"
                class="w-full text-left p-2.5 bg-slate-50 hover:bg-blue-50 rounded-lg border border-slate-100 hover:border-blue-200 transition-colors group"
              >
                <p class="text-xs font-semibold text-slate-700 group-hover:text-blue-700 truncate">{{ card.title }}</p>
                <p class="text-[10px] text-slate-400 mt-0.5">所在列表: {{ card.listTitle }}</p>
              </button>
            </div>
          </div>
        </template>
      </div>

      <!-- Report Tab -->
      <div v-if="activeTab === 'report'" class="space-y-3">
        <template v-if="workspaceId">
          <p class="text-xs text-slate-500">
            AI 将读取当前看板中所有"已完成"和"进行中"的卡片，生成结构化项目进度周报
          </p>
          <button
            @click="handleWeeklyReport"
            :disabled="loading"
            class="w-full py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white font-medium rounded-xl text-sm flex items-center justify-center space-x-2 transition-colors disabled:opacity-50"
          >
            <Loader2 v-if="loading" class="h-4 w-4 animate-spin" />
            <FileText v-else class="h-4 w-4" />
            <span>{{ loading ? '生成中...' : '一键生成周报' }}</span>
          </button>
          <p v-if="taskStatus" class="text-xs text-slate-500 italic">{{ taskStatus }}</p>
          <div v-if="result" class="relative">
            <div class="prose prose-sm max-w-none text-slate-700 bg-slate-50 rounded-xl p-4 border border-slate-200 whitespace-pre-wrap text-xs leading-relaxed">
              {{ result }}
            </div>
            <button
              @click="copyResult"
              class="absolute top-2 right-2 p-1.5 bg-white rounded-lg shadow-sm border border-slate-200 hover:bg-slate-50"
            >
              <Check v-if="copied" class="h-3.5 w-3.5 text-emerald-500" />
              <Copy v-else class="h-3.5 w-3.5 text-slate-400" />
            </button>
          </div>
        </template>
        <p v-else class="text-xs text-slate-400">请先选择工作区以生成周报</p>
      </div>

      <!-- Risks Tab -->
      <div v-if="activeTab === 'risks'" class="space-y-3">
        <template v-if="workspaceId">
          <p class="text-xs text-slate-500">
            扫描临近截止日期且状态仍为"未开始"的卡片，给出风险提示
          </p>
          <button
            @click="handleRiskAnalysis"
            :disabled="loading"
            class="w-full py-2.5 bg-orange-600 hover:bg-orange-700 text-white font-medium rounded-xl text-sm flex items-center justify-center space-x-2 transition-colors disabled:opacity-50"
          >
            <Loader2 v-if="loading" class="h-4 w-4 animate-spin" />
            <AlertTriangle v-else class="h-4 w-4" />
            <span>{{ loading ? '分析中...' : '扫描风险' }}</span>
          </button>
          <p v-if="taskStatus" class="text-xs text-slate-500 italic">{{ taskStatus }}</p>
          <div v-if="risks.length > 0" class="space-y-2">
            <div
              v-for="(risk, i) in risks"
              :key="i"
              :class="['p-3 rounded-xl border', risk.riskLevel === 'HIGH' ? 'bg-red-50 border-red-200' : 'bg-yellow-50 border-yellow-200']"
            >
              <div class="flex items-start justify-between">
                <div>
                  <p class="text-sm font-semibold text-slate-800">{{ risk.cardTitle }}</p>
                  <p class="text-xs text-slate-600 mt-0.5">{{ risk.message }}</p>
                </div>
                <span
                  :class="['text-xs font-bold px-2 py-0.5 rounded-full', risk.riskLevel === 'HIGH' ? 'bg-red-100 text-red-700' : 'bg-yellow-100 text-yellow-700']"
                >
                  {{ risk.daysUntilDue }}天
                </span>
              </div>
            </div>
          </div>
          <p v-if="!loading && risks.length === 0 && taskStatus" class="text-xs text-emerald-600 font-medium">
            ✅ 未发现风险项，所有任务进展正常
          </p>
        </template>
        <p v-else class="text-xs text-slate-400">请先选择工作区以进行风险扫描</p>
      </div>
    </div>
  </div>
</template>
