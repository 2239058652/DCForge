package com.forge.dc.modules.localport.support;

import com.forge.dc.modules.localport.vo.LocalPortVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.util.Set;

@Component
@Slf4j
public class PortProcessTerminatePolicy {

    /**
     * 禁止结束的系统关键进程名（小写比较）。
     */
    private static final Set<String> BLOCKED_PROCESS_NAMES = Set.of(
            "system",
            "registry",
            "idle",
            "smss.exe",
            "csrss.exe",
            "wininit.exe",
            "winlogon.exe",
            "services.exe",
            "lsass.exe",
            "svchost.exe",
            "fontdrvhost.exe",
            "dwm.exe",
            "explorer.exe"
    );

    private final long currentPid;

    public PortProcessTerminatePolicy() {
        String runtimeName = ManagementFactory.getRuntimeMXBean().getName();
        this.currentPid = Long.parseLong(runtimeName.split("@")[0]);
    }

    /**
     * 判断是否允许结束指定进程。
     * 返回 AllowCheckResult，canTerminate=false 时携带 blockedReason。
     */
    public AllowCheckResult check(LocalPortVO portInfo) {
        Long pid = portInfo.getPid();

        if (pid != null && pid <= 4) {
            return AllowCheckResult.blocked("系统关键 PID (pid<=4) 不允许结束");
        }

        if (pid != null && pid == currentPid) {
            return AllowCheckResult.blocked("当前后端进程不允许结束");
        }

        String processName = portInfo.getProcessName();
        if (processName != null && BLOCKED_PROCESS_NAMES.contains(processName.toLowerCase())) {
            return AllowCheckResult.blocked("系统关键进程 " + processName + " 不允许结束");
        }

        return AllowCheckResult.allowed();
    }

    public long getCurrentPid() {
        return currentPid;
    }

    public record AllowCheckResult(boolean canTerminate, String blockedReason) {
        public static AllowCheckResult allowed() {
            return new AllowCheckResult(true, null);
        }

        public static AllowCheckResult blocked(String reason) {
            return new AllowCheckResult(false, reason);
        }
    }
}
