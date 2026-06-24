package com.neu.CoursePlatform.dto;
import lombok.Data;
@Data public class KnowledgeMasteryUpdateRequest { private String studentNo; private String courseCode; private String knowledgePointId; private Integer masteryScore; private String sourceType; private String sourceId; }
