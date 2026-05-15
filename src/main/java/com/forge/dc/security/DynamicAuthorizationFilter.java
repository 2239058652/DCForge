package com.forge.dc.security;

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
            "/v3/api-docs/**"
    );

    private final InterfacePermissionRuleLoader ruleLoader;
    // PathPatternParser 线程安全，复用同一个实例
    private final PathPatternParser patternParser = new PathPatternParser();

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

        if (requiredPermission != null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            if (auth == null || !auth.isAuthenticated()) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            boolean hasPermission = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals(requiredPermission));

            if (!hasPermission) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            // ✅ 有权限，走到这里直接放行
        } else {
            // 未命中规则，默认拒绝
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
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
}