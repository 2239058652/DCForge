package com.forge.dc.modules.dictionary.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DictOptionVo {
    private String label;   // 显示文字（如权限名称）
    private String value;   // 实际值（如权限码）
}