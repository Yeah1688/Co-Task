package com.cotask.service.impl;

import com.cotask.entity.AiTask;
import com.cotask.repository.AiTaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 异步任务处理器
 *
 * 必须独立成一个类，因为 Spring @Async 通过 AOP 代理实现，
 * 类内部直接调用 @Async 方法不会经过代理，导致异步失效。
 */
@Service
public class AiTaskAsyncProcessor {

    @Autowired
    private AiTaskRepository aiTaskRepository;

    @Value("${ai.api.key:}")
    private String apiKey;

    @Value("${ai.api.url:https://api.openai.com/v1/chat/completions}")
    private String apiUrl;

    @Value("${ai.api.model:doubao-pro-32k}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplateBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .readTimeout(Duration.ofSeconds(120))
            .build();

    /**
     * 异步调用 AI 进行任务拆解（在独立线程中执行）
     */
    @Async
    public void processDecomposeAsync(String taskId, String prompt) {
        AiTask aiTask = aiTaskRepository.findById(taskId).orElse(null);
        if (aiTask == null) return;

        aiTask.setStatus("PROCESSING");
        aiTaskRepository.save(aiTask);

        try {
            String result = callAiApi("你是一个专业的项目管理助手", prompt);
            aiTask.setResult(result);
            aiTask.setStatus("COMPLETED");
            aiTask.setCompletedAt(LocalDateTime.now());
        } catch (Exception e) {
            aiTask.setStatus("FAILED");
            aiTask.setErrorMessage("AI 调用失败: " + e.getMessage());
        }

        aiTaskRepository.save(aiTask);
    }

    /**
     * 异步调用 AI 生成周报（在独立线程中执行）
     */
    @Async
    public void processReportAsync(String taskId, String prompt) {
        AiTask aiTask = aiTaskRepository.findById(taskId).orElse(null);
        if (aiTask == null) return;

        aiTask.setStatus("PROCESSING");
        aiTaskRepository.save(aiTask);

        try {
            String result = callAiApi("你是一个专业的项目经理助手", prompt);
            aiTask.setResult(result);
            aiTask.setStatus("COMPLETED");
            aiTask.setCompletedAt(LocalDateTime.now());
        } catch (Exception e) {
            aiTask.setStatus("FAILED");
            aiTask.setErrorMessage("AI 调用失败: " + e.getMessage());
        }

        aiTaskRepository.save(aiTask);
    }

    /**
     * 调用 AI Chat Completion API（兼容 OpenAI / DeepSeek / 豆包）
     */
    private String callAiApi(String systemPrompt, String userPrompt) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        ));
        requestBody.put("temperature", 0.7);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, entity, Map.class);

        List<?> choices = (List<?>) response.getBody().get("choices");
        Map<?, ?> choice = (Map<?, ?>) choices.get(0);
        Map<?, ?> message = (Map<?, ?>) choice.get("message");
        return (String) message.get("content");
    }
}
