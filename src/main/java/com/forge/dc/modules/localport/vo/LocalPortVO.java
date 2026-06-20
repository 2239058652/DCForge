package com.forge.dc.modules.localport.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LocalPortVO {

    private String protocol;
    private String localAddress;
    private Integer localPort;
    private String remoteAddress;
    private Integer remotePort;
    private String state;
    private Long pid;
    private String processName;
    private String executablePath;
    private String commandLine;
    private Boolean canTerminate;
    private String terminateBlockedReason;
}
