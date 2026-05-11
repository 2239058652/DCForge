package com.forge.dc.staff.mapper;

import com.forge.dc.staff.dto.ScheduleDetailDO;
import com.forge.dc.staff.entity.Schedule;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ScheduleMapper {

    @Insert("<script>" +
            "INSERT INTO schedule(staff_id, shift_date, shift_type, is_swapped) VALUES " +
            "<foreach collection='list' item='s' separator=','>" +
            "(#{s.staffId}, #{s.shiftDate}, #{s.shiftType}, #{s.isSwapped})" +
            "</foreach>" +
            "</script>")
    void batchInsert(List<Schedule> list);

    @Select("SELECT s.*, st.name as staff_name, st.type as staff_type " +
            "FROM schedule s JOIN staff st ON s.staff_id = st.id " +
            "WHERE YEAR(s.shift_date)=#{year} AND MONTH(s.shift_date)=#{month} " +
            "ORDER BY s.shift_date, st.type, st.night_order")
    @Results({
            @Result(column = "id", property = "id"),
            @Result(column = "staff_id", property = "staffId"),
            @Result(column = "shift_date", property = "shiftDate"),
            @Result(column = "shift_type", property = "shiftType"),
            @Result(column = "is_swapped", property = "isSwapped")
    })
    List<ScheduleDetailDO> findByYearMonth(@Param("year") int year, @Param("month") int month);

    @Select("SELECT * FROM schedule WHERE staff_id=#{staffId} " +
            "AND YEAR(shift_date)=#{year} AND MONTH(shift_date)=#{month} " +
            "ORDER BY shift_date")
    List<Schedule> findByStaffAndMonth(@Param("staffId") Long staffId,
                                       @Param("year") int year,
                                       @Param("month") int month);

    @Select("SELECT COUNT(*) FROM schedule WHERE YEAR(shift_date)=#{year} AND MONTH(shift_date)=#{month}")
    int countByYearMonth(@Param("year") int year, @Param("month") int month);

    @Delete("DELETE FROM schedule WHERE YEAR(shift_date)=#{year} AND MONTH(shift_date)=#{month}")
    void deleteByYearMonth(@Param("year") int year, @Param("month") int month);
}