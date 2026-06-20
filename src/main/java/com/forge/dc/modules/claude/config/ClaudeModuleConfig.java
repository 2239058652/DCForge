package com.forge.dc.modules.claude.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "claude")
public class ClaudeModuleConfig {

    private String basePath = "C:\\Users\\22390\\.claude";
}
