package com.neu.CoursePlatform.profile.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neu.CoursePlatform.profile.entity.ProfileProjectionLedger;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

public interface ProfileProjectionLedgerMapper extends BaseMapper<ProfileProjectionLedger> {
    @Insert("""
            INSERT INTO profile_projection_ledger (
                id, student_no, course_code, source_type, source_id, projection_type, applied_at
            ) SELECT
                #{item.id}, #{item.studentNo}, #{item.courseCode}, #{item.sourceType},
                #{item.sourceId}, #{item.projectionType}, #{item.appliedAt}
            WHERE NOT EXISTS (
                SELECT 1 FROM profile_projection_ledger
                WHERE source_type = #{item.sourceType}
                  AND source_id = #{item.sourceId}
                  AND projection_type = #{item.projectionType}
            )
            """)
    int insertIfAbsent(@Param("item") ProfileProjectionLedger item);
}
