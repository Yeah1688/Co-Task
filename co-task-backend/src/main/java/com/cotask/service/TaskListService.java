package com.cotask.service;

import com.cotask.entity.TaskList;
import java.util.List;

public interface TaskListService {
    // 创建列表
    TaskList createTaskList(String boardId, String title, Double position);

    // 获取看板的所有列表（已排序）
    List<TaskList> getBoardTaskLists(String boardId);

    // 更新列表
    TaskList updateTaskList(String listId, String title);

    // 删除列表
    void deleteTaskList(String listId);

    // 移动列表（拖拽排序）
    TaskList moveTaskList(String listId, Double prevPosition, Double nextPosition);
}
