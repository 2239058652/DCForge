package com.forge.dc.modules.dictionary.controller;

import com.forge.dc.common.result.Result;
import com.forge.dc.modules.dictionary.service.DictionaryService;
import com.forge.dc.modules.dictionary.vo.DictOptionVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/dictionary")
@Tag(name = "字典管理")
public class DictionaryController {
    private final DictionaryService dictionaryService;

    public DictionaryController(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    @GetMapping("/permisson-code/list")
    @Operation(summary = "查询权限码字典接口")
    public Result<List<DictOptionVo>> list() {
        return Result.success(dictionaryService.listPermissionCodeDictOptions());
    }
}
