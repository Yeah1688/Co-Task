// Card 卡片类型
export interface Card {
  id: string;
  title: string;
  description?: string;
  position: number;
  dueDate?: string;
  createdAt?: string;
  updatedAt?: string;
}

// TaskList 任务列表类型
export interface TaskList {
  id: string;
  title: string;
  position: number;
  cards: Card[];
}

// Board 看板类型
export interface BoardData {
  id: string;
  title: string;
  lists: TaskList[];
}

// Workspace 工作区类型
export interface Workspace {
  id: string;
  name: string;
  role: 'OWNER' | 'ADMIN' | 'MEMBER';
}

// User 用户类型
export interface User {
  id: string;
  name: string;
  email: string;
}

// Comment 评论类型
export interface Comment {
  id: string;
  content: string;
  user: {
    id: string;
    name: string;
    email: string;
  };
  createdAt: string;
  updatedAt: string;
}

// ActivityLog 活动日志类型
export interface ActivityLog {
  id: string;
  actionType: string;
  entityType: string;
  entityId: string;
  description: string;
  user: {
    id: string;
    name: string;
    email: string;
  };
  workspace: {
    id: string;
    name: string;
  };
  createdAt: string;
}

// AiTask AI任务类型
export interface AiTask {
  id: string;
  taskType: 'DECOMPOSE' | 'SUMMARY' | 'RISK_ANALYSIS';
  sourceCardId?: string;
  workspaceId?: string;
  prompt: string;
  result?: string;
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  errorMessage?: string;
  createdAt: string;
  completedAt?: string;
}

// Risk 风险预警类型
export interface Risk {
  cardId: string;
  cardTitle: string;
  dueDate: string;
  currentStatus: string;
  daysUntilDue: number;
  riskLevel: 'HIGH' | 'MEDIUM' | 'LOW';
  message: string;
}

// SubTask AI拆解子任务类型
export interface SubTask {
  title: string;
  description: string;
}
