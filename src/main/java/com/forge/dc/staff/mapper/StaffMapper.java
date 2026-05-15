package com.forge.dc.staff.mapper;

import com.forge.dc.staff.entity.Staff;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface StaffMapper {

    @Select("SELECT * FROM staff ORDER BY type, night_order")
    List<Staff> findAll();

    @Select("SELECT * FROM staff WHERE type = #{type} AND is_active = 1 ORDER BY night_order")
    List<Staff> findActiveByType(int type);

    @Select("SELECT * FROM staff WHERE id = #{id}")
    Staff findById(Long id);

    @Select("SELECT MAX(night_order) FROM staff WHERE type = #{type}")
    Integer findMaxNightOrder(int type);

    @Select("SELECT COUNT(*) FROM staff WHERE type = #{type} AND night_order = #{order} AND id != #{excludeId}")
    int countByTypeAndOrder(@Param("type") int type,
                            @Param("order") int order,
                            @Param("excludeId") long excludeId);

    @Insert("INSERT INTO staff(name,type,rest_day,night_order,is_active) " +
            "VALUES(#{name},#{type},#{restDay},#{nightOrder},1)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Staff staff);

    @Update("UPDATE staff SET name=#{name}, rest_day=#{restDay}, " +
            "night_order=#{nightOrder}, updated_at=NOW() WHERE id=#{id}")
    void update(Staff staff);

    @Update("UPDATE staff SET is_active=#{isActive}, updated_at=NOW() WHERE id=#{id}")
    void updateActive(@Param("id") Long id, @Param("isActive") boolean isActive);

    /**
     * 停用时，把同type中order>被停用者的全部-1，保持连续
     */
    @Update("UPDATE staff SET night_order = night_order - 1, updated_at=NOW() " +
            "WHERE type=#{type} AND night_order > #{order} AND is_active=1")
    void shiftOrderDown(@Param("type") int type, @Param("order") int order);

    /**
     * 新增时，给插入位置之后的人+1
     */
    @Update("UPDATE staff SET night_order = night_order + 1, updated_at=NOW() " +
            "WHERE type=#{type} AND night_order >= #{order}")
    void shiftOrderUp(@Param("type") int type, @Param("order") int order);

    List<Staff> findByCondition(@Param("name") String name, @Param("type") Integer type);
}