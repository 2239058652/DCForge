package com.forge.dc.modules.ai.task;

import com.forge.dc.common.util.MinioUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class MinioCleanupTask {

    private final MinioUtil minioUtil;

    /**
     * -- GETTER --
     * 获取当前配置的 cron 表达式
     */
    @Getter
    @Value("${minio.cleanup.cron:0 0 2 * * ?}")
    private String cronExpression;

    /**
     * 清理 temp-image 下超过 24 小时的文件
     * cron 表达式通过配置文件按环境设置
     */
    @Scheduled(cron = "${minio.cleanup.cron}")
    public void cleanupTempImages() {
        log.info("=== 定时任务开始：清理临时图片文件 ===");
        try {
            int cleaned = minioUtil.deleteTempOlderThan(Duration.ofHours(24));
            if (cleaned > 0) {
                log.info("=== 定时任务完成：已清理 {} 个过期临时文件 ===", cleaned);
            } else {
                log.info("=== 定时任务完成：没有需要清理的文件 ===");
            }
        } catch (Exception e) {
            log.error("=== 定时任务失败：清理临时图片文件出错 ===", e);
        }
    }

    /**
     * 手动触发清理（供测试接口调用）
     */
    public void triggerCleanup() {
        cleanupTempImages();
    }

}
