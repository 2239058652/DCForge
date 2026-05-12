package com.forge.dc.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forge.dc.config.JwtProperties;
import com.forge.dc.users.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

@Slf4j
@Component
public class UserAuthCacheManagerUtils {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final UserMapper userMapper;
    private final Duration ttl;

    public UserAuthCacheManagerUtils(StringRedisTemplate redisTemplate,
                                     ObjectMapper objectMapper,
                                     UserMapper userMapper,
                                     JwtProperties jwtProperties) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.userMapper = userMapper;
        this.ttl = Duration.ofMillis(jwtProperties.getExpire());
    }

    // ========== 主动写入（登录时用） ==========
    public void save(Long userId, List<String> roles, List<String> permissions) {
        try {
            String rolesKey = rolesKey(userId);
            String permissionsKey = permissionsKey(userId);
            redisTemplate.opsForValue().set(rolesKey,
                    objectMapper.writeValueAsString(roles), ttl);
            redisTemplate.opsForValue().set(permissionsKey,
                    objectMapper.writeValueAsString(permissions), ttl);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize auth cache for userId: {}", userId, e);
        }
    }

    // ========== 获取角色（优先缓存，miss 查库并回写） ==========
    public List<String> getRoles(Long userId) {
        return getOrFetch(rolesKey(userId),
                () -> userMapper.findRoleCodesByUserId(userId));
    }

    // ========== 获取权限（同上） ==========
    public List<String> getPermissions(Long userId) {
        return getOrFetch(permissionsKey(userId),
                () -> userMapper.findPermissionCodesByUserId(userId));
    }

    // ========== 清除缓存 ==========
    public void evict(Long userId) {
        redisTemplate.delete(rolesKey(userId));
        redisTemplate.delete(permissionsKey(userId));
    }

    // ========== 内部工具方法 ==========
    private List<String> getOrFetch(String key, Supplier<List<String>> dbFetcher) {
        // 1. 读缓存
        String json = redisTemplate.opsForValue().get(key);
        if (json != null) {
            try {
                return objectMapper.readValue(json, new TypeReference<List<String>>() {
                });
            } catch (JsonProcessingException e) {
                log.error("Failed to deserialize cache key: {}, will fetch from db", key, e);
            }
        }

        // 2. 缓存 miss，从数据库加载
        List<String> data = dbFetcher.get();
        if (data == null) {
            data = List.of(); // 避免存 null
        }

        // 3. 回写缓存
        try {
            redisTemplate.opsForValue().set(key,
                    objectMapper.writeValueAsString(data), ttl);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize data for cache key: {}", key, e);
        }
        return data;
    }

    private String rolesKey(Long userId) {
        return "user:roles:" + userId;
    }

    private String permissionsKey(Long userId) {
        return "user:permissions:" + userId;
    }
}