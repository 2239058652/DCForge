package com.forge.dc.modules.localport.dto;

import lombok.Data;

@Data
public class LocalPortQueryDto {

    /**
     * 协议: all | tcp | udp，默认 all
     */
    private String protocol;

    /**
     * 状态: all | listen | established | time_wait | close_wait，默认 all
     */
    private String state;

    /**
     * 端口号 1-65535，可选
     */
    private Integer port;

    /**
     * 按进程名、PID、地址模糊过滤
     */
    private String keyword;

    /**
     * 是否只显示有进程关联的端口
     */
    private Boolean onlyWithProcess;
}
