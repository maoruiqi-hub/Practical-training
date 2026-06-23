package com.neu.CoursePlatform.module5_analytics.dto.external;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 知识点信息（来自模块1）
 */
@Data
@NoArgsConstructor
public class KnowledgePointDTO {
    private String id;
    private String name;
    private String description;
    private Integer level;
    private String courseId;
}
