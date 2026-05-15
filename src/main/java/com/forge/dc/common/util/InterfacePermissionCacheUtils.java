package com.forge.dc.common.util;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class InterfacePermissionCacheUtils {

    private static final String RULES_KEY = "interface:permission:rules";
    private final RedisTemplate<String, String> redisTemplate;

    public void putAll(Map<String, String> rules) {
        redisTemplate.delete(RULES_KEY);
        redisTemplate.opsForHash().putAll(RULES_KEY, rules);
    }

    public Map<Object, Object> getAll() {
        return redisTemplate.opsForHash().entries(RULES_KEY);
    }

    public boolean hasRules() {
        return redisTemplate.hasKey(RULES_KEY);
    }
}