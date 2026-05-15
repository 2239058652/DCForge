package com.forge.dc.staff.service;

import com.forge.dc.common.result.PageResult;
import com.forge.dc.staff.dto.StaffPageDto;
import com.forge.dc.staff.dto.StaffRequest;
import com.forge.dc.staff.entity.Staff;

import java.util.List;

public interface StaffService {
    List<Staff> listAll();

    Staff addStaff(StaffRequest req);

    Staff updateStaff(Long id, StaffRequest req);

    void deactivateStaff(Long id);

    void activateStaff(Long id);

    PageResult<Staff> findStaffByPage(StaffPageDto dto);
}