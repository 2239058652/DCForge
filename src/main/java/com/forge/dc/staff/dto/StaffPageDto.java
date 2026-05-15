package com.forge.dc.staff.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class StaffPageDto {
    @Min(value = 1, message = "页码最小为1")
    private int pageNum = 1;   // 默认第1页

    @Min(1)
    @Max(value = 100, message = "每页最多100条")
    private int pageSize = 10; // 默认每页10条

    private String name;       // 可选：按姓名模糊搜索
    private Integer type;      // 可选：按类型精确筛选
}