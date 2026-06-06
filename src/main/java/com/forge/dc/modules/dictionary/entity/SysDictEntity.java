package com.forge.dc.modules.dictionary.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SysDictEntity {
    private Long id;
    private String dictCode;
    private String dictLabel;
    private String dictValue;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createdAt;
}
