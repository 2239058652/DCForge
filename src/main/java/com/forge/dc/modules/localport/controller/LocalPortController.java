package com.forge.dc.modules.localport.controller;

import com.forge.dc.common.result.Result;
import com.forge.dc.modules.localport.dto.LocalPortQueryDto;
import com.forge.dc.modules.localport.dto.TerminatePortProcessDto;
import com.forge.dc.modules.localport.service.LocalPortService;
import com.forge.dc.modules.localport.vo.LocalPortVO;
import com.forge.dc.modules.localport.vo.TerminatePortProcessVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/local-ports")
@RequiredArgsConstructor
@Tag(name = "本机端口管理", description = "Windows 本机端口查询与进程结束")
@Slf4j
public class LocalPortController {

    private final LocalPortService localPortService;

    @GetMapping
    @Operation(summary = "查询本机端口列表")
    public Result<List<LocalPortVO>> queryPorts(LocalPortQueryDto dto) {
        return Result.success(localPortService.queryPorts(dto));
    }

    @PostMapping("/terminate")
    @Operation(summary = "结束占用端口的进程")
    public Result<TerminatePortProcessVO> terminate(@RequestBody @Valid TerminatePortProcessDto dto) {
        return Result.success(localPortService.terminateProcess(dto));
    }
}
