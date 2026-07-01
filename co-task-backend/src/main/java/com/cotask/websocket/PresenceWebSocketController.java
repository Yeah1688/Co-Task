
package com.cotask.websocket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

/**
 * 在线状态 WebSocket 控制器
 * 处理用户在线状态相关的消息，如用户加入/离开工作区等
 */
@Controller
public class PresenceWebSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private OnlineUserTracker onlineUserTracker;

    /**
     * 处理用户加入工作区事件
     * @param payload 包含工作区信息的 Map
     * @param authentication 当前用户认证信息
     */
    @MessageMapping("/workspace/join")
    public void handleWorkspaceJoin(@Payload Map<String, Object> payload, Authentication authentication) {
        String workspaceId = (String) payload.get("workspaceId");
        String userId = authentication.getName();

        // 记录用户加入工作区
        onlineUserTracker.addUserToWorkspace(workspaceId, userId);

        // 获取工作区的所有在线用户
        Set<String> onlineUsers = onlineUserTracker.getUsersInWorkspace(workspaceId);

        // 广播用户加入事件给同一工作区的所有用户
        messagingTemplate.convertAndSend("/topic/workspace/" + workspaceId + "/presence/joined", Map.of(
                "userId", userId,
                "userName", authentication.getName(), // 这里实际应该从数据库获取用户名
                "onlineUsers", onlineUsers,
                "timestamp", LocalDateTime.now()
        ));
    }

    /**
     * 处理用户离开工作区事件
     * @param payload 包含工作区信息的 Map
     * @param authentication 当前用户认证信息
     */
    @MessageMapping("/workspace/leave")
    public void handleWorkspaceLeave(@Payload Map<String, Object> payload, Authentication authentication) {
        String workspaceId = (String) payload.get("workspaceId");
        String userId = authentication.getName();

        // 记录用户离开工作区
        onlineUserTracker.removeUserFromWorkspace(workspaceId, userId);

        // 获取工作区的所有在线用户
        Set<String> onlineUsers = onlineUserTracker.getUsersInWorkspace(workspaceId);

        // 广播用户离开事件给同一工作区的所有用户
        messagingTemplate.convertAndSend("/topic/workspace/" + workspaceId + "/presence/left", Map.of(
                "userId", userId,
                "userName", authentication.getName(), // 这里实际应该从数据库获取用户名
                "onlineUsers", onlineUsers,
                "timestamp", LocalDateTime.now()
        ));
    }

    /**
     * 处理用户活动事件（如发送消息、更新卡片等）
     * @param payload 包含活动信息的 Map
     * @param authentication 当前用户认证信息
     */
    @MessageMapping("/workspace/activity")
    public void handleUserActivity(@Payload Map<String, Object> payload, Authentication authentication) {
        String workspaceId = (String) payload.get("workspaceId");
        String activityType = (String) payload.get("activityType");
        String activityDetails = (String) payload.get("activityDetails");

        // 获取当前用户ID
        String userId = authentication.getName();

        // 广播用户活动事件给同一工作区的所有用户
        messagingTemplate.convertAndSend("/topic/workspace/" + workspaceId + "/presence/activity", Map.of(
                "userId", userId,
                "userName", authentication.getName(), // 这里实际应该从数据库获取用户名
                "activityType", activityType,
                "activityDetails", activityDetails,
                "timestamp", LocalDateTime.now()
        ));
    }

    /**
     * 请求在线用户列表
     * @param payload 包含工作区信息的 Map
     * @param authentication 当前用户认证信息
     */
    @MessageMapping("/workspace/presence")
    public void handlePresenceRequest(@Payload Map<String, Object> payload, Authentication authentication) {
        String workspaceId = (String) payload.get("workspaceId");

        // 获取工作区的所有在线用户
        Set<String> onlineUsers = onlineUserTracker.getUsersInWorkspace(workspaceId);

        // 发送在线用户列表给请求者
        messagingTemplate.convertAndSendToUser(authentication.getName(), "/queue/workspace/" + workspaceId + "/presence", Map.of(
                "onlineUsers", onlineUsers,
                "timestamp", LocalDateTime.now()
        ));
    }
}
