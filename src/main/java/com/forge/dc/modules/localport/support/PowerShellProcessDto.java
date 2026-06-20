package com.forge.dc.modules.localport.support;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * PowerShell Get-CimInstance Win32_Process 输出的原始 JSON DTO。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PowerShellProcessDto {

    @JsonProperty("ProcessId")
    private Long processId;

    @JsonProperty("Name")
    private String name;

    @JsonProperty("ExecutablePath")
    private String executablePath;

    @JsonProperty("CommandLine")
    private String commandLine;
}
