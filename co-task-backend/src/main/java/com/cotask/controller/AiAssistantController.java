package com.cotask.controller;

import com.cotask.entity.AiTask;
import com.cotask.service.AiAssistantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
public class AiAssistantController {

    @Autowired
    private AiAssistantService aiAssistantService;

    @PostMapping("/decompose")
    public ResponseEntity<?> decomposeTask(@RequestBody Map<String, String> request) {
        String currentUserId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        try {
            AiTask task = aiAssistantService.decomposeTask(request.get("cardId"), currentUserId);
            return ResponseEntity.ok(Map.of(
                    "message", "AI正在拆解任务，请稍后查询结果",
                    "taskId", task.getId()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/summary/weekly-report")
    public ResponseEntity<?> generateWeeklyReport(@RequestBody Map<String, String> request) {
        String currentUserId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        try {
            AiTask task = aiAssistantService.generateWeeklyReport(request.get("workspaceId"), currentUserId);
            return ResponseEntity.ok(Map.of(
                    "message", "AI正在生成周报，请稍后查询结果",
                    "taskId", task.getId()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/risks/workspace/{workspaceId}")
    public ResponseEntity<?> analyzeRisks(@PathVariable String workspaceId) {
        String currentUserId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        try {
            List<Map<String, Object>> risks = aiAssistantService.analyzeRisks(workspaceId, currentUserId);
            return ResponseEntity.ok(Map.of(
                    "workspaceId", workspaceId,
                    "totalRisks", risks.size(),
                    "risks", risks
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/task/{taskId}")
    public ResponseEntity<?> getAiTaskStatus(@PathVariable String taskId) {
        try {
            AiTask task = aiAssistantService.getAiTaskStatus(taskId);
            return ResponseEntity.ok(task);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
