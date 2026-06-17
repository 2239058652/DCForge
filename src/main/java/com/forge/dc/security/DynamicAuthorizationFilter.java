package com.forge.dc.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forge.dc.common.result.Result;
import com.forge.dc.common.result.ResultCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.PathContainer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class DynamicAuthorizationFilter extends OncePerRequestFilter {

    private static final List<String> WHITE_LIST = List.of(
            "/users/login",
            "/users/register",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/admin/**",   // 管理接口走 @PreAuthorize，不走动态规则
            "/ws/**"       // WebSocket 走自己的 token 认证，不走接口权限
    );

    private final InterfacePermissionRuleLoader ruleLoader;
    // PathPatternParser 线程安全，复用同一个实例
    private final PathPatternParser patternParser = new PathPatternParser();

    private final ObjectMapper objectMapper;

    private boolean isWhitelisted(String uri) {
        PathContainer pathContainer = PathContainer.parsePath(uri);
        return WHITE_LIST.stream().anyMatch(pattern -> {
            try {
                return patternParser.parse(pattern).matches(pathContainer);
            } catch (Exception e) {
                return false;
            }
        });
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {

        String method = request.getMethod();
        String uri = request.getRequestURI();

        // 白名单直接放行
        if (isWhitelisted(uri)) {
            chain.doFilter(request, response);
            return;
        }

        String requiredPermission = matchPermission(method, uri);

        // 配置了 PERMIT_ALL 的接口直接放行
        if ("PERMIT_ALL".equals(requiredPermission)) {
            chain.doFilter(request, response);
            return;
        }

        if (requiredPermission != null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            // 1. 未认证
            if (auth == null || !auth.isAuthenticated()) {
                writeResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                        Result.fail(ResultCode.UNAUTHORIZED, "请先登录"));
                return; // ✅ 写入响应后必须 return，截断请求
            }

            boolean hasPermission = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals(requiredPermission));

            // 2. 有角色但无该权限
            if (!hasPermission) {
                writeResponse(response, HttpServletResponse.SC_FORBIDDEN,
                        Result.fail(ResultCode.FORBIDDEN, "无权限访问"));
                return; // ✅ 写入响应后必须 return，截断请求
            }

            // ✅ 有权限，走到这里直接放行
        } else {
            // 3. 未命中任何规则，默认拒绝
            writeResponse(response, HttpServletResponse.SC_FORBIDDEN,
                    Result.fail(ResultCode.FORBIDDEN, "未命中规则，拒绝访问"));
            return; // ✅ 写入响应后必须 return，截断请求
        }

        chain.doFilter(request, response);
    }

    private String matchPermission(String method, String uri) {
        Map<String, String> rules = ruleLoader.getLocalRules();
        PathContainer pathContainer = PathContainer.parsePath(uri);

        for (Map.Entry<String, String> entry : rules.entrySet()) { // ✅ String, String
            String[] parts = entry.getKey().split(":", 2);
            if (!parts[0].equals(method)) continue;

            try {
                PathPattern pattern = patternParser.parse(parts[1]);
                if (pattern.matches(pathContainer)) {
                    return entry.getValue();
                }
            } catch (Exception e) {
                log.warn("无效的路径规则: {}", entry.getKey());
            }
        }
        return null;
    }

    /**
     * ✅ 核心修复：在 Filter 层直接将 Result 写入 Response
     */
    private void writeResponse(HttpServletResponse response, int status, Result<?> result) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}