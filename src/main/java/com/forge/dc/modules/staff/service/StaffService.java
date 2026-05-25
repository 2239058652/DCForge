package com.forge.dc.modules.staff.service;

import com.forge.dc.common.result.PageResult;
import com.forge.dc.modules.file.vo.FileUploadVO;
import com.forge.dc.modules.staff.dto.StaffPageDto;
import com.forge.dc.modules.staff.dto.StaffRequest;
import com.forge.dc.modules.staff.entity.Staff;
import com.forge.dc.modules.staff.vo.StaffVo;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface StaffService {
    List<StaffVo> listAll();

    Staff addStaff(StaffRequest req);

    Staff updateStaff(Long id, StaffRequest req);

    void deactivateStaff(Long id);

    void activateStaff(Long id);

    PageResult<StaffVo> findStaffByPage(StaffPageDto dto);

    FileUploadVO updateAvatar(Long id, MultipartFile file);
}