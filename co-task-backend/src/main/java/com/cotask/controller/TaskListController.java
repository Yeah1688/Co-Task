package com.cotask.controller;

import com.cotask.entity.TaskList;
import com.cotask.service.TaskListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lists")
@CrossOrigin(origins = "*")
public class TaskListController {

    @Autowired
    private TaskListService taskListService;

    @PostMapping
    public ResponseEntity<?> createTaskList(@RequestBody Map<String, Object> request) {
        try {
            String boardId = (String) request.get("boardId");
            String title = (String) request.get("title");
            Double position = request.get("position") != null
                    ? ((Number) request.get("position")).doubleValue()
                    : null;

            TaskList taskList = taskListService.createTaskList(boardId, title, position);
            return ResponseEntity.ok(Map.of(
                    "message", "列表创建成功",
                    "list", taskList
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/board/{boardId}")
    public ResponseEntity<?> getBoardTaskLists(@PathVariable String boardId) {
        try {
            List<TaskList> lists = taskListService.getBoardTaskLists(boardId);
            return ResponseEntity.ok(lists);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{listId}")
    public ResponseEntity<?> updateTaskList(
            @PathVariable String listId,
            @RequestBody Map<String, String> request
    ) {
        try {
            TaskList taskList = taskListService.updateTaskList(listId, request.get("title"));
            return ResponseEntity.ok(Map.of(
                    "message", "列表更新成功",
                    "list", taskList
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{listId}")
    public ResponseEntity<?> deleteTaskList(@PathVariable String listId) {
        try {
            taskListService.deleteTaskList(listId);
            return ResponseEntity.ok(Map.of("message", "列表已删除"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{listId}/move")
    public ResponseEntity<?> moveTaskList(
            @PathVariable String listId,
            @RequestBody Map<String, Object> requestBody
    ) {
        try {
            Double prevPosition = requestBody.get("prevPosition") != null
                    ? ((Number) requestBody.get("prevPosition")).doubleValue()
                    : null;
            Double nextPosition = requestBody.get("nextPosition") != null
                    ? ((Number) requestBody.get("nextPosition")).doubleValue()
                    : null;

            TaskList updatedList = taskListService.moveTaskList(listId, prevPosition, nextPosition);
            return ResponseEntity.ok(Map.of(
                    "id", updatedList.getId(),
                    "title", updatedList.getTitle(),
                    "position", updatedList.getPosition()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
