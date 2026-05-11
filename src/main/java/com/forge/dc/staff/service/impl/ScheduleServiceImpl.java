package com.forge.dc.staff.service.impl;

import com.forge.dc.staff.dto.ScheduleDetailDO;
import com.forge.dc.staff.entity.*;
import com.forge.dc.staff.mapper.RotaStateMapper;
import com.forge.dc.staff.mapper.ScheduleMapper;
import com.forge.dc.staff.mapper.StaffMapper;
import com.forge.dc.staff.service.ScheduleService;
import com.forge.dc.staff.vo.DailyScheduleVO;
import com.forge.dc.staff.vo.ShiftVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleServiceImpl implements ScheduleService {

    private final StaffMapper staffMapper;
    private final ScheduleMapper scheduleMapper;
    private final RotaStateMapper rotaStateMapper;

    @Override
    @Transactional
    public void generateSchedule(int year, int month, boolean forceOverwrite) {
        int existing = scheduleMapper.countByYearMonth(year, month);
        if (existing > 0) {
            if (!forceOverwrite) throw new RuntimeException("该月排班已存在，请使用 forceOverwrite=true 覆盖");
            scheduleMapper.deleteByYearMonth(year, month);
        }

        List<Staff> doctors = staffMapper.findActiveByType(StaffType.DOCTOR.value);
        List<Staff> nurses = staffMapper.findActiveByType(StaffType.NURSE.value);
        if (doctors.isEmpty() || nurses.isEmpty()) {
            throw new RuntimeException("医生或护士人数为0，无法排班");
        }

        // 读取夜班指针
        RotaState doctorState = rotaStateMapper.findByType(StaffType.DOCTOR.value);
        RotaState nurseState = rotaStateMapper.findByType(StaffType.NURSE.value);
        if (doctorState == null || nurseState == null) {
            throw new RuntimeException("夜班队列状态未初始化，请先添加人员");
        }

        // 用 index 表示当前指针在队列中的位置
        int doctorPtr = findIndexById(doctors, doctorState.getCurrentStaffId());
        int nursePtr = findIndexById(nurses, nurseState.getCurrentStaffId());

        YearMonth ym = YearMonth.of(year, month);
        List<Schedule> toInsert = new ArrayList<>();

        for (int day = 1; day <= ym.lengthOfMonth(); day++) {
            LocalDate date = LocalDate.of(year, month, day);
            int weekday = date.getDayOfWeek().getValue() % 7; // 周日=0，周一=1...周六=6

            // 找出今天休息的人
            Set<Long> restIds = new HashSet<>();
            collectRestIds(doctors, weekday, restIds, toInsert, date);
            collectRestIds(nurses, weekday, restIds, toInsert, date);

            // 医生夜班
            int[] doctorResult = findNightStaff(doctors, doctorPtr, restIds);
            int nightDoctorIdx = doctorResult[0];
            boolean doctorSwapped = doctorResult[1] == 1;
            Staff nightDoctor = doctors.get(nightDoctorIdx);
            addShift(toInsert, nightDoctor.getId(), date, ShiftType.NIGHT.value, doctorSwapped);
            // 指针移到下一个
            doctorPtr = (nightDoctorIdx + 1) % doctors.size();

            // 护士夜班
            int[] nurseResult = findNightStaff(nurses, nursePtr, restIds);
            int nightNurseIdx = nurseResult[0];
            boolean nurseSwapped = nurseResult[1] == 1;
            Staff nightNurse = nurses.get(nightNurseIdx);
            addShift(toInsert, nightNurse.getId(), date, ShiftType.NIGHT.value, nurseSwapped);
            nursePtr = (nightNurseIdx + 1) % nurses.size();

            // 剩余 active 人员 = day
            Set<Long> assignedToday = new HashSet<>(restIds);
            assignedToday.add(nightDoctor.getId());
            assignedToday.add(nightNurse.getId());

            for (Staff s : doctors) {
                if (!assignedToday.contains(s.getId())) {
                    addShift(toInsert, s.getId(), date, ShiftType.DAY.value, false);
                }
            }
            for (Staff s : nurses) {
                if (!assignedToday.contains(s.getId())) {
                    addShift(toInsert, s.getId(), date, ShiftType.DAY.value, false);
                }
            }
        }

        scheduleMapper.batchInsert(toInsert);

        // 持久化最新指针
        doctorState.setCurrentStaffId(doctors.get(doctorPtr).getId());
        nurseState.setCurrentStaffId(nurses.get(nursePtr).getId());
        rotaStateMapper.upsert(doctorState);
        rotaStateMapper.upsert(nurseState);
    }

    /**
     * 找夜班：从ptr开始找第一个不在restIds里的人，返回[index, isSwapped]
     */
    private int[] findNightStaff(List<Staff> list, int ptr, Set<Long> restIds) {
        int size = list.size();
        for (int i = 0; i < size; i++) {
            int idx = (ptr + i) % size;
            if (!restIds.contains(list.get(idx).getId())) {
                return new int[]{idx, i > 0 ? 1 : 0};
            }
        }
        throw new RuntimeException("所有人今天均在休息，无法安排夜班");
    }

    private int findIndexById(List<Staff> list, Long id) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId().equals(id)) return i;
        }
        // 指针指向的人已停用，从0开始
        return 0;
    }

    private void collectRestIds(List<Staff> list, int weekday,
                                Set<Long> restIds, List<Schedule> toInsert, LocalDate date) {
        for (Staff s : list) {
            if (s.getRestDay().equals(weekday)) {
                restIds.add(s.getId());
                addShift(toInsert, s.getId(), date, ShiftType.REST.value, false);
            }
        }
    }

    private void addShift(List<Schedule> list, Long staffId, LocalDate date, int type, boolean swapped) {
        Schedule s = new Schedule();
        s.setStaffId(staffId);
        s.setShiftDate(date);
        s.setShiftType(type);
        s.setIsSwapped(swapped);
        list.add(s);
    }

    @Override
    public List<DailyScheduleVO> getMonthlySchedule(int year, int month) {
        List<ScheduleDetailDO> raw = scheduleMapper.findByYearMonth(year, month);

        Map<LocalDate, List<ShiftVO>> byDate = new LinkedHashMap<>();
        for (ScheduleDetailDO d : raw) {
            byDate.computeIfAbsent(d.getShiftDate(), k -> new ArrayList<>())
                    .add(new ShiftVO(d.getStaffId(), d.getStaffName(),
                            d.getStaffType(), d.getShiftType(), d.getIsSwapped()));
        }

        return byDate.entrySet().stream()
                .map(e -> {
                    DailyScheduleVO vo = new DailyScheduleVO();
                    vo.setDate(e.getKey());
                    vo.setShifts(e.getValue());
                    return vo;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Schedule> getStaffSchedule(Long staffId, int year, int month) {
        return scheduleMapper.findByStaffAndMonth(staffId, year, month);
    }

    @Override
    @Transactional
    public void deleteSchedule(int year, int month) {
        scheduleMapper.deleteByYearMonth(year, month);
    }
}