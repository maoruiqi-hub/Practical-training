package com.neu.CoursePlatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neu.CoursePlatform.entity.StudentTowerRun;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface StudentTowerRunMapper extends BaseMapper<StudentTowerRun> {
    @Select("SELECT * FROM student_tower_run WHERE run_id = #{runId} FOR UPDATE")
    StudentTowerRun selectForUpdate(@Param("runId") String runId);
}
