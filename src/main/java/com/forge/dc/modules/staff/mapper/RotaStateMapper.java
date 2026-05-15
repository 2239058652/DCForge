package com.forge.dc.modules.staff.mapper;

import com.forge.dc.modules.staff.entity.RotaState;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RotaStateMapper {

    @Select("SELECT * FROM rota_state WHERE type = #{type}")
    RotaState findByType(int type);

    @Insert("INSERT INTO rota_state(type, current_staff_id) VALUES(#{type}, #{currentStaffId}) " +
            "ON DUPLICATE KEY UPDATE current_staff_id=#{currentStaffId}, updated_at=NOW()")
    void upsert(RotaState rotaState);
}