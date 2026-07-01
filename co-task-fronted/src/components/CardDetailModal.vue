<script setup lang="ts">
import { ref, onMounted } from 'vue'
import {
  X, Send, Clock, User, MessageSquare, Calendar, Edit3, Save, Loader2,
} from 'lucide-vue-next'
import api from '../api/axios'
import type { Card, Comment, ActivityLog } from '../types'

const props = defineProps<{
  cardId: string
  onClose: () => void
  onCardUpdated?: (card: Card) => void
}>()

const card = ref<Card | null>(null)
const loading = ref(true)
const comments = ref<Comment[]>([])
const activities = ref<ActivityLog[]>([])
const newComment = ref('')
const submittingComment = ref(false)

const editingTitle = ref(false)
const titleValue = ref('')
const editingDesc = ref(false)
const descValue = ref('')
const editingDueDate = ref(false)
const dueDateValue = ref('')
const saving = ref(false)

onMounted(() => {
  if (!props.cardId) return
  loadCardDetail()
  loadComments()
  loadActivities()
})

const loadCardDetail = async () => {
  try {
    const res = await api.get(`/cards/${props.cardId}`)
    card.value = res.data
    titleValue.value = res.data.title
    descValue.value = res.data.description || ''
    if (res.data.dueDate) {
      dueDateValue.value = res.data.dueDate.slice(0, 16)
    } else {
      dueDateValue.value = ''
    }
  } catch (err) {
    console.error('加载卡片详情失败', err)
  } finally {
    loading.value = false
  }
}

const loadComments = async () => {
  try {
    const res = await api.get(`/comments/card/${props.cardId}`)
    comments.value = res.data
  } catch (err) {
    console.error('加载评论失败', err)
  }
}

const loadActivities = async () => {
  try {
    const res = await api.get(`/activities/entity/${props.cardId}`)
    activities.value = res.data
  } catch (err) {
    console.error('加载活动日志失败', err)
  }
}

const saveTitle = async () => {
  if (!titleValue.value.trim() || !card.value) return
  saving.value = true
  try {
    const res = await api.put(`/cards/${props.cardId}`, { title: titleValue.value.trim() })
    const updated = res.data.card
    card.value = updated
    editingTitle.value = false
    props.onCardUpdated?.(updated)
    loadActivities()
  } catch (err) {
    console.error('保存标题失败', err)
  } finally {
    saving.value = false
  }
}

const saveDescription = async () => {
  if (!card.value) return
  saving.value = true
  try {
    const res = await api.put(`/cards/${props.cardId}`, { description: descValue.value })
    const updated = res.data.card
    card.value = updated
    editingDesc.value = false
    props.onCardUpdated?.(updated)
    loadActivities()
  } catch (err) {
    console.error('保存描述失败', err)
  } finally {
    saving.value = false
  }
}

const saveDueDate = async () => {
  if (!card.value) return
  saving.value = true
  try {
    const dueDateIso = dueDateValue.value ? dueDateValue.value + ':00' : ''
    const res = await api.put(`/cards/${props.cardId}`, { dueDate: dueDateIso })
    const updated = res.data.card
    card.value = updated
    editingDueDate.value = false
    props.onCardUpdated?.(updated)
    loadActivities()
  } catch (err) {
    console.error('保存截止日期失败', err)
  } finally {
    saving.value = false
  }
}

const handleAddComment = async () => {
  if (!newComment.value.trim()) return
  submittingComment.value = true
  try {
    await api.post('/comments', { cardId: props.cardId, content: newComment.value.trim() })
    newComment.value = ''
    loadComments()
    loadActivities()
  } catch (err) {
    console.error('发表评论失败', err)
  } finally {
    submittingComment.value = false
  }
}

