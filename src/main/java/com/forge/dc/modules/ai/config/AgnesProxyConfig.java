package com.forge.dc.modules.ai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class AgnesProxyConfig {

    /**
     * 流式响应专用线程池。
     * defaultCandidate = false 使该 Bean 不参与 @ConditionalOnMissingBean 匹配，
     * Boot 的默认 applicationTaskExecutor 自动创建不受影响。
     */
    @Bean(name = "agnesStreamExecutor", destroyMethod = "shutdown", defaultCandidate = false)
    public ThreadPoolExecutor agnesStreamExecutor() {
        return new ThreadPoolExecutor(
                4, 50,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100),
                r -> {
                    Thread t = new Thread(r, "agnes-stream-" + r.hashCode());
                    t.setDaemon(true);
                    return t;
                },
                new ThreadPoolExecutor.AbortPolicy()
        );
    }
}
