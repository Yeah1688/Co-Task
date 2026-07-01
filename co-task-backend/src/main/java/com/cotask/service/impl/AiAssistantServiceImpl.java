package com.cotask.service.impl;

import com.cotask.entity.*;
import com.cotask.repository.*;
import com.cotask.service.AiAssistantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AiAssistantServiceImpl implements AiAssistantService {

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private AiTaskRepository aiTaskRepository;

    @Autowired
    private AiTaskAsyncProcessor asyncProcessor;

    @Override
    public AiTask decomposeTask(String cardId, String userId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new RuntimeException("卡片不存在"));

        String prompt = String.format(
                "请根据以下任务标题和描述，将其拆解为3-5个具体的子任务。\n" +
                        "任务标题：%s\n" +
                        "任务描述：%s\n" +
                        "请以JSON数组格式返回，每个子任务包含title和description字段。",
                card.getTitle(),
                card.getDescription() != null ? card.getDescription() : "无"
        );

        // 1. 同步创建 AiTask 并立即返回 taskId
        AiTask aiTask = AiTask.builder()
                .taskType("DECOMPOSE")
                .sourceCardId(cardId)
                .workspaceId(card.getTaskList().getBoard().getWorkspace().getId())
                .prompt(prompt)
                .status("PENDING")
                .build();

        aiTask = aiTaskRepository.save(aiTask);

        // 2. 通过独立 Bean 触发异步 AI 调用（跨类调用才能经过 Spring AOP 代理）
        asyncProcessor.processDecomposeAsync(aiTask.getId(), prompt);

        return aiTask;
    }

    @Override
    public AiTask generateWeeklyReport(String workspaceId, String userId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new RuntimeException("工作区不存在"));

        List<Card> allCards = workspace.getBoards().stream()
                .flatMap(board -> board.getLists().stream())
                .flatMap(list -> list.getCards().stream())
                .collect(Collectors.toList());

        List<Card> completedCards = allCards.stream()
                .filter(card -> {
                    String listTitle = card.getTaskList().getTitle().toLowerCase();
                    return listTitle.contains("完成") || listTitle.contains("done");
                })
                .collect(Collectors.toList());

        List<Card> inProgressCards = allCards.stream()
                .filter(card -> {
                    String listTitle = card.getTaskList().getTitle().toLowerCase();
                    return listTitle.contains("进行") || listTitle.contains("progress");
                })
                .collect(Collectors.toList());

        String prompt = String.format(
                "请根据以下信息生成一份结构化的项目进度周报：\n" +
                        "工作区名称：%s\n" +
                        "已完成任务数量：%d\n" +
                        "进行中任务数量：%d\n" +
                        "\n已完成的任务：\n%s\n" +
                        "\n进行中的任务：\n%s\n" +
                        "\n请以Markdown格式输出，包含：本周完成、进行中工作、下周计划、风险与建议。",
                workspace.getName(),
                completedCards.size(),
                inProgressCards.size(),
                formatCardList(completedCards),
                formatCardList(inProgressCards)
        );

        // 1. 同步创建 AiTask 并立即返回 taskId
        AiTask aiTask = AiTask.builder()
                .taskType("SUMMARY")
                .workspaceId(workspaceId)
                .prompt(prompt)
                .status("PENDING")
                .build();

        aiTask = aiTaskRepository.save(aiTask);

        // 2. 通过独立 Bean 触发异步 AI 调用
        asyncProcessor.processReportAsync(aiTask.getId(), prompt);

        return aiTask;
    }

    @Override
    public List<Map<String, Object>> analyzeRisks(String workspaceId, String userId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new RuntimeException("工作区不存在"));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threeDaysLater = now.plusDays(3);

        List<Card> allCards = workspace.getBoards().stream()
                .flatMap(board -> board.getLists().stream())
                .flatMap(list -> list.getCards().stream())
                .collect(Collectors.toList());

        List<Map<String, Object>> risks = new ArrayList<>();

        for (Card card : allCards) {
            if (card.getDueDate() != null && card.getDueDate().isAfter(now) && card.getDueDate().isBefore(threeDaysLater)) {
                String listTitle = card.getTaskList().getTitle().toLowerCase();

                if (listTitle.contains("未开始") || listTitle.contains("todo") || listTitle.contains("待处理")) {
                    Map<String, Object> risk = new HashMap<>();
                    risk.put("cardId", card.getId());
                    risk.put("cardTitle", card.getTitle());
                    risk.put("dueDate", card.getDueDate());
                    risk.put("currentStatus", card.getTaskList().getTitle());
                    risk.put("daysUntilDue", ChronoUnit.DAYS.between(now, card.getDueDate()));
                    risk.put("riskLevel", "HIGH");
                    risk.put("message", String.format("任务 \"%s\" 将在 %d 天后到期，但目前仍处于未开始状态",
                            card.getTitle(), ChronoUnit.DAYS.between(now, card.getDueDate())));

                    risks.add(risk);
                }
            }
        }

        risks.sort((a, b) -> ((Long) a.get("daysUntilDue")).compareTo((Long) b.get("daysUntilDue")));

        return risks;
    }

    @Override
    public AiTask getAiTaskStatus(String taskId) {
        return aiTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("AI任务不存在"));
    }

    private String formatCardList(List<Card> cards) {
        return cards.stream()
                .map(card -> String.format("- %s%s",
                        card.getTitle(),
                        card.getDueDate() != null ? " (截止: " + card.getDueDate().toLocalDate() + ")" : ""))
                .collect(Collectors.joining("\n"));
    }
}
