
package com.cotask.websocket;

import com.cotask.security.AuthChannelInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket 配置类
 * 配置 STOMP 协议的消息代理、端点和拦截器
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Autowired
    private AuthChannelInterceptor authChannelInterceptor;

    @Autowired
    private PresenceWebSocketInterceptor presenceWebSocketInterceptor;

    /**
     * 注册 STOMP 协议的端点，前端通过此端点连接 WebSocket
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 注册 /ws 端点，使用原生 WebSocket（STOMP over WebSocket）
        // 前端通过 brokerURL: 'ws://localhost:5000/ws' 直连
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*"); // 允许跨域，实际生产环境应配置具体域名
    }

    /**
     * 配置消息代理，指定消息前缀和订阅前缀
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 启用简单的消息代理，处理以 /topic 开头的消息
        registry.enableSimpleBroker("/topic", "/queue");

        // 配置客户端发送消息的前缀，客户端发送的消息需要以 /app 开头
        registry.setApplicationDestinationPrefixes("/app");
    }

    /**
     * 配置消息通道拦截器
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // 添加认证拦截器
        registration.interceptors(authChannelInterceptor);

        // 添加在线状态拦截器
        registration.interceptors(presenceWebSocketInterceptor);
    }
}
