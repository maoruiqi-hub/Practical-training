package com.neu.CoursePlatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 知识点关系实体
 */
@Data
@NoArgsConstructor
public class KnowledgeEdge {
    @TableId(type = IdType.AUTO)
    private String edgeId;
    private String courseCode;
    private String sourceId;
    private String targetId;
    private String relationType;
}
