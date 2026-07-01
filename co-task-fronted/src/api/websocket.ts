import { Client } from '@stomp/stompjs';
import type { IMessage } from '@stomp/stompjs';
import { useBoardStore } from '../stores/boardStore';

let stompClient: Client | null = null;

/**
 * 激活工作区级看板协同专属 STOMP 管道
 */
export const connectWorkspaceWebSocket = (workspaceId: string) => {
  if (stompClient) {
    stompClient.deactivate();
  }

  const token = localStorage.getItem('cotask_token');
  if (!token) {
    console.warn('未登录，无法建立 WebSocket 连接');
    return;
  }

  const socketUrl = 'ws://localhost:5000/ws';

  stompClient = new Client({
    brokerURL: socketUrl,
    reconnectDelay: 5000,
    connectHeaders: {
      Authorization: `Bearer ${token}`,
    },
    onConnect: () => {
      console.log(`📡 Co-Task 实时长连接激活 (Native WS)。当前隔离工作区: ${workspaceId}`);

      stompClient?.subscribe(`/topic/workspace/${workspaceId}/card/moved`, (message: IMessage) => {
        if (message.body) {
          const payload = JSON.parse(message.body);
          useBoardStore().handleCardMovedEvent({
            cardId: payload.cardId,
            listId: payload.listId,
            position: payload.position,
            title: payload.title,
          });
        }
      });

      stompClient?.subscribe(`/topic/workspace/${workspaceId}/card/updated`, (message: IMessage) => {
        if (message.body) {
          const payload = JSON.parse(message.body);
          useBoardStore().handleCardUpdatedEvent(payload);
        }
      });
    },
    onStompError: (frame) => {
      console.error('STOMP 错误:', frame.headers['message']);
    },
  });

  stompClient.activate();
};

/**
 * 用户释放卡片时，直接向后端的 STOMP 接收端点推送实时变动载荷
 */
export const publishCardMove = (
  cardId: string,
  targetListId: string,
  prevPosition: number | null,
  nextPosition: number | null
) => {
  if (!stompClient || !stompClient.connected) {
    console.error('STOMP 客户端未就绪，持久化流发送失败！');
    return;
  }

  stompClient.publish({
    destination: '/app/board/card/move',
    body: JSON.stringify({ cardId, targetListId, prevPosition, nextPosition }),
  });
};

/**
 * 修改标题与属性的实时推送
 */
export const publishCardUpdate = (
  cardId: string,
  title: string,
  description?: string,
  dueDate?: string
) => {
  if (!stompClient || !stompClient.connected) return;

  stompClient.publish({
    destination: '/app/board/card/update',
    body: JSON.stringify({ cardId, title, description, dueDate }),
  });
};

export const disconnectWorkspaceWebSocket = () => {
  if (stompClient) {
    stompClient.deactivate();
    stompClient = null;
  }
};
