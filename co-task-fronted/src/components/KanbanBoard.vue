<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import draggable from 'vuedraggable'
import { useBoardStore } from '../stores/boardStore'
import {
  connectWorkspaceWebSocket, disconnectWorkspaceWebSocket,
  publishCardMove, publishCardUpdate,
} from '../api/websocket'
import {
  Plus, GripVertical, Kanban, Layers, Calendar, Edit3, Check,
  Brain, LayoutDashboard,
} from 'lucide-vue-next'
import CardDetailModal from './CardDetailModal.vue'
import AiAssistant from './AiAssistant.vue'
import DashboardPanel from './DashboardPanel.vue'
import type { Card } from '../stores/boardStore'

const props = defineProps<{ workspaceId: string }>()

const boardStore = useBoardStore()

const activeBoardId = ref('')
const showNewBoardInput = ref(false)
const newBoardTitle = ref('')
const newListTitle = ref('')
const newCardTitles = ref<Record<string, string>>({})

const editingCardId = ref<string | null>(null)
const editingTitle = ref('')

const selectedCardId = ref<string | null>(null)
const showAiPanel = ref(false)
const showDashboard = ref(false)

// 1. 初始化
onMounted(() => {
  boardStore.fetchWorkspaceBoards(props.workspaceId)
  connectWorkspaceWebSocket(props.workspaceId)
})

onUnmounted(() => {
  disconnectWorkspaceWebSocket()
})

// 2. 工作区切换：监听 workspaceId 变化，重置状态并重新加载
watch(
  () => props.workspaceId,
  (newWsId, oldWsId) => {
    if (!newWsId || newWsId === oldWsId) return

    // 清空看板状态
    activeBoardId.value = ''
    boardStore.resetBoardState()

    // 切换 WebSocket 到新工作区频道
    disconnectWorkspaceWebSocket()
    connectWorkspaceWebSocket(newWsId)

    // 拉取新工作区的看板列表
    boardStore.fetchWorkspaceBoards(newWsId)
  }
)

// 3. 当 boards 加载后自动选中第一个
watch(
  [() => boardStore.boards, activeBoardId],
  () => {
    if (activeBoardId.value) {
      boardStore.fetchBoardDetail(activeBoardId.value)
    } else if (boardStore.boards.length > 0) {
      activeBoardId.value = boardStore.boards[0].id
    }
  },
  { immediate: true }
)

// 3. 拖拽变化处理（乐观更新 Store + WebSocket 同步后端）
const handleListChange = (listId: string, event: { moved?: any; added?: any }) => {
  const board = boardStore.currentBoard
  if (!board) return

  const targetList = board.lists.find((l) => l.id === listId)
  if (!targetList) return

  if (event.moved) {
    // 同列表内移动
    const card = event.moved.element as Card
    const oldIdx = event.moved.oldIndex as number
    const newIdx = event.moved.newIndex as number

    // 1. 乐观更新 Pinia Store（立即响应用户拖拽，避免回弹）
    const { prevPosition, nextPosition } = boardStore.optimizeMoveCard(
      card.id, listId, listId, oldIdx, newIdx
    )

    // 2. 通过 WebSocket 同步到后端
    publishCardMove(card.id, listId, prevPosition, nextPosition)
  } else if (event.added) {
    // 跨列表移动
    const card = event.added.element as Card
    const newIdx = event.added.newIndex as number

    // 找到卡片原来的列表（Store 中仍是旧位置）
    let sourceListId = ''
    let sourceIndex = -1
    for (const list of board.lists) {
      const idx = list.cards.findIndex(c => c.id === card.id)
      if (idx !== -1) {
        sourceListId = list.id
        sourceIndex = idx
        break
      }
    }

    if (sourceListId && sourceIndex >= 0) {
      // 1. 乐观更新 Pinia Store
      const { prevPosition, nextPosition } = boardStore.optimizeMoveCard(
        card.id, sourceListId, listId, sourceIndex, newIdx
      )

      // 2. 通过 WebSocket 同步到后端
      publishCardMove(card.id, listId, prevPosition, nextPosition)
    }
  }
}

// 4. 创建看板
const handleCreateBoard = () => {
  if (!newBoardTitle.value.trim()) return
  boardStore.createBoard(props.workspaceId, newBoardTitle.value)
  newBoardTitle.value = ''
  showNewBoardInput.value = false
}

// 5. 创建列表
const handleCreateList = () => {
  if (!newListTitle.value.trim() || !boardStore.currentBoard) return
  const lastPosition = boardStore.currentBoard.lists[boardStore.currentBoard.lists.length - 1]?.position || 0.0
  boardStore.createList(boardStore.currentBoard.id, newListTitle.value, lastPosition)
  newListTitle.value = ''
}

// 6. 创建卡片
const handleCreateCard = (listId: string) => {
  const title = newCardTitles.value[listId]?.trim()
  if (!title) return
  boardStore.createCard(listId, title)
  newCardTitles.value = { ...newCardTitles.value, [listId]: '' }
}

