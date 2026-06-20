package com.forge.dc.modules.localport.support;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

/**
 * PowerShell Get-NetTCPConnection / Get-NetUDPEndpoint 输出的原始 JSON DTO。
 * 字段名为 PowerShell 的 PascalCase，通过 @JsonProperty 映射。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PowerShellConnectionDto {

    private String protocol;

    @JsonProperty("LocalAddress")
    private String localAddress;

    @JsonProperty("LocalPort")
    private Integer localPort;

    @JsonProperty("RemoteAddress")
    private String remoteAddress;

    @JsonProperty("RemotePort")
    private Integer remotePort;

    @JsonProperty("State")
    private String state;

    @JsonProperty("OwningProcess")
    private Long owningProcess;

    /**
     * TCP 状态码 → 可读文本映射。
     * 数值来自 PowerShell TcpState 枚举（.NET System.Net.NetworkInformation.TcpState）。
     */
    private static final Map<String, String> TCP_STATE_MAP = Map.ofEntries(
            Map.entry("1", "Closed"),
            Map.entry("2", "Listen"),
            Map.entry("3", "SynSent"),
            Map.entry("4", "SynReceived"),
            Map.entry("5", "Established"),
            Map.entry("6", "FinWait1"),
            Map.entry("7", "FinWait2"),
            Map.entry("8", "CloseWait"),
            Map.entry("9", "Closing"),
            Map.entry("10", "LastAck"),
            Map.entry("11", "TimeWait"),
            Map.entry("12", "DeleteTcb")
    );

    /**
     * 将数字状态码转为可读文本。
     * UDP 端点或无法识别的码返回原始值。
     */
    public String getStateText() {
        if (state == null) {
            return null;
        }
        if ("udp".equalsIgnoreCase(protocol)) {
            return "None";
        }
        return TCP_STATE_MAP.getOrDefault(state, state);
    }
}
