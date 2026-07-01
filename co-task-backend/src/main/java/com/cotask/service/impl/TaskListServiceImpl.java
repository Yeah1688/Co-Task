package com.cotask.service.impl;

import com.cotask.entity.Board;
import com.cotask.entity.TaskList;
import com.cotask.repository.BoardRepository;
import com.cotask.repository.TaskListRepository;
import com.cotask.security.WorkspacePermissionChecker;
import com.cotask.service.TaskListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TaskListServiceImpl implements TaskListService {

    @Autowired
    private TaskListRepository taskListRepository;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private WorkspacePermissionChecker permissionChecker;

    @Override
    @Transactional
    public TaskList createTaskList(String boardId, String title, Double position) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new RuntimeException("看板不存在"));

        // 权限校验：ADMIN 及以上角色才能创建列表
        permissionChecker.checkAdminOrThrow(board.getWorkspace().getId(), getCurrentUserId());

        double newPosition = position != null ? position : 1000.0;

        TaskList taskList = TaskList.builder()
                .title(title)
                .position(newPosition)
                .board(board)
                .build();

        return taskListRepository.save(taskList);
    }

    private String getCurrentUserId() {
        // 从 Spring Security 上下文获取当前用户ID
        return org.springframework.security.core.context.SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskList> getBoardTaskLists(String boardId) {
        return taskListRepository.findAll().stream()
                .filter(list -> list.getBoard().getId().equals(boardId))
                .sorted((a, b) -> Double.compare(a.getPosition(), b.getPosition()))
                .toList();
    }

    @Override
    @Transactional
    public TaskList updateTaskList(String listId, String title) {
        TaskList taskList = taskListRepository.findById(listId)
                .orElseThrow(() -> new RuntimeException("列表不存在"));

        // 权限校验：ADMIN 及以上角色才能更新列表
        permissionChecker.checkAdminOrThrow(taskList.getBoard().getWorkspace().getId(), getCurrentUserId());

        if (title != null && !title.trim().isEmpty()) {
            taskList.setTitle(title);
        }

        return taskListRepository.save(taskList);
    }

    @Override
    @Transactional
    public void deleteTaskList(String listId) {
        TaskList taskList = taskListRepository.findById(listId)
                .orElseThrow(() -> new RuntimeException("列表不存在"));

        // 权限校验：ADMIN 及以上角色才能删除列表
        permissionChecker.checkAdminOrThrow(taskList.getBoard().getWorkspace().getId(), getCurrentUserId());

        taskListRepository.delete(taskList);
    }

    @Override
    @Transactional
    public TaskList moveTaskList(String listId, Double prevPosition, Double nextPosition) {
        TaskList taskList = taskListRepository.findById(listId)
                .orElseThrow(() -> new RuntimeException("列表不存在"));

        // 权限校验：ADMIN 及以上角色才能移动列表
        permissionChecker.checkAdminOrThrow(taskList.getBoard().getWorkspace().getId(), getCurrentUserId());

        double newPosition;

        if (prevPosition == null && nextPosition == null) {
            newPosition = 1000.0;
        } else if (prevPosition == null) {
            newPosition = nextPosition / 2.0;
        } else if (nextPosition == null) {
            newPosition = prevPosition + 1000.0;
        } else {
            newPosition = (prevPosition + nextPosition) / 2.0;
        }

        taskList.setPosition(newPosition);
        return taskListRepository.save(taskList);
    }
}
