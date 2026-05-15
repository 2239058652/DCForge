package com.forge.dc.modules.staff.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Staff {
    private Long id;
    private String name;
    /**
     * 0=doctor 1=nurse
     */
    private Integer type;
    /**
     * 0=周日 1=周一 ... 6=周六
     */
    private Integer restDay;
    private Integer nightOrder;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}