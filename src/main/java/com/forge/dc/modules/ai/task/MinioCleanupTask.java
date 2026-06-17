package com.forge.dc.modules.ai.task;

import com.forge.dc.common.util.MinioUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class MinioCleanupTask {

    private final MinioUtil minioUtil;

    /**
     * 每天凌晨 2 点清理 temp-image 下超过 24 小时的文件
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupTempImages() {
        log.info("开始清理临时图片文件...");
        try {
            minioUtil.deleteTempOlderThan(Duration.ofHours(24));
        } catch (Exception e) {
            log.error("清理临时图片文件失败", e);
        }
    }
}
