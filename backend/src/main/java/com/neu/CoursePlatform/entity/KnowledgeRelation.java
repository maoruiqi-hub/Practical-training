package com.neu.CoursePlatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@TableName("knowledge_relation")
public class KnowledgeRelation {

    @TableId(value = "relation_id", type = IdType.AUTO)
    private String relationId;

    private String courseCode;
    private String fromKnowledgePointId;
    private String toKnowledgePointId;
    private String relationType;
}
