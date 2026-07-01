
package com.cotask.security;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * WebSocket 消息拦截器
 * 从 JWT Token 中提取用户信息，并设置认证信息
 */
@Component
public class AuthChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    public AuthChannelInterceptor(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * 拦截 WebSocket 连接请求，从 Token 中提取用户信息并设置认证
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        // 如果是 CONNECT 命令（客户端连接请求）
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = extractToken(accessor);

            // 验证 Token
            if (token != null && jwtTokenProvider.validateToken(token)) {
                // 从 Token 中提取用户信息
                String userId = jwtTokenProvider.getUserIdFromToken(token);
                String email = jwtTokenProvider.getEmailFromToken(token);

                // 创建认证对象
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        userId, 
                        null, 
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                );

                // 设置认证信息到消息头
                accessor.setUser(authentication);
            } else {
                // Token 无效，拒绝连接
                throw new RuntimeException("无效的连接令牌");
            }
        }

        return message;
    }

    /**
     * 从消息头中提取 JWT Token
     */
    private String extractToken(StompHeaderAccessor accessor) {
        // 从 Authorization 头中提取 Token
        String authorization = accessor.getFirstNativeHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }

        // 尝试从查询参数中获取 Token（某些客户端可能使用这种方式）
        String tokenHeader = accessor.getFirstNativeHeader("token");
        if (tokenHeader != null && !tokenHeader.isEmpty()) {
            return tokenHeader;
        }

        return null;
    }
}