const clearDueDate = async () => {
  if (!card.value) return
  saving.value = true
  try {
    const res = await api.put(`/cards/${props.cardId}`, { dueDate: '' })
    card.value = res.data.card
    dueDateValue.value = ''
    editingDueDate.value = false
    props.onCardUpdated?.(res.data.card)
    loadActivities()
  } catch (err) {
    console.error('清除截止日期失败', err)
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div v-if="loading" class="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center z-50">
    <div class="bg-white rounded-2xl p-8 shadow-2xl">
      <Loader2 class="h-8 w-8 animate-spin text-blue-600" />
    </div>
  </div>

  <div v-else-if="!card" class="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center z-50">
    <div class="bg-white rounded-2xl p-8 shadow-2xl max-w-sm text-center">
      <p class="text-slate-500">卡片不存在或已被删除</p>
      <button @click="onClose" class="mt-4 px-4 py-2 bg-slate-800 text-white rounded-xl text-sm">关闭</button>
    </div>
  </div>

  <div v-else class="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-start justify-center z-50 p-4 pt-[10vh]">
    <div class="bg-white rounded-2xl shadow-2xl max-w-2xl w-full max-h-[80vh] flex flex-col overflow-hidden">
      <!-- Header -->
      <div class="p-6 border-b border-slate-100 flex items-center justify-between shrink-0">
        <div class="flex items-center space-x-3 flex-1 min-w-0">
          <template v-if="editingTitle">
            <div class="flex items-center space-x-2 flex-1">
              <input
                type="text"
                v-model="titleValue"
                class="flex-1 text-lg font-bold border border-blue-300 rounded-lg px-2 py-1 focus:outline-none focus:border-blue-500"
                autofocus
                @keydown.enter="saveTitle"
              />
              <button @click="saveTitle" :disabled="saving" class="p-1.5 bg-blue-600 text-white rounded-lg">
                <Loader2 v-if="saving" class="h-4 w-4 animate-spin" />
                <Save v-else class="h-4 w-4" />
              </button>
              <button @click="editingTitle = false; titleValue = card.title" class="p-1.5 text-slate-400 hover:text-slate-600">
                <X class="h-4 w-4" />
              </button>
            </div>
          </template>
          <template v-else>
            <h2 class="text-lg font-bold text-slate-800 truncate">{{ card.title }}</h2>
            <button @click="editingTitle = true" class="p-1 text-slate-400 hover:text-blue-500 transition-colors shrink-0">
              <Edit3 class="h-3.5 w-3.5" />
            </button>
          </template>
        </div>
        <button @click="onClose" class="p-1.5 hover:bg-slate-100 rounded-lg text-slate-400 shrink-0 ml-2">
          <X class="h-5 w-5" />
        </button>
      </div>

      <!-- Body -->
      <div class="flex-1 overflow-y-auto p-6 space-y-6">
        <!-- Description -->
        <div>
          <h3 class="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-2">描述</h3>
          <template v-if="editingDesc">
            <div class="space-y-2">
              <textarea
                v-model="descValue"
                class="w-full border border-slate-200 rounded-xl p-3 text-sm min-h-[100px] focus:outline-none focus:border-blue-500"
                placeholder="添加详细描述..."
              ></textarea>
              <div class="flex space-x-2">
                <button @click="saveDescription" :disabled="saving" class="px-3 py-1.5 bg-blue-600 text-white text-xs rounded-lg flex items-center space-x-1">
                  <Loader2 v-if="saving" class="h-3 w-3 animate-spin" />
                  <Save v-else class="h-3 w-3" />
                  <span>保存</span>
                </button>
                <button @click="editingDesc = false; descValue = card.description || ''" class="px-3 py-1.5 text-slate-500 text-xs hover:bg-slate-100 rounded-lg">
                  取消
                </button>
              </div>
            </div>
          </template>
          <template v-else>
            <div
              class="text-sm text-slate-600 leading-relaxed cursor-pointer hover:bg-slate-50 rounded-xl p-2 min-h-[40px]"
              @click="editingDesc = true"
            >
              {{ card.description || '<span class="text-slate-400 italic">点击添加描述...</span>' }}
            </div>
          </template>
        </div>

        <!-- Due Date -->
        <div>
          <h3 class="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-2">截止日期</h3>
          <template v-if="editingDueDate">
            <div class="flex items-center space-x-2">
              <input
                type="datetime-local"
                v-model="dueDateValue"
                class="flex-1 border border-slate-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-blue-500"
              />
              <button @click="saveDueDate" :disabled="saving" class="px-3 py-1.5 bg-blue-600 text-white text-xs rounded-lg flex items-center space-x-1">
                <Loader2 v-if="saving" class="h-3 w-3 animate-spin" />
                <Save v-else class="h-3 w-3" />
                <span>保存</span>
              </button>
              <button
                @click="editingDueDate = false; dueDateValue = card.dueDate ? card.dueDate.slice(0, 16) : ''"
                class="px-3 py-1.5 text-slate-500 text-xs hover:bg-slate-100 rounded-lg"
              >
                取消
              </button>
              <button
                v-if="card.dueDate"
                @click="clearDueDate"
                class="px-3 py-1.5 text-red-500 text-xs hover:bg-red-50 rounded-lg"
              >
                清除
              </button>
            </div>
          </template>
          <template v-else>
            <div
              class="flex items-center space-x-2 cursor-pointer hover:bg-slate-50 rounded-xl p-2"
              @click="editingDueDate = true"
            >
              <Calendar class="h-4 w-4 text-slate-400" />
              <span :class="['text-sm', card.dueDate ? 'font-medium text-slate-700' : 'text-slate-400 italic']">
                {{ card.dueDate ? new Date(card.dueDate).toLocaleString('zh-CN') : '点击设置截止日期...' }}
              </span>
              <Edit3 class="h-3 w-3 text-slate-300" />
            </div>
          </template>
        </div>

        <!-- Activity Log -->
        <div>
          <h3 class="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-3 flex items-center space-x-2">
            <Clock class="h-3.5 w-3.5" />
            <span>活动日志</span>
          </h3>
          <p v-if="activities.length === 0" class="text-xs text-slate-400 italic">暂无活动记录</p>
          <div v-else class="space-y-2">
            <div v-for="activity in activities.slice(0, 20)" :key="activity.id" class="flex items-start space-x-3 text-xs">
              <div class="w-1.5 h-1.5 rounded-full bg-slate-300 mt-1.5 shrink-0" />
              <div>
                <span class="text-slate-600">{{ activity.description }}</span>
                <span class="text-slate-400 ml-2">{{ new Date(activity.createdAt).toLocaleString('zh-CN') }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Comments -->
        <div>
          <h3 class="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-3 flex items-center space-x-2">
            <MessageSquare class="h-3.5 w-3.5" />
            <span>评论 ({{ comments.length }})</span>
          </h3>

          <div class="space-y-3 mb-4">
            <div v-for="comment in comments" :key="comment.id" class="bg-slate-50 rounded-xl p-3">
              <div class="flex items-center justify-between mb-1">
                <div class="flex items-center space-x-2">
                  <div class="w-6 h-6 rounded-full bg-blue-100 text-blue-600 flex items-center justify-center text-xs font-bold">
                    {{ comment.user?.name?.charAt(0) || '?' }}
                  </div>
                  <span class="text-xs font-semibold text-slate-700">{{ comment.user?.name || '未知用户' }}</span>
                </div>
                <span class="text-[10px] text-slate-400">{{ new Date(comment.createdAt).toLocaleString('zh-CN') }}</span>
              </div>
              <p class="text-sm text-slate-600 ml-8">{{ comment.content }}</p>
            </div>
            <p v-if="comments.length === 0" class="text-xs text-slate-400 italic">暂无评论</p>
          </div>

          <!-- Add Comment -->
          <div class="flex items-start space-x-2">
            <div class="w-7 h-7 rounded-full bg-blue-100 text-blue-600 flex items-center justify-center text-xs font-bold shrink-0 mt-1">
              <User class="h-3.5 w-3.5" />
            </div>
            <div class="flex-1 flex space-x-2">
              <input
                type="text"
                placeholder="添加评论..."
                v-model="newComment"
                @keydown.enter="handleAddComment"
                class="flex-1 border border-slate-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-blue-500"
              />
              <button
                @click="handleAddComment"
                :disabled="!newComment.trim() || submittingComment"
                class="p-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 transition-colors"
              >
                <Loader2 v-if="submittingComment" class="h-4 w-4 animate-spin" />
                <Send v-else class="h-4 w-4" />
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
