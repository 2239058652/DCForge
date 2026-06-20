package com.forge.dc.modules.localport.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forge.dc.common.exception.BusinessException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class PowerShellCommandRunner {

    private static final int DEFAULT_TIMEOUT_SECONDS = 5;

    private final ObjectMapper objectMapper;

    /**
     * 执行 PowerShell 命令并解析 JSON 输出为指定类型的 List。
     * 支持单条记录返回 JSON 对象、多条记录返回 JSON 数组两种情况。
     */
    public <T> List<T> execute(String command, Class<T> elementClass) {
        PowerShellResult result = executeCommand(command);
        if (!result.isSuccess()) {
            log.error("PowerShell 命令执行失败: command={}, exitCode={}, stderr={}",
                    command, result.getExitCode(), result.getStderr());
            throw new BusinessException(500, "PowerShell 命令执行失败");
        }
        return parseOutput(result.getStdout(), elementClass);
    }

    private PowerShellResult executeCommand(String command) {
        ProcessBuilder pb = new ProcessBuilder(
                "powershell.exe", "-NoProfile", "-NonInteractive", "-Command", command);
        pb.redirectErrorStream(false);

        Process process = null;
        try {
            process = pb.start();
            String stdout = readStream(process.getInputStream());
            String stderr = readStream(process.getErrorStream());

            boolean finished = process.waitFor(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.error("PowerShell 命令超时: {}", command);
                throw new BusinessException(500, "PowerShell 命令执行超时");
            }

            return new PowerShellResult(process.exitValue(), stdout, stderr);
        } catch (IOException | InterruptedException e) {
            if (process != null) {
                process.destroyForcibly();
            }
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("PowerShell 命令执行异常: {}", command, e);
            throw new BusinessException(500, "PowerShell 命令执行异常");
        }
    }

    private String readStream(java.io.InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString().trim();
        }
    }

    /**
     * 解析 PowerShell ConvertTo-Json 输出。
     * 单条记录时返回 JSON 对象，多条时返回 JSON 数组；空输出返回空列表。
     */
    private <T> List<T> parseOutput(String stdout, Class<T> elementClass) {
        if (stdout == null || stdout.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            String trimmed = stdout.trim();
            if (trimmed.startsWith("[")) {
                JavaType listType = objectMapper.getTypeFactory()
                        .constructCollectionType(List.class, elementClass);
                return objectMapper.readValue(trimmed, listType);
            } else {
                T single = objectMapper.readValue(trimmed, elementClass);
                return Collections.singletonList(single);
            }
        } catch (Exception e) {
            log.error("解析 PowerShell JSON 输出失败: {}", stdout, e);
            throw new BusinessException(500, "解析 PowerShell 输出失败");
        }
    }

    @Data
    private static class PowerShellResult {
        private final int exitCode;
        private final String stdout;
        private final String stderr;

        public boolean isSuccess() {
            return exitCode == 0;
        }
    }
}
