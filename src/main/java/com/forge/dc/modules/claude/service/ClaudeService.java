package com.forge.dc.modules.claude.service;

import com.forge.dc.common.result.PageResult;
import com.forge.dc.modules.claude.dto.ClaudeProjectPageDto;
import com.forge.dc.modules.claude.vo.ClaudeConversationDetailVo;
import com.forge.dc.modules.claude.vo.ClaudeConversationListVo;
import com.forge.dc.modules.claude.vo.ClaudeProjectVo;

import java.util.List;

public interface ClaudeService {

    PageResult<ClaudeProjectVo> listProjects(ClaudeProjectPageDto dto);

    List<ClaudeConversationListVo> listConversations(String projectDirName);

    ClaudeConversationDetailVo getConversationDetail(String projectDirName, String sessionId);

    void deleteConversation(String projectDirName, String sessionId);

    void deleteProject(String projectDirName);
}
