package com.forge.dc.modules.staff.service.impl;

import com.forge.dc.common.result.PageResult;
import com.forge.dc.modules.file.service.FileService;
import com.forge.dc.modules.file.vo.FileUploadVO;
import com.forge.dc.modules.staff.dto.StaffPageDto;
import com.forge.dc.modules.staff.dto.StaffRequest;
import com.forge.dc.modules.staff.entity.RotaState;
import com.forge.dc.modules.staff.entity.Staff;
import com.forge.dc.modules.staff.mapper.RotaStateMapper;
import com.forge.dc.modules.staff.mapper.StaffMapper;
import com.forge.dc.modules.staff.service.StaffService;
import com.forge.dc.modules.staff.vo.StaffVo;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StaffServiceImpl implements StaffService {

    private final StaffMapper staffMapper;
    private final RotaStateMapper rotaStateMapper;
    private final FileService fileService;

    @Override
    public List<StaffVo> listAll() {
        return staffMapper.findAll().stream()
                .map(staff -> {
                    StaffVo vo = StaffVo.from(staff);
                    if (StringUtils.hasText(staff.getAvatarObjectName())) {
                        vo.setAvatarUrl(fileService.getUrl(staff.getAvatarObjectName()));
                    }
                    return vo;
                })
                .toList();
    }

    @Override
    @Transactional
    public Staff addStaff(StaffRequest req) {
        int type = req.getType();
        Integer maxOrder = staffMapper.findMaxNightOrder(type);
        int insertOrder;

        if (req.getNightOrder() == null) {
            insertOrder = (maxOrder == null ? 0 : maxOrder) + 1;
        } else {
            insertOrder = req.getNightOrder();
            // 插入位置之后的人全部后移
            staffMapper.shiftOrderUp(type, insertOrder);
        }

        Staff staff = new Staff();
        staff.setName(req.getName());
        staff.setType(type);
        staff.setRestDay(req.getRestDay());
        staff.setNightOrder(insertOrder);
        staffMapper.insert(staff);

        // 若该type还没有rota_state，初始化为该新人
        RotaState state = rotaStateMapper.findByType(type);
        if (state == null) {
            RotaState newState = new RotaState();
            newState.setType(type);
            newState.setCurrentStaffId(staff.getId());
            rotaStateMapper.upsert(newState);
        }

        return staffMapper.findById(staff.getId());
    }

    @Override
    @Transactional
    public Staff updateStaff(Long id, StaffRequest req) {
        Staff exist = staffMapper.findById(id);
        if (exist == null) throw new RuntimeException("人员不存在");

        // 如果调整了nightOrder，需要重新排列
        if (!exist.getNightOrder().equals(req.getNightOrder()) && req.getNightOrder() != null) {
            // 先把原位置后面的人前移
            staffMapper.shiftOrderDown(exist.getType(), exist.getNightOrder());
            // 再把新位置后面的人后移
            staffMapper.shiftOrderUp(exist.getType(), req.getNightOrder());
            exist.setNightOrder(req.getNightOrder());
        }

        exist.setName(req.getName());
        exist.setRestDay(req.getRestDay());
        staffMapper.update(exist);
        return staffMapper.findById(id);
    }

    @Override
    @Transactional
    public void deactivateStaff(Long id) {
        Staff staff = staffMapper.findById(id);
        if (staff == null) throw new RuntimeException("人员不存在");
        staffMapper.updateActive(id, false);
        // 同type中该人之后的order前移，保持队列连续
        staffMapper.shiftOrderDown(staff.getType(), staff.getNightOrder());

        // 若夜班指针正指向该人，移动到下一个active人员
        RotaState state = rotaStateMapper.findByType(staff.getType());
        if (state != null && state.getCurrentStaffId().equals(id)) {
            List<Staff> activeList = staffMapper.findActiveByType(staff.getType());
            if (!activeList.isEmpty()) {
                // 找到原order的下一个
                Staff next = activeList.stream()
                        .filter(s -> s.getNightOrder() > staff.getNightOrder())
                        .findFirst()
                        .orElse(activeList.get(0));
                state.setCurrentStaffId(next.getId());
                rotaStateMapper.upsert(state);
            }
        }
    }

    @Override
    public void activateStaff(Long id) {
        Staff staff = staffMapper.findById(id);
        if (staff == null) throw new RuntimeException("人员不存在");
        // 重新激活：加到末尾
        Integer maxOrder = staffMapper.findMaxNightOrder(staff.getType());
        staff.setNightOrder((maxOrder == null ? 0 : maxOrder) + 1);
        staffMapper.updateActive(id, true);
        staffMapper.update(staff);
    }

    @Override
    public PageResult<Staff> findStaffByPage(StaffPageDto dto) {
        // 启动分页，必须紧挨着查询方法
        PageHelper.startPage(dto.getPageNum(), dto.getPageSize());

        List<Staff> list = staffMapper.findByCondition(dto.getName(), dto.getType());
        PageInfo<Staff> pageInfo = new PageInfo<>(list);

        return new PageResult<>(
                pageInfo.getTotal(),
                pageInfo.getList(),
                pageInfo.getPageNum(),
                pageInfo.getPageSize()
        );
    }

    @Override
    public FileUploadVO updateAvatar(Long id, MultipartFile file) {
        Staff staff = staffMapper.findById(id);
        if (staff == null) throw new RuntimeException("人员不存在");

        // 删旧文件
        if (StringUtils.hasText(staff.getAvatarObjectName())) {
            fileService.delete(staff.getAvatarObjectName());
        }
        // 上传新文件
        FileUploadVO result = fileService.upload(file, "avatar");
        staff.setAvatarObjectName(result.getObjectName());

        staffMapper.update(staff);
        // 构建返回给前端的 VO（包含临时访问URL）
        String presignedUrl = fileService.getUrl(result.getObjectName());
        return FileUploadVO.builder()
                .objectName(result.getObjectName())   // 可选，前端一般不关心
                .url(presignedUrl)                    // 前端直接拿来展示
                .build();
    }

    public List<StaffVo> listStaff() {
        return staffMapper.findAll().stream()
                .map(staff -> {
                    StaffVo vo = new StaffVo();
                    vo.setId(staff.getId());
                    vo.setName(staff.getName());
                    if (StringUtils.hasText(staff.getAvatarObjectName())) {
                        vo.setAvatarUrl(fileService.getUrl(staff.getAvatarObjectName()));
                    }
                    return vo;
                })
                .toList();
    }
}