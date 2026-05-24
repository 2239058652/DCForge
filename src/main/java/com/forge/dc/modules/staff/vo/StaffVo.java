package com.forge.dc.modules.staff.vo;

import com.forge.dc.modules.staff.entity.Staff;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StaffVo {
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
    private String avatarObjectName;
    private String avatarUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static StaffVo from(Staff staff) {
        StaffVo vo = new StaffVo();
        vo.setId(staff.getId());
        vo.setName(staff.getName());
        vo.setType(staff.getType());
        vo.setRestDay(staff.getRestDay());
        vo.setNightOrder(staff.getNightOrder());
        vo.setIsActive(staff.getIsActive());
        vo.setAvatarObjectName(staff.getAvatarObjectName());
        vo.setCreatedAt(staff.getCreatedAt());
        vo.setUpdatedAt(staff.getUpdatedAt());
        return vo;
    }
}
