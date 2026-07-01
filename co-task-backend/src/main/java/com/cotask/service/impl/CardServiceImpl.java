package com.cotask.service.impl;

import com.cotask.entity.Card;
import com.cotask.entity.TaskList;
import com.cotask.entity.User;
import com.cotask.repository.CardRepository;
import com.cotask.repository.TaskListRepository;
import com.cotask.repository.UserRepository;
import com.cotask.security.WorkspacePermissionChecker;
import com.cotask.service.ActivityLogService;
import com.cotask.service.CardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CardServiceImpl implements CardService {

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private TaskListRepository taskListRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkspacePermissionChecker permissionChecker;

    @Autowired
    private ActivityLogService activityLogService;

    @Override
    @Transactional
    public Card createCard(String listId, String title, String description, String dueDate) {
        TaskList taskList = taskListRepository.findById(listId)
                .orElseThrow(() -> new RuntimeException("列表不存在"));

        String workspaceId = taskList.getBoard().getWorkspace().getId();
        String userId = getCurrentUserId();

        // 权限校验：必须是工作区成员才能创建卡片
        permissionChecker.checkMemberOrThrow(workspaceId, userId);

        LocalDateTime dueDateTime = null;
        if (dueDate != null && !dueDate.isEmpty()) {
            try {
                dueDateTime = LocalDateTime.parse(dueDate);
            } catch (DateTimeParseException e) {
                throw new RuntimeException("日期格式错误，请使用 ISO 格式：yyyy-MM-ddTHH:mm:ss");
            }
        }

        double maxPosition = taskList.getCards() != null
                ? taskList.getCards().stream()
                .mapToDouble(Card::getPosition)
                .max()
                .orElse(0.0)
                : 0.0;

        Card card = Card.builder()
                .title(title)
                .description(description)
                .position(maxPosition + 1000.0)
                .dueDate(dueDateTime)
                .taskList(taskList)
                .build();

        Card saved = cardRepository.save(card);

        // 记录活动日志
        try {
            User user = userRepository.findById(userId).orElse(null);
            String userName = user != null ? user.getName() : userId;
            activityLogService.logActivity("CREATE", "CARD", saved.getId(),
                    null, null, userName + " 创建了卡片 \"" + saved.getTitle() + "\"",
                    workspaceId, userId);
        } catch (Exception ignored) {}

        return saved;
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
    public List<Card> getListCards(String listId) {
        return cardRepository.findAll().stream()
                .filter(card -> card.getTaskList().getId().equals(listId))
                .sorted((a, b) -> Double.compare(a.getPosition(), b.getPosition()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Card getCardDetail(String cardId) {
        return cardRepository.findById(cardId)
                .orElseThrow(() -> new RuntimeException("卡片不存在"));
    }

    @Override
    @Transactional
    public Card updateCard(String cardId, String title, String description, String dueDate) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new RuntimeException("卡片不存在"));

        String workspaceId = card.getTaskList().getBoard().getWorkspace().getId();
        String userId = getCurrentUserId();

        // 权限校验：必须是工作区成员才能更新卡片
        permissionChecker.checkMemberOrThrow(workspaceId, userId);

        // 收集变更描述用于活动日志
        StringBuilder changes = new StringBuilder();

        if (title != null && !title.trim().isEmpty() && !title.equals(card.getTitle())) {
            changes.append("标题: \"").append(card.getTitle()).append("\" → \"").append(title).append("\"; ");
            card.setTitle(title);
        }

        if (description != null && !description.equals(card.getDescription() != null ? card.getDescription() : "")) {
            changes.append("描述已更新; ");
            card.setDescription(description);
        }

        boolean dueDateChanged = false;
        if (dueDate != null) {
            if (dueDate.isEmpty()) {
                if (card.getDueDate() != null) {
                    changes.append("截止日期已清除; ");
                    dueDateChanged = true;
                }
                card.setDueDate(null);
            } else {
                try {
                    LocalDateTime newDue = LocalDateTime.parse(dueDate);
                    if (!newDue.equals(card.getDueDate())) {
                        changes.append("截止日期已更新; ");
                        dueDateChanged = true;
                    }
                    card.setDueDate(newDue);
                } catch (DateTimeParseException e) {
                    throw new RuntimeException("日期格式错误，请使用 ISO 格式：yyyy-MM-ddTHH:mm:ss");
                }
            }
        }

        Card saved = cardRepository.save(card);

        // 记录活动日志
        if (changes.length() > 0) {
            try {
                User user = userRepository.findById(userId).orElse(null);
                String userName = user != null ? user.getName() : userId;
                activityLogService.logActivity("UPDATE", "CARD", saved.getId(),
                        null, null, userName + " 修改了卡片 \"" + saved.getTitle() + "\": " + changes.toString().trim(),
                        workspaceId, userId);
            } catch (Exception ignored) {}
        }

        return saved;
    }

    @Override
    @Transactional
    public void deleteCard(String cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new RuntimeException("卡片不存在"));

        String workspaceId = card.getTaskList().getBoard().getWorkspace().getId();
        String userId = getCurrentUserId();
        String cardTitle = card.getTitle();

        // 权限校验：必须是工作区成员才能删除卡片
        permissionChecker.checkMemberOrThrow(workspaceId, userId);

        cardRepository.delete(card);

        // 记录活动日志
        try {
            User user = userRepository.findById(userId).orElse(null);
            String userName = user != null ? user.getName() : userId;
            activityLogService.logActivity("DELETE", "CARD", cardId,
                    null, null, userName + " 删除了卡片 \"" + cardTitle + "\"",
                    workspaceId, userId);
        } catch (Exception ignored) {}
    }

    @Override
    @Transactional
    public Card moveCard(String cardId, String targetListId, Double prevPosition, Double nextPosition) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new RuntimeException("未找到该任务卡片"));

        TaskList targetList = taskListRepository.findById(targetListId)
                .orElseThrow(() -> new RuntimeException("未找到目标列表"));

        // 权限校验：必须是工作区成员才能移动卡片
        permissionChecker.checkMemberOrThrow(card.getTaskList().getBoard().getWorkspace().getId(), getCurrentUserId());

        String oldListTitle = card.getTaskList().getTitle();
        card.setTaskList(targetList);

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

        card.setPosition(newPosition);
        Card saved = cardRepository.save(card);

        // 记录活动日志（REST API 路径也会触发；WebSocket 路径在 BoardWebSocketController 中已记录）
        if (!oldListTitle.equals(targetList.getTitle())) {
            try {
                String workspaceId = saved.getTaskList().getBoard().getWorkspace().getId();
                String userId = getCurrentUserId();
                User user = userRepository.findById(userId).orElse(null);
                String userName = user != null ? user.getName() : userId;
                activityLogService.logActivity("MOVE", "CARD", saved.getId(),
                        null, null, userName + " 将卡片 \"" + saved.getTitle() + "\" 从 " + oldListTitle + " 移动到 " + targetList.getTitle(),
                        workspaceId, userId);
            } catch (Exception ignored) {}
        }

        return saved;
    }
}
