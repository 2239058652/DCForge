package com.forge.dc.modules.dictionary.controller;

import com.forge.dc.common.result.PageResult;
import com.forge.dc.common.result.Result;
import com.forge.dc.modules.dictionary.dto.DictPageDto;
import com.forge.dc.modules.dictionary.dto.DictSaveDto;
import com.forge.dc.modules.dictionary.service.DictionaryService;
import com.forge.dc.modules.dictionary.vo.DictListVo;
import com.forge.dc.modules.dictionary.vo.DictOptionVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/dictionary")
@Tag(name = "字典管理", description = "字典管理的相关接口")
public class DictionaryController {

    private final DictionaryService dictionaryService;

    public DictionaryController(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    @GetMapping("/permission-code/list")
    @Operation(summary = "查询权限码字典接口")
    public Result<List<DictOptionVo>> list() {
        return Result.success(dictionaryService.listPermissionCodeDictOptions());
    }

    @GetMapping("/{dictCode}")
    @Operation(summary = "根据字典编码查询字典列表")
    public Result<List<DictOptionVo>> listByDictCode(@PathVariable String dictCode) {
        return Result.success(dictionaryService.listByDictCode(dictCode));
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询字典列表")
    public Result<PageResult<DictListVo>> page(@Valid DictPageDto dto) {
        return Result.success(dictionaryService.page(dto));
    }

    @PostMapping
    @Operation(summary = "新增或修改字典项")
    public Result<Void> save(@RequestBody @Valid DictSaveDto dto) {
        dictionaryService.save(dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除字典项")
    public Result<Void> removeById(@PathVariable Long id) {
        dictionaryService.removeById(id);
        return Result.success();
    }
}
