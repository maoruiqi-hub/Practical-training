package com.neu.CoursePlatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 知识点关系实体
 */
@Data
@NoArgsConstructor
@TableName("knowledge_relation")
public class KnowledgeRelation {
    @TableId(type = IdType.AUTO)
    private String relationId;
    private String courseCode;
    private String fromKnowledgePointId;
    private String toKnowledgePointId;
    private String relationType;
}
