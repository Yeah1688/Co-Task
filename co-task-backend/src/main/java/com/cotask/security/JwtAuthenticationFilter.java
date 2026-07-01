
package com.cotask.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器
 * 拦截每个请求，从 Authorization 头中提取 JWT Token，
 * 校验通过后将用户信息写入 Spring Security 上下文
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 1. 从请求头中提取 Token
        String token = resolveToken(request);

        // 2. 验证 Token 并设置认证信息
        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            String userId = jwtTokenProvider.getUserIdFromToken(token);
            String email = jwtTokenProvider.getEmailFromToken(token);

            // 构造 Spring Security 认证对象，将 userId 作为 principal
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId,                          // principal: 用户ID
                            null,                            // credentials: 不需要密码
                            List.of(new SimpleGrantedAuthority("ROLE_USER"))  // 默认角色
                    );

            // 写入安全上下文，后续 Controller 可通过 SecurityContextHolder 获取
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // 3. 继续过滤器链
        filterChain.doFilter(request, response);
    }

    /**
     * 从请求头 Authorization: Bearer <token> 中提取 Token
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
