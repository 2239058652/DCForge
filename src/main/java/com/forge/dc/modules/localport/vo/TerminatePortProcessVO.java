package com.forge.dc.modules.localport.vo;

import lombok.Data;

@Data
public class TerminatePortProcessVO {

    private Long pid;
    private String protocol;
    private Integer port;
    private String processName;
    private Boolean terminated;
    private Boolean portReleased;
}
