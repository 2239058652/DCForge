package com.forge.dc.modules.claude.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forge.dc.common.exception.BusinessException;
import com.forge.dc.common.result.PageResult;
import com.forge.dc.common.result.ResultCode;
import com.forge.dc.modules.claude.config.ClaudeModuleConfig;
import com.forge.dc.modules.claude.dto.ClaudeProjectPageDto;
import com.forge.dc.modules.claude.service.ClaudeService;
import com.forge.dc.modules.claude.vo.ClaudeConversationDetailVo;
import com.forge.dc.modules.claude.vo.ClaudeConversationListVo;
import com.forge.dc.modules.claude.vo.ClaudeProjectVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClaudeServiceImpl implements ClaudeService {

    private final ClaudeModuleConfig config;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // -- Projects --

    @Override
    public PageResult<ClaudeProjectVo> listProjects(ClaudeProjectPageDto dto) {
        Path projectsDir = getProjectsDir();
        if (!Files.exists(projectsDir) || !Files.isDirectory(projectsDir)) {
            return new PageResult<>(0L, List.of(), dto.getPageNum(), dto.getPageSize());
        }

        List<ClaudeProjectVo> all;
        try (Stream<Path> dirs = Files.list(projectsDir)) {
            all = dirs
                    .filter(Files::isDirectory)
                    .map(this::buildProjectVo)
                    .filter(Objects::nonNull)
                    .filter(p -> dto.getProjectPath() == null
                            || p.getProjectPath().toLowerCase().contains(dto.getProjectPath().toLowerCase()))
                    .sorted(Comparator.comparing(
                            ClaudeProjectVo::getLastModified,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .toList();
        } catch (IOException e) {
            log.warn("Failed to list projects dir", e);
            return new PageResult<>(0L, List.of(), dto.getPageNum(), dto.getPageSize());
        }

        int total = all.size();
        int from = (dto.getPageNum() - 1) * dto.getPageSize();
        int to = Math.min(from + dto.getPageSize(), total);
        List<ClaudeProjectVo> page = from < total ? all.subList(from, to) : List.of();

        return new PageResult<>((long) total, page, dto.getPageNum(), dto.getPageSize());
    }

    private ClaudeProjectVo buildProjectVo(Path projectDir) {
        ClaudeProjectVo vo = new ClaudeProjectVo();
        String dirName = projectDir.getFileName().toString();
        vo.setDirName(dirName);
        vo.setProjectPath(dirName);

        try (Stream<Path> children = Files.list(projectDir)) {
            List<Path> jsonlFiles = children
                    .filter(p -> p.toString().endsWith(".jsonl"))
                    .toList();

            vo.setConversationCount(jsonlFiles.size());

            // 目录最后修改时间
            vo.setLastModified(toLocalDateTime(Files.getLastModifiedTime(projectDir)));

            // 最近一个 jsonl 文件的修改时间作为最后对话时间
            Optional<Path> latest = jsonlFiles.stream()
                    .max(Comparator.comparingLong(p -> {
                        try {
                            return Files.getLastModifiedTime(p).toMillis();
                        } catch (IOException e) {
                            return 0L;
                        }
                    }));
            if (latest.isPresent()) {
                vo.setLastConversationTime(toLocalDateTime(Files.getLastModifiedTime(latest.get())));
            }
        } catch (IOException e) {
            log.warn("Error scanning project dir: {}", projectDir, e);
            vo.setConversationCount(0);
        }
        return vo;
    }

    // -- Conversations --

    @Override
    public List<ClaudeConversationListVo> listConversations(String projectDirName) {
        Path projectDir = getProjectsDir().resolve(projectDirName).normalize();
        validateProjectDir(projectDir);
        if (!Files.exists(projectDir) || !Files.isDirectory(projectDir)) {
            return List.of();
        }

        try (Stream<Path> jsonlFiles = Files.list(projectDir)
                .filter(p -> p.toString().endsWith(".jsonl"))) {
            return jsonlFiles
                    .map(this::buildConversationListVo)
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(
                            ClaudeConversationListVo::getLastActivity,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .toList();
        } catch (IOException e) {
            log.warn("Failed to list conversations in: {}", projectDir, e);
            return List.of();
        }
    }

    private ClaudeConversationListVo buildConversationListVo(Path jsonlFile) {
        ClaudeConversationListVo vo = new ClaudeConversationListVo();
        String fileName = jsonlFile.getFileName().toString();
        vo.setSessionId(fileName.replace(".jsonl", ""));

        int messageCount = 0;
        LocalDateTime lastActivity = null;

        try (java.io.BufferedReader reader = Files.newBufferedReader(jsonlFile, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                messageCount++;
                JsonNode node = objectMapper.readTree(line);

                String ts = getText(node, "timestamp");
                if (ts != null && !ts.isEmpty()) {
                    LocalDateTime ldt = parseTimestamp(ts);
                    if (ldt != null && (lastActivity == null || ldt.isAfter(lastActivity))) {
                        lastActivity = ldt;
                    }
                }

                // 取第一条有效 user 消息作为标题
                if (vo.getTitle() == null && "user".equals(getText(node, "type"))) {
                    String content = extractUserContent(node);
                    if (content != null && !content.startsWith("<local-command-caveat>")) {
                        vo.setTitle(truncate(content, 100));
                    }
                }
            }
        } catch (IOException e) {
            log.warn("Error reading jsonl file: {}", jsonlFile, e);
            return null;
        }

        vo.setMessageCount(messageCount);
        if (vo.getTitle() == null) {
            vo.setTitle("(untitled)");
        }
        if (lastActivity == null) {
            try {
                lastActivity = toLocalDateTime(Files.getLastModifiedTime(jsonlFile));
            } catch (IOException e) {
                log.warn("Failed to get last modified time for: {}", jsonlFile, e);
            }
        }
        vo.setLastActivity(lastActivity);
        return vo;
    }

    // -- Detail --

    @Override
    public ClaudeConversationDetailVo getConversationDetail(String projectDirName, String sessionId) {
        validateSessionId(sessionId);
        Path projectDir = getProjectsDir().resolve(projectDirName).normalize();
        validateProjectDir(projectDir);
        Path jsonlFile = projectDir.resolve(sessionId + ".jsonl");

        if (!Files.exists(jsonlFile)) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "Conversation not found");
        }

        ClaudeConversationDetailVo vo = new ClaudeConversationDetailVo();
        vo.setSessionId(sessionId);
        vo.setProjectPath(projectDirName);

        List<Map<String, Object>> messages = new ArrayList<>();
        LocalDateTime startTime = null;
        LocalDateTime lastActivity = null;

        try (java.io.BufferedReader reader = Files.newBufferedReader(jsonlFile, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> msg = objectMapper.readValue(line, Map.class);
                messages.add(msg);

                String ts = getText(msg, "timestamp");
                if (ts != null && !ts.isEmpty()) {
                    LocalDateTime ldt = parseTimestamp(ts);
                    if (ldt != null) {
                        if (startTime == null) {
                            startTime = ldt;
                        }
                        if (lastActivity == null || ldt.isAfter(lastActivity)) {
                            lastActivity = ldt;
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR.getCode(), "Failed to read conversation file");
        }

        vo.setMessages(messages);
        vo.setMessageCount(messages.size());
        vo.setStartTime(startTime);
        vo.setLastActivity(lastActivity);
        return vo;
    }

    // -- Delete --

    @Override
    public void deleteConversation(String projectDirName, String sessionId) {
        validateSessionId(sessionId);
        Path projectDir = getProjectsDir().resolve(projectDirName).normalize();
        validateProjectDir(projectDir);

        Path jsonlFile = projectDir.resolve(sessionId + ".jsonl");
        Path sessionSubDir = projectDir.resolve(sessionId);

        boolean deleted = false;
        try {
            if (Files.deleteIfExists(jsonlFile)) {
                deleted = true;
            }
            if (Files.exists(sessionSubDir) && Files.isDirectory(sessionSubDir)) {
                deleteDirectoryRecursively(sessionSubDir);
                deleted = true;
            }
        } catch (IOException e) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR.getCode(), "Failed to delete conversation");
        }
        if (!deleted) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "Conversation not found");
        }
    }

    @Override
    public void deleteProject(String projectDirName) {
        Path projectDir = getProjectsDir().resolve(projectDirName).normalize();
        validateProjectDir(projectDir);

        if (!Files.isDirectory(projectDir)) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "Project not found");
        }
        try {
            deleteDirectoryRecursively(projectDir);
        } catch (IOException e) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR.getCode(), "Failed to delete project");
        }
    }

    // -- Helpers --

    private Path getProjectsDir() {
        return Path.of(config.getBasePath(), "projects");
    }

    private void validateProjectDir(Path projectDir) {
        if (!projectDir.startsWith(getProjectsDir())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "Invalid project path");
        }
    }

    private void validateSessionId(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()
                || sessionId.contains("/") || sessionId.contains("\\")
                || sessionId.contains("..") || sessionId.contains(".")) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "Invalid session id");
        }
    }

    private String extractUserContent(JsonNode node) {
        JsonNode msgNode = node.get("message");
        if (msgNode == null || !msgNode.isObject()) {
            return null;
        }
        JsonNode contentNode = msgNode.get("content");
        if (contentNode == null) {
            return null;
        }
        if (contentNode.isTextual()) {
            return contentNode.asText();
        }
        if (contentNode.isArray()) {
            for (JsonNode block : contentNode) {
                if (block.isObject()) {
                    JsonNode textNode = block.get("text");
                    if (textNode != null && textNode.isTextual()) {
                        return textNode.asText();
                    }
                }
            }
        }
        return null;
    }

    private LocalDateTime parseTimestamp(String ts) {
        try {
            return Instant.parse(ts).atZone(ZoneId.systemDefault()).toLocalDateTime();
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDateTime toLocalDateTime(java.nio.file.attribute.FileTime fileTime) {
        return fileTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private String getText(JsonNode node, String field) {
        JsonNode child = node.get(field);
        return child != null ? child.asText() : null;
    }

    @SuppressWarnings("unchecked")
    private String getText(Map<String, Object> map, String field) {
        Object val = map.get(field);
        return val instanceof String s ? s : null;
    }

    private String truncate(String s, int maxLen) {
        if (s == null) {
            return null;
        }
        s = s.replaceAll("\\s+", " ").trim();
        return s.length() > maxLen ? s.substring(0, maxLen) + "..." : s;
    }

    private void deleteDirectoryRecursively(Path dir) throws IOException {
        Files.walkFileTree(dir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path d, IOException exc) throws IOException {
                if (exc != null) {
                    throw exc;
                }
                Files.delete(d);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
