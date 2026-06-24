package com.neu.CoursePlatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neu.CoursePlatform.entity.KnowledgeEdge;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface KnowledgeEdgeMapper extends BaseMapper<KnowledgeEdge> {

    List<KnowledgeEdge> selectByCourse(@Param("courseCode") String courseCode);
}
