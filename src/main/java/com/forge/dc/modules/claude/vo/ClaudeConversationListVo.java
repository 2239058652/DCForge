package com.forge.dc.modules.claude.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ClaudeConversationListVo {

    private String sessionId;

    private String title;

    private Integer messageCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastActivity;
}
