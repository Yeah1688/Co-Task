package com.cotask.service;

import com.cotask.entity.AiTask;
import java.util.List;
import java.util.Map;

public interface AiAssistantService {
    AiTask decomposeTask(String cardId, String userId);
    AiTask generateWeeklyReport(String workspaceId, String userId);
    List<Map<String, Object>> analyzeRisks(String workspaceId, String userId);
    AiTask getAiTaskStatus(String taskId);
}
