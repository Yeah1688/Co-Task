
package com.cotask.websocket;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * 在线状态 WebSocket 拦截器
 * 负责追踪用户在线状态，并在用户连接/断开时更新状态
 */
@Component
public class PresenceWebSocketInterceptor implements ChannelInterceptor {

    private final OnlineUserTracker onlineUserTracker;

    public PresenceWebSocketInterceptor(OnlineUserTracker onlineUserTracker) {
        this.onlineUserTracker = onlineUserTracker;
    }

    /**
     * 拦截连接请求，记录用户状态
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        // 如果是 CONNECT 命令（客户端连接）
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            Authentication user = (accessor.getUser() instanceof Authentication) ? (Authentication) accessor.getUser() : null;
            if (user != null && user.isAuthenticated()) {
                String userId = user.getName();

                // 从消息头中获取工作区ID
                String workspaceId = extractWorkspaceId(accessor);

                if (workspaceId != null) {
                    // 记录用户加入工作区
                    onlineUserTracker.addUserToWorkspace(workspaceId, userId);
                }
            }
        }

        // 如果是 DISCONNECT 命令（客户端断开连接）
        else if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
            Authentication user = (accessor.getUser() instanceof Authentication) ? (Authentication) accessor.getUser() : null;
            if (user != null && user.isAuthenticated()) {
                String userId = user.getName();

                // 从消息头中获取工作区ID
                String workspaceId = extractWorkspaceId(accessor);

                if (workspaceId != null) {
                    // 记录用户离开工作区
                    onlineUserTracker.removeUserFromWorkspace(workspaceId, userId);
                }
            }
        }

        return message;
    }

    /**
     * 从消息头中提取工作区ID
     */
    private String extractWorkspaceId(StompHeaderAccessor accessor) {
        // 从自定义头中获取工作区ID
        String workspaceId = accessor.getFirstNativeHeader("workspace-id");
        if (workspaceId != null && !workspaceId.isEmpty()) {
            return workspaceId;
        }

        return null;
    }
}
