package com.forge.dc.modules.staff.service.impl;

import com.forge.dc.modules.staff.dto.ScheduleDetailDO;
import com.forge.dc.modules.staff.entity.*;
import com.forge.dc.modules.staff.mapper.RotaStateMapper;
import com.forge.dc.modules.staff.mapper.ScheduleMapper;
import com.forge.dc.modules.staff.mapper.StaffMapper;
import com.forge.dc.modules.staff.service.ScheduleService;
import com.forge.dc.modules.staff.vo.DailyScheduleVO;
import com.forge.dc.modules.staff.vo.ShiftVO;
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

        // 动态加载所有类型，新增类型无需再改这里
        int[] types = {StaffType.DOCTOR.value, StaffType.NURSE.value, StaffType.RECEPTIONIST.value};

        Map<Integer, List<Staff>> staffByType = new HashMap<>();
        Map<Integer, Integer> ptrByType = new HashMap<>();
        Map<Integer, RotaState> stateByType = new HashMap<>();

        for (int type : types) {
            List<Staff> list = staffMapper.findActiveByType(type);
            if (list.isEmpty()) throw new RuntimeException("类型[" + type + "]人数为0，无法排班");

            RotaState state = rotaStateMapper.findByType(type);
            if (state == null) throw new RuntimeException("类型[" + type + "]夜班队列未初始化，请先添加人员");

            staffByType.put(type, list);
            stateByType.put(type, state);
            ptrByType.put(type, findIndexById(list, state.getCurrentStaffId()));
        }

        YearMonth ym = YearMonth.of(year, month);
        List<Schedule> toInsert = new ArrayList<>();

        for (int day = 1; day <= ym.lengthOfMonth(); day++) {
            LocalDate date = LocalDate.of(year, month, day);
            int weekday = date.getDayOfWeek().getValue() % 7;

            Set<Long> restIds = new HashSet<>();

            // 所有类型统一处理 rest
            for (int type : types) {
                collectRestIds(staffByType.get(type), weekday, restIds, toInsert, date);
            }

            // 所有类型统一处理 night
            Set<Long> nightIds = new HashSet<>();
            for (int type : types) {
                List<Staff> list = staffByType.get(type);
                int ptr = ptrByType.get(type);
                int[] result = findNightStaff(list, ptr, restIds);
                Staff nightStaff = list.get(result[0]);
                addShift(toInsert, nightStaff.getId(), date, ShiftType.NIGHT.value, result[1] == 1);
                nightIds.add(nightStaff.getId());
                ptrByType.put(type, (result[0] + 1) % list.size());
            }

            // 剩余全部 day
            Set<Long> assignedToday = new HashSet<>(restIds);
            assignedToday.addAll(nightIds);
            for (int type : types) {
                for (Staff s : staffByType.get(type)) {
                    if (!assignedToday.contains(s.getId())) {
                        addShift(toInsert, s.getId(), date, ShiftType.DAY.value, false);
                    }
                }
            }
        }

        scheduleMapper.batchInsert(toInsert);

        // 持久化所有类型指针
        for (int type : types) {
            List<Staff> list = staffByType.get(type);
            RotaState state = stateByType.get(type);
            state.setCurrentStaffId(list.get(ptrByType.get(type)).getId());
            rotaStateMapper.upsert(state);
        }
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