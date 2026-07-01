import { defineStore } from 'pinia';
import { ref } from 'vue';
import api from '../api/axios';

export interface Card {
  id: string;
  title: string;
  description?: string;
  position: number;
  dueDate?: string;
}

export interface TaskList {
  id: string;
  title: string;
  position: number;
  cards: Card[];
}

export interface Board {
  id: string;
  title: string;
  workspaceId: string;
  lists: TaskList[];
}

export const useBoardStore = defineStore('board', () => {
  const boards = ref<Board[]>([]);
  const currentBoard = ref<Board | null>(null);
  const isLoading = ref(false);

  // 本地乐观更新移动卡片
  function moveCardLocally(
    _cardId: string,
    sourceListId: string,
    targetListId: string,
    sourceIndex: number,
    targetIndex: number
  ) {
    if (!currentBoard.value) return { prevPosition: null as number | null, nextPosition: null as number | null, targetListId };

    const updatedLists: TaskList[] = JSON.parse(JSON.stringify(currentBoard.value.lists));
    const sourceList = updatedLists.find((l) => l.id === sourceListId);
    const targetList = updatedLists.find((l) => l.id === targetListId);

    if (!sourceList || !targetList) return { prevPosition: null as number | null, nextPosition: null as number | null, targetListId };

    const [movedCard] = sourceList.cards.splice(sourceIndex, 1);
    targetList.cards.splice(targetIndex, 0, movedCard);

    const prevCard = targetList.cards[targetIndex - 1];
    const nextCard = targetList.cards[targetIndex + 1];
    const prevPosition = prevCard ? prevCard.position : null;
    const nextPosition = nextCard ? nextCard.position : null;

    currentBoard.value = { ...currentBoard.value, lists: updatedLists };

    return { prevPosition, nextPosition, targetListId };
  }

  // 同步移动到后端并广播
  async function syncCardMoveWithBackend(
    cardId: string,
    targetListId: string,
    prevPosition: number | null,
    nextPosition: number | null
  ) {
    try {
      await api.put(`/cards/${cardId}/move`, {
        targetListId,
        prevPosition,
        nextPosition,
      });
    } catch (error) {
      console.error('同步卡片移动失败', error);
      if (currentBoard.value) fetchBoardDetail(currentBoard.value.id);
    }
  }

  // GET /api/boards/workspace/{workspaceId}
  async function fetchWorkspaceBoards(workspaceId: string) {
    try {
      const res = await api.get(`/boards/workspace/${workspaceId}`);
      boards.value = res.data;
    } catch (err) {
      console.error('加载看板列表失败', err);
    }
  }

  // POST /api/boards
  async function createBoard(workspaceId: string, title: string) {
    try {
      const res = await api.post('/boards', { workspaceId, title });
      boards.value = [...boards.value, res.data.board];
    } catch (err) {
      console.error('创建看板失败', err);
    }
  }

  // GET /api/boards/{boardId}
  async function fetchBoardDetail(boardId: string) {
    isLoading.value = true;
    try {
      const res = await api.get(`/boards/${boardId}`);
      const board = res.data;
      board.lists.sort((a: TaskList, b: TaskList) => a.position - b.position);
      board.lists.forEach((list: TaskList) => list.cards.sort((a: Card, b: Card) => a.position - b.position));
      currentBoard.value = board;
      isLoading.value = false;
    } catch (err) {
      console.error('加载看板详情失败', err);
      isLoading.value = false;
    }
  }

  // POST /api/lists
  async function createList(boardId: string, title: string, lastPosition: number) {
    if (!currentBoard.value) return;
    try {
      const targetPos = lastPosition + 1.0;
      const res = await api.post('/lists', { boardId, title, position: targetPos });
      const newList: TaskList = { ...res.data.list, cards: [] };

      currentBoard.value = {
        ...currentBoard.value,
        lists: [...currentBoard.value.lists, newList].sort((a, b) => a.position - b.position),
      };
    } catch (err) {
      console.error('创建列表工作流失败', err);
    }
  }

  // POST /api/cards
  async function createCard(listId: string, title: string) {
    if (!currentBoard.value) return;
    try {
      const res = await api.post('/cards', { listId, title });
      const newCard: Card = res.data.card;

      const updatedLists = currentBoard.value.lists.map((list) => {
        if (list.id === listId) {
          return { ...list, cards: [...list.cards, newCard].sort((a, b) => a.position - b.position) };
        }
        return list;
      });

      currentBoard.value = { ...currentBoard.value, lists: updatedLists };
    } catch (err) {
      console.error('创建卡片失败', err);
    }
  }

  // 需求 2：高平滑度拖拽排序的核心本地乐观算法
  function optimizeMoveCard(
    _cardId: string,
    sourceListId: string,
    targetListId: string,
    sourceIndex: number,
    targetIndex: number
  ) {
    if (!currentBoard.value) return { prevPosition: null as number | null, nextPosition: null as number | null };

    const updatedLists: TaskList[] = JSON.parse(JSON.stringify(currentBoard.value.lists));
    const sourceList = updatedLists.find((l) => l.id === sourceListId)!;
    const targetList = updatedLists.find((l) => l.id === targetListId)!;

    const [movedCard] = sourceList.cards.splice(sourceIndex, 1);
    targetList.cards.splice(targetIndex, 0, movedCard);

    const prevCard = targetList.cards[targetIndex - 1];
    const nextCard = targetList.cards[targetIndex + 1];
    const prevPosition = prevCard ? prevCard.position : null;
    const nextPosition = nextCard ? nextCard.position : null;

    currentBoard.value = { ...currentBoard.value, lists: updatedLists };

    return { prevPosition, nextPosition };
  }

  // 处理来自工作区频道的其他协同用户的卡片滑行移动广播
  function handleCardMovedEvent(event: { cardId: string; listId: string; position: number; title: string }) {
    if (!currentBoard.value) return;

    const updatedLists: TaskList[] = JSON.parse(JSON.stringify(currentBoard.value.lists));

    let targetCardEntity: Card | null = null;
    for (const list of updatedLists) {
      const idx = list.cards.findIndex((c) => c.id === event.cardId);
      if (idx !== -1) {
        [targetCardEntity] = list.cards.splice(idx, 1);
        break;
      }
    }

    if (!targetCardEntity) {
      targetCardEntity = { id: event.cardId, title: event.title, position: event.position };
    } else {
      targetCardEntity.position = event.position;
    }

    const targetList = updatedLists.find((l) => l.id === event.listId);
    if (targetList) {
      targetList.cards.push(targetCardEntity);
      targetList.cards.sort((a, b) => a.position - b.position);
    }

    currentBoard.value = { ...currentBoard.value, lists: updatedLists };
  }

  // 处理卡片元数据（如标题修改）的实时同屏同步广播
  function handleCardUpdatedEvent(event: { cardId: string; title: string; description?: string; dueDate?: string }) {
    if (!currentBoard.value) return;

    const updatedLists = currentBoard.value.lists.map((list) => {
      const hasCard = list.cards.some((c) => c.id === event.cardId);
      if (!hasCard) return list;

      return {
        ...list,
        cards: list.cards.map((card) => {
          if (card.id === event.cardId) {
            return {
              ...card,
              title: event.title,
              description: event.description,
              dueDate: event.dueDate,
            };
          }
          return card;
        }),
      };
    });

    currentBoard.value = { ...currentBoard.value, lists: updatedLists };
  }

  // 切换工作区时重置所有看板状态
  function resetBoardState() {
    boards.value = [];
    currentBoard.value = null;
    isLoading.value = false;
  }

  return {
    boards,
    currentBoard,
    isLoading,
    resetBoardState,
    moveCardLocally,
    syncCardMoveWithBackend,
    fetchWorkspaceBoards,
    createBoard,
    fetchBoardDetail,
    createList,
    createCard,
    optimizeMoveCard,
    handleCardMovedEvent,
    handleCardUpdatedEvent,
  };
});
