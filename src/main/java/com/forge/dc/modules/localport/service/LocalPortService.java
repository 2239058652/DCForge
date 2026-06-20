package com.forge.dc.modules.localport.service;

import com.forge.dc.modules.localport.dto.LocalPortQueryDto;
import com.forge.dc.modules.localport.dto.TerminatePortProcessDto;
import com.forge.dc.modules.localport.vo.LocalPortVO;
import com.forge.dc.modules.localport.vo.TerminatePortProcessVO;

import java.util.List;

public interface LocalPortService {

    /**
     * 查询本机端口列表，支持协议、状态、端口、关键字过滤。
     */
    List<LocalPortVO> queryPorts(LocalPortQueryDto dto);

    /**
     * 结束占用指定端口的进程。
     * 执行前会重新校验 PID 是否仍占用端口，并通过安全策略检查。
     */
    TerminatePortProcessVO terminateProcess(TerminatePortProcessDto dto);
}
