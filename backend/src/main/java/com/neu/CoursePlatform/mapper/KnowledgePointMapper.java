package com.neu.CoursePlatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neu.CoursePlatform.entity.KnowledgePoint;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface KnowledgePointMapper extends BaseMapper<KnowledgePoint> {

    List<KnowledgePoint> selectByCourse(@Param("courseCode") String courseCode,
                                        @Param("lessonNo") String lessonNo,
                                        @Param("keyword") String keyword);
}
