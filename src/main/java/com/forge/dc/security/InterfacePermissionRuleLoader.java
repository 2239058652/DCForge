package com.forge.dc.security;

import com.forge.dc.Interface.entity.InterfacePermission;
import com.forge.dc.Interface.service.InterfacePermissionService;
import com.forge.dc.common.util.InterfacePermissionCacheUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class InterfacePermissionRuleLoader implements ApplicationListener<ContextRefreshedEvent> {

    private final InterfacePermissionService permissionService;
    private final InterfacePermissionCacheUtils cacheUtils;
    // volatile 保证多线程可见性
    @Getter
    private volatile Map<String, String> localRules = Collections.emptyMap();

    @Override
    public void onApplicationEvent(@NonNull ContextRefreshedEvent event) {
        refresh();
    }

    public void refresh() {
        List<InterfacePermission> rules = permissionService.listAll();
        Map<String, String> map = rules.stream().collect(
                Collectors.toMap(
                        r -> r.getHttpMethod() + ":" + r.getUrlPattern(),
                        InterfacePermission::getPermissionCode
                )
        );
        cacheUtils.putAll(map);    // 同步 Redis（多实例场景用）
        this.localRules = map;     // 同步本地内存
    }

}