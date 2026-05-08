package com.forge.dc.security;

import com.forge.dc.common.util.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器，每个请求执行一次，负责从 Authorization 头中提取 Token 并设置安全上下文。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;

    public JwtAuthenticationFilter(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        // 1. 获取请求头 Authorization
        String authorization = request.getHeader("Authorization");

        // 2. 如果没有 Authorization 头或不是 Bearer 开头，直接放行（让后续安全机制处理）
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. 提取 Token 字符串（去掉 "Bearer " 前缀）
        String token = authorization.substring(7);

        // 4. 验证 Token 有效性（如过期、签名等）
        if (!jwtUtils.validateToken(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 5. 解析 Token 中的负载信息
        Claims claims = jwtUtils.parseToken(token);

        // 6. 从 Claims 中获取用户标识及属性
        Long userId = Long.valueOf(claims.getSubject());
        String username = claims.get("username", String.class);
        String role = claims.get("role", String.class);

        // 7. 构造应用内的用户主体对象，包含权限信息
        LoginUser loginUser = new LoginUser(userId, username, role);

        // 8. 创建 Spring Security 的认证令牌，标记为已认证
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        loginUser,
                        null,
                        loginUser.getAuthorities()
                );

        // 9. 将认证信息存入安全上下文，供后续业务获取当前用户
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 10. 继续执行过滤器链
        filterChain.doFilter(request, response);
    }
}