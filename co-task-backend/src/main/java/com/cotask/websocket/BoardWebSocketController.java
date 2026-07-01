
package com.cotask.websocket;

import com.cotask.entity.Board;
import com.cotask.entity.Card;
import com.cotask.entity.TaskList;
import com.cotask.entity.User;
import com.cotask.security.AuthChannelInterceptor;
import com.cotask.security.JwtTokenProvider;
import com.cotask.security.WorkspacePermissionChecker;
import com.cotask.service.ActivityLogService;
import com.cotask.service.BoardService;
import com.cotask.service.CardService;
import com.cotask.service.TaskListService;
import com.cotask.repository.UserRepository;
import com.cotask.service.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * WebSocket 控制器
 * 处理看板相关的实时消息，如卡片移动、更新等
 */
@Controller
public class BoardWebSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private WorkspacePermissionChecker permissionChecker;

    @Autowired
    private BoardService boardService;

    @Autowired
    private TaskListService taskListService;

    @Autowired
    private CardService cardService;

    @Autowired
    private WorkspaceService workspaceService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActivityLogService activityLogService;

    /**
     * 处理卡片移动事件
     * @param payload 包含卡片移动信息的 Map
     * @param authentication 当前用户认证信息
     */
    @MessageMapping("/board/card/move")
    public void handleCardMove(@Payload Map<String, Object> payload, Authentication authentication) {
        // WebSocket 线程独立于 HTTP 线程，SecurityContextHolder 是 ThreadLocal，
        // 必须手动注入，否则 Service 层 getCurrentUserId() 会拿到 null
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String cardId = (String) payload.get("cardId");
        String targetListId = (String) payload.get("targetListId");
        // 使用 Number 类型中转，避免 JS JSON.stringify 发来的整数被 Jackson 解析为 Integer
        // 而导致 (Double) 强转抛出 ClassCastException
        Double prevPosition = toDoubleOrNull(payload.get("prevPosition"));
        Double nextPosition = toDoubleOrNull(payload.get("nextPosition"));

        // 获取当前用户ID
        String userId = authentication.getName();

        // 移动卡片
        Card movedCard = cardService.moveCard(cardId, targetListId, prevPosition, nextPosition);

        // 获取卡片所在的工作区ID
        String workspaceId = movedCard.getTaskList().getBoard().getWorkspace().getId();

        // 记录活动日志
        User user = userRepository.findById(userId).orElse(null);
        String userName = user != null ? user.getName() : userId;
        try {
            activityLogService.logActivity(
                "MOVE", "CARD", cardId,
                null, null,
                userName + " 将卡片移到了 " + movedCard.getTaskList().getTitle(),
                workspaceId, userId
            );
        } catch (Exception e) {
            // 日志失败不影响主流程
        }

        // 广播卡片移动事件给同一工作区的所有用户
        messagingTemplate.convertAndSend("/topic/workspace/" + workspaceId + "/card/moved", Map.of(
                "cardId", movedCard.getId(),
                "title", movedCard.getTitle(),
                "listId", movedCard.getTaskList().getId(),
                "position", movedCard.getPosition(),
                "movedBy", userId,
                "timestamp", LocalDateTime.now()
        ));
    }

    /**
     * 处理卡片更新事件
     * @param payload 包含卡片更新信息的 Map
     * @param authentication 当前用户认证信息
     */
    @MessageMapping("/board/card/update")
    public void handleCardUpdate(@Payload Map<String, Object> payload, Authentication authentication) {
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String cardId = (String) payload.get("cardId");
        String title = (String) payload.get("title");
        String description = (String) payload.get("description");
        String dueDate = (String) payload.get("dueDate");

        // 获取当前用户ID
        String userId = authentication.getName();

        // 更新卡片
        Card updatedCard = cardService.updateCard(cardId, title, description, dueDate);

        // 获取卡片所在的工作区ID
        String workspaceId = updatedCard.getTaskList().getBoard().getWorkspace().getId();

        // 记录活动日志
        User user = userRepository.findById(userId).orElse(null);
        String userName = user != null ? user.getName() : userId;
        try {
            String desc = userName + " 修改了卡片";
            if (title != null && !title.isEmpty()) {
                desc += " 标题为: " + title;
            }
            activityLogService.logActivity(
                "UPDATE", "CARD", cardId,
                null, null, desc,
                workspaceId, userId
            );
        } catch (Exception e) {
            // 日志失败不影响主流程
        }

        // 广播卡片更新事件给同一工作区的所有用户
        messagingTemplate.convertAndSend("/topic/workspace/" + workspaceId + "/card/updated", Map.of(
                "cardId", updatedCard.getId(),
                "title", updatedCard.getTitle(),
                "description", updatedCard.getDescription(),
                "dueDate", updatedCard.getDueDate() != null ? updatedCard.getDueDate().toString() : null,
                "updatedBy", userId,
                "timestamp", LocalDateTime.now()
        ));
    }

    /**
     * 安全地将 Object 转换为 Double（兼容 Integer/Long/Float/Double/null）
     */
    private Double toDoubleOrNull(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return null;
    }

    /**
     * 处理列表更新事件
     * @param payload 包含列表更新信息的 Map
     * @param authentication 当前用户认证信息
     */
    @MessageMapping("/board/list/update")
    public void handleListUpdate(@Payload Map<String, Object> payload, Authentication authentication) {
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String listId = (String) payload.get("listId");
        String title = (String) payload.get("title");

        // 获取当前用户ID
        String userId = authentication.getName();

        // 更新列表
        TaskList updatedList = taskListService.updateTaskList(listId, title);

        // 获取列表所在的工作区ID
        String workspaceId = updatedList.getBoard().getWorkspace().getId();

        // 广播列表更新事件给同一工作区的所有用户
        messagingTemplate.convertAndSend("/topic/workspace/" + workspaceId + "/list/updated", Map.of(
                "listId", updatedList.getId(),
                "title", updatedList.getTitle(),
                "updatedBy", userId,
                "timestamp", LocalDateTime.now()
        ));
    }
}