// 7. 保存卡片标题
const handleSaveCardTitle = (cardId: string) => {
  if (!editingTitle.value.trim()) return
  publishCardUpdate(cardId, editingTitle.value)
  editingCardId.value = null
}
</script>

<template>
  <div class="h-screen flex flex-col bg-slate-50 overflow-hidden text-slate-800">
    <!-- 看板导航栏 -->
    <div class="bg-white border-b border-slate-200 px-6 py-3 flex items-center justify-between shadow-sm">
      <div class="flex items-center space-x-4 overflow-x-auto max-w-4xl py-1">
        <div class="flex items-center space-x-1.5 font-bold text-sm text-slate-500 border-r border-slate-200 pr-3">
          <Kanban class="h-4 w-4 text-blue-600" />
          <span>工作区看板:</span>
        </div>

        <button
          v-for="b in boardStore.boards"
          :key="b.id"
          @click="activeBoardId = b.id"
          :class="[
            'px-3 py-1.5 rounded-xl text-xs font-semibold transition-all shrink-0',
            activeBoardId === b.id
              ? 'bg-blue-600 text-white shadow-md shadow-blue-500/20'
              : 'bg-slate-100 hover:bg-slate-200 text-slate-600',
          ]"
        >
          {{ b.title }}
        </button>

        <!-- 仪表盘 & AI 切换 -->
        <div class="flex items-center space-x-1 ml-auto">
          <button
            @click="showDashboard = !showDashboard; showAiPanel = false"
            :class="['px-3 py-1.5 rounded-xl text-xs font-semibold transition-all flex items-center space-x-1', showDashboard ? 'bg-emerald-600 text-white' : 'bg-emerald-50 text-emerald-600 hover:bg-emerald-100']"
            title="项目仪表盘"
          >
            <LayoutDashboard class="h-3.5 w-3.5" />
            <span>仪表盘</span>
          </button>
          <button
            @click="showAiPanel = !showAiPanel; showDashboard = false"
            :class="['px-3 py-1.5 rounded-xl text-xs font-semibold transition-all flex items-center space-x-1', showAiPanel ? 'bg-indigo-600 text-white' : 'bg-indigo-50 text-indigo-600 hover:bg-indigo-100']"
            title="AI 智能助手"
          >
            <Brain class="h-3.5 w-3.5" />
            <span>AI 助手</span>
          </button>
        </div>

        <template v-if="showNewBoardInput">
          <div class="flex items-center space-x-1.5 bg-slate-50 p-1 rounded-xl border border-slate-200">
            <input
              type="text"
              placeholder="看板名称..."
              class="bg-transparent text-xs px-2 focus:outline-none w-28"
              v-model="newBoardTitle"
              @keydown.enter="handleCreateBoard"
            />
            <button @click="handleCreateBoard" class="p-1 bg-blue-600 text-white rounded-md">
              <Check class="h-3 w-3" />
            </button>
          </div>
        </template>
        <button
          v-else
          @click="showNewBoardInput = true"
          class="flex items-center space-x-1 px-2.5 py-1.5 text-xs font-medium text-blue-600 border border-dashed border-blue-300 rounded-xl hover:bg-blue-50 transition-colors shrink-0"
        >
          <Plus class="h-3.5 w-3.5" />
          <span>新建看板</span>
        </button>
      </div>
    </div>

    <!-- 主区域 -->
    <div v-if="boardStore.isLoading" class="flex-1 p-8 text-slate-400 text-xs animate-pulse">
      正在解析云端多维看板流模型...
    </div>

    <template v-else-if="boardStore.currentBoard">
      <div class="flex-1 overflow-x-auto overflow-y-hidden p-6 flex space-x-4 items-start">
        <!-- 列表列 -->
        <div
          v-for="list in boardStore.currentBoard.lists"
          :key="list.id"
          class="w-72 bg-slate-100/90 rounded-2xl p-3.5 flex flex-col max-h-full border border-slate-200/80 shadow-sm shrink-0"
        >
          <!-- 列表头 -->
          <div class="flex items-center justify-between mb-3 px-1">
            <span class="font-bold text-sm text-slate-700">{{ list.title }}</span>
            <span class="text-[11px] bg-slate-200 text-slate-500 font-bold px-2 py-0.5 rounded-full">
              {{ list.cards.length }}
            </span>
          </div>

          <!-- 卡片拖拽区域 -->
          <draggable
            :model-value="list.cards"
            :group="{ name: 'cards', pull: true, put: true }"
            item-key="id"
            class="flex-1 overflow-y-auto space-y-2.5 pb-2 transition-colors rounded-xl min-h-[40px]"
            ghost-class="opacity-50"
            @change="(evt: any) => handleListChange(list.id, evt)"
          >
            <template #item="{ element: card }">
              <div
                :class="[
                  'p-3 bg-white rounded-xl border border-slate-200 flex flex-col space-y-2 hover:border-blue-400 hover:shadow-sm transition-all select-none cursor-pointer',
                ]"
                @click="selectedCardId = card.id"
              >
                <div class="flex items-start justify-between">
                  <!-- 行内编辑 -->
                  <div v-if="editingCardId === card.id" class="flex items-center space-x-1 w-full mr-2">
                    <input
                      type="text"
                      class="border border-blue-400 rounded-md px-1.5 py-0.5 text-xs focus:outline-none w-full"
                      v-model="editingTitle"
                      @blur="handleSaveCardTitle(card.id)"
                      @keydown.enter="handleSaveCardTitle(card.id)"
                      autofocus
                    />
                  </div>
                  <span v-else class="text-xs font-bold text-slate-700 leading-snug flex-1">
                    {{ card.title }}
                  </span>

                  <div class="flex items-center space-x-1 shrink-0 ml-1">
                    <button
                      @click.stop="editingCardId = card.id; editingTitle = card.title"
                      class="p-0.5 hover:bg-slate-100 text-slate-400 hover:text-slate-600 rounded"
                    >
                      <Edit3 class="h-3 w-3" />
                    </button>
                    <GripVertical class="h-3.5 w-3.5 text-slate-300" />
                  </div>
                </div>

                <p v-if="card.description" class="text-[11px] text-slate-400 line-clamp-2 leading-relaxed">
                  {{ card.description }}
                </p>

                <div class="flex items-center pt-1.5 border-t border-slate-50 text-[9px] text-slate-400">
                  <span class="flex items-center bg-slate-50 border border-slate-100 text-slate-500 px-1.5 py-0.5 rounded-md">
                    <Calendar class="h-2.5 w-2.5 mr-1" />
                    {{ card.dueDate ? new Date(card.dueDate).toLocaleDateString() : '无截止日期' }}
                  </span>
                </div>
              </div>
            </template>
          </draggable>

          <!-- 底部添加卡片 -->
          <div class="mt-2 pt-2 border-t border-slate-200/50">
            <div class="flex space-x-1">
              <input
                type="text"
                placeholder="建新任务卡片..."
                class="flex-1 bg-white border border-slate-200 rounded-lg text-xs px-2 py-1.5 focus:outline-none focus:border-blue-500"
                :value="newCardTitles[list.id] || ''"
                @input="(e) => newCardTitles = { ...newCardTitles, [list.id]: (e.target as HTMLInputElement).value }"
                @keydown.enter="handleCreateCard(list.id)"
              />
              <button
                @click="handleCreateCard(list.id)"
                class="p-1.5 bg-blue-600 hover:bg-blue-700 text-white rounded-lg transition-colors"
              >
                <Plus class="h-3.5 w-3.5" />
              </button>
            </div>
          </div>
        </div>

        <!-- 新建列表 -->
        <div class="w-72 bg-slate-200/40 border border-dashed border-slate-300 rounded-2xl p-4 shrink-0 flex flex-col space-y-2">
          <input
            type="text"
            placeholder="新增阶段列表 (如：已测试)..."
            class="w-full bg-white border border-slate-200 rounded-xl text-xs p-2.5 focus:outline-none focus:border-blue-500 shadow-sm"
            v-model="newListTitle"
            @keydown.enter="handleCreateList"
          />
          <button
            @click="handleCreateList"
            class="w-full py-2 bg-slate-800 hover:bg-slate-900 text-white font-semibold rounded-xl text-xs flex items-center justify-center space-x-1 transition-colors"
          >
            <Layers class="h-3.5 w-3.5" />
            <span>新建工作流阶段</span>
          </button>
        </div>
      </div>
    </template>

    <!-- 空状态 -->
    <div v-else class="flex-1 flex flex-col items-center justify-center text-slate-400 p-8 space-y-2">
      <Kanban class="h-8 w-8 text-slate-300" />
      <p class="text-xs">该隔离工作区内目前未建立敏捷看板，请在上方点击"新建看板"开始协作。</p>
    </div>

    <!-- 卡片详情弹窗 -->
    <CardDetailModal
      v-if="selectedCardId"
      :card-id="selectedCardId"
      :on-close="() => (selectedCardId = null)"
      :on-card-updated="() => { if (boardStore.currentBoard) boardStore.fetchBoardDetail(boardStore.currentBoard.id) }"
    />

    <!-- AI 助手面板 -->
    <div v-if="showAiPanel" class="fixed right-4 top-20 z-40">
      <AiAssistant
        :workspace-id="workspaceId"
        :card-id="selectedCardId || undefined"
        :card-title="boardStore.currentBoard?.lists.flatMap(l => l.cards).find(c => c.id === selectedCardId)?.title"
        :on-close="() => (showAiPanel = false)"
      />
    </div>

    <!-- 仪表盘 -->
    <div v-if="showDashboard" class="fixed right-4 top-20 z-40 w-96">
      <div class="relative">
        <button
          @click="showDashboard = false"
          class="absolute top-3 right-3 p-1 bg-white rounded-lg shadow-sm text-slate-400 hover:text-slate-600 z-10"
        >
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
        </button>
        <DashboardPanel :workspace-id="workspaceId" />
      </div>
    </div>
  </div>
</template>
