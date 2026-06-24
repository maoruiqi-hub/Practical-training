package com.neu.CoursePlatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neu.CoursePlatform.entity.KnowledgeRelation;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface KnowledgeRelationMapper extends BaseMapper<KnowledgeRelation> {

    List<KnowledgeRelation> selectByCourse(@Param("courseCode") String courseCode);
}
