package com.neu.CoursePlatform.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neu.CoursePlatform.entity.AbilityKnowledgePoint;
import org.apache.ibatis.annotations.Insert;

public interface AbilityKnowledgePointMapper extends BaseMapper<AbilityKnowledgePoint> {
    @Insert("""
            INSERT INTO ability_knowledge_point (id, ability_point_id, knowledge_point_id)
            VALUES (#{id}, #{abilityPointId}, #{knowledgePointId})
            """)
    int insertWithId(AbilityKnowledgePoint mapping);
}
