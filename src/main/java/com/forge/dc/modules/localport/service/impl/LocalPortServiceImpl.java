package com.forge.dc.modules.localport.service.impl;

import com.forge.dc.common.exception.BusinessException;
import com.forge.dc.modules.localport.dto.LocalPortQueryDto;
import com.forge.dc.modules.localport.dto.TerminatePortProcessDto;
import com.forge.dc.modules.localport.service.LocalPortService;
import com.forge.dc.modules.localport.support.PortProcessTerminatePolicy;
import com.forge.dc.modules.localport.support.PortProcessTerminatePolicy.AllowCheckResult;
import com.forge.dc.modules.localport.support.PowerShellCommandRunner;
import com.forge.dc.modules.localport.support.PowerShellConnectionDto;
import com.forge.dc.modules.localport.support.PowerShellProcessDto;
import com.forge.dc.modules.localport.vo.LocalPortVO;
import com.forge.dc.modules.localport.vo.TerminatePortProcessVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocalPortServiceImpl implements LocalPortService {

    private final PowerShellCommandRunner powerShellCommandRunner;
    private final PortProcessTerminatePolicy terminatePolicy;

    @Override
    public List<LocalPortVO> queryPorts(LocalPortQueryDto dto) {
        List<PowerShellConnectionDto> tcpConnections = queryTcpConnections();
        List<PowerShellConnectionDto> udpConnections = queryUdpConnections();

        Set<Long> pids = new HashSet<>();
        tcpConnections.forEach(c -> pids.add(c.getOwningProcess()));
        udpConnections.forEach(c -> pids.add(c.getOwningProcess()));
        pids.remove(null);
        Map<Long, PowerShellProcessDto> processMap = batchQueryProcessInfo(pids);

        List<LocalPortVO> allPorts = new ArrayList<>();
        for (PowerShellConnectionDto conn : tcpConnections) {
            allPorts.add(buildVo(conn, processMap.get(conn.getOwningProcess())));
        }
        for (PowerShellConnectionDto conn : udpConnections) {
            allPorts.add(buildVo(conn, processMap.get(conn.getOwningProcess())));
        }

        allPorts.forEach(this::fillTerminatePolicy);

        return applyFilters(allPorts, dto);
    }

    @Override
    public TerminatePortProcessVO terminateProcess(TerminatePortProcessDto dto) {
        if (!Boolean.TRUE.equals(dto.getConfirm())) {
            throw new BusinessException(400, "confirm 必须为 true");
        }

        // 1. 重新查询当前端口列表，校验 PID + protocol + port 仍然匹配
        List<LocalPortVO> currentPorts = queryPorts(new LocalPortQueryDto());
        Optional<LocalPortVO> matched = currentPorts.stream()
                .filter(p -> p.getPid() != null && p.getPid().equals(dto.getPid()))
                .filter(p -> Objects.equals(p.getProtocol(), dto.getProtocol()))
                .filter(p -> p.getLocalPort() != null && p.getLocalPort().equals(dto.getPort()))
                .findFirst();

        if (matched.isEmpty()) {
            throw new BusinessException(404, "未找到该 PID 对应的端口记录，可能进程已变化");
        }

        LocalPortVO target = matched.get();

        // 2. 安全策略检查
        AllowCheckResult checkResult = terminatePolicy.check(target);
        if (!checkResult.canTerminate()) {
            log.warn("进程结束被拒绝: pid={}, port={}, protocol={}, reason={}",
                    dto.getPid(), dto.getPort(), dto.getProtocol(), checkResult.blockedReason());
            throw new BusinessException(403, checkResult.blockedReason());
        }

        // 3. 执行 Stop-Process
        String processName = target.getProcessName();
        executeStopProcess(target.getPid());

        // 4. 再次查询该端口，确认是否释放
        List<LocalPortVO> afterPorts = queryPorts(new LocalPortQueryDto());
        boolean portReleased = afterPorts.stream()
                .noneMatch(p -> Objects.equals(p.getProtocol(), dto.getProtocol())
                        && Objects.equals(p.getLocalPort(), dto.getPort())
                        && Objects.equals(p.getPid(), dto.getPid()));

        log.info("结束进程: pid={}, protocol={}, port={}, processName={}, success={}, portReleased={}",
                dto.getPid(), dto.getProtocol(), dto.getPort(), processName, true, portReleased);

        TerminatePortProcessVO vo = new TerminatePortProcessVO();
        vo.setPid(dto.getPid());
        vo.setProtocol(dto.getProtocol());
        vo.setPort(dto.getPort());
        vo.setProcessName(processName);
        vo.setTerminated(true);
        vo.setPortReleased(portReleased);
        return vo;
    }

    private List<PowerShellConnectionDto> queryTcpConnections() {
        String command = "Get-NetTCPConnection | Select-Object @{Name='protocol';Expression={'tcp'}},LocalAddress,LocalPort,RemoteAddress,RemotePort,State,OwningProcess | ConvertTo-Json -Depth 3";
        try {
            return powerShellCommandRunner.execute(command, PowerShellConnectionDto.class);
        } catch (Exception e) {
            log.error("查询 TCP 端口失败", e);
            return Collections.emptyList();
        }
    }

    private List<PowerShellConnectionDto> queryUdpConnections() {
        String command = "Get-NetUDPEndpoint | Select-Object @{Name='protocol';Expression={'udp'}},LocalAddress,LocalPort,@{Name='RemoteAddress';Expression={$null}},@{Name='RemotePort';Expression={$null}},@{Name='State';Expression={'None'}},OwningProcess | ConvertTo-Json -Depth 3";
        try {
            return powerShellCommandRunner.execute(command, PowerShellConnectionDto.class);
        } catch (Exception e) {
            log.error("查询 UDP 端口失败", e);
            return Collections.emptyList();
        }
    }

    private Map<Long, PowerShellProcessDto> batchQueryProcessInfo(Set<Long> pids) {
        if (pids.isEmpty()) {
            return Collections.emptyMap();
        }
        String idList = pids.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        String command = String.format(
                "$ids = @(%s); Get-CimInstance Win32_Process | Where-Object { $ids -contains [int]$_.ProcessId } | Select-Object ProcessId,Name,ExecutablePath,CommandLine | ConvertTo-Json -Depth 3",
                idList);

        try {
            List<PowerShellProcessDto> processes = powerShellCommandRunner.execute(command, PowerShellProcessDto.class);
            return processes.stream()
                    .filter(p -> p.getProcessId() != null)
                    .collect(Collectors.toMap(
                            PowerShellProcessDto::getProcessId,
                            p -> p,
                            (existing, replacement) -> existing));
        } catch (Exception e) {
            log.error("批量查询进程信息失败", e);
            return Collections.emptyMap();
        }
    }

    private LocalPortVO buildVo(PowerShellConnectionDto conn, PowerShellProcessDto process) {
        LocalPortVO vo = new LocalPortVO();
        vo.setProtocol(conn.getProtocol());
        vo.setLocalAddress(conn.getLocalAddress());
        vo.setLocalPort(conn.getLocalPort());
        vo.setRemoteAddress(conn.getRemoteAddress());
        vo.setRemotePort(conn.getRemotePort());
        vo.setState(conn.getStateText());
        vo.setPid(conn.getOwningProcess());

        if (process != null) {
            vo.setProcessName(process.getName());
            vo.setExecutablePath(process.getExecutablePath());
            vo.setCommandLine(process.getCommandLine());
        }
        return vo;
    }

    private void fillTerminatePolicy(LocalPortVO vo) {
        AllowCheckResult result = terminatePolicy.check(vo);
        vo.setCanTerminate(result.canTerminate());
        vo.setTerminateBlockedReason(result.blockedReason());
    }

    private List<LocalPortVO> applyFilters(List<LocalPortVO> ports, LocalPortQueryDto dto) {
        return ports.stream()
                .filter(p -> !hasText(dto.getProtocol()) || "all".equalsIgnoreCase(dto.getProtocol())
                        || dto.getProtocol().equalsIgnoreCase(p.getProtocol()))
                .filter(p -> !hasText(dto.getState()) || "all".equalsIgnoreCase(dto.getState())
                        || dto.getState().equalsIgnoreCase(p.getState()))
                .filter(p -> dto.getPort() == null || dto.getPort().equals(p.getLocalPort()))
                .filter(p -> Boolean.TRUE.equals(dto.getOnlyWithProcess()) ? p.getPid() != null : true)
                .filter(p -> !hasText(dto.getKeyword()) || matchesKeyword(p, dto.getKeyword()))
                .collect(Collectors.toList());
    }

    private boolean matchesKeyword(LocalPortVO vo, String keyword) {
        String lower = keyword.toLowerCase();
        if (vo.getProcessName() != null && vo.getProcessName().toLowerCase().contains(lower)) {
            return true;
        }
        if (vo.getPid() != null && vo.getPid().toString().contains(lower)) {
            return true;
        }
        if (vo.getLocalAddress() != null && vo.getLocalAddress().toLowerCase().contains(lower)) {
            return true;
        }
        if (vo.getExecutablePath() != null && vo.getExecutablePath().toLowerCase().contains(lower)) {
            return true;
        }
        return false;
    }

    private void executeStopProcess(Long pid) {
        String command = String.format(
                "Stop-Process -Id %d -Force -ErrorAction Stop", pid);
        ProcessBuilder pb = new ProcessBuilder(
                "powershell.exe", "-NoProfile", "-NonInteractive", "-Command", command);
        try {
            Process process = pb.start();
            boolean finished = process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.error("Stop-Process 超时: pid={}", pid);
                throw new BusinessException(500, "结束进程超时");
            }
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                log.error("Stop-Process 失败: pid={}, exitCode={}", pid, exitCode);
                throw new BusinessException(500, "结束进程失败，可能需要管理员权限");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(500, "结束进程被中断");
        } catch (java.io.IOException e) {
            log.error("Stop-Process IO异常: pid={}", pid, e);
            throw new BusinessException(500, "结束进程异常");
        }
    }

    private static boolean hasText(String str) {
        return str != null && !str.isBlank();
    }
}
