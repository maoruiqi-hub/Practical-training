package com.neu.CoursePlatform.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
@Data @AllArgsConstructor public class WeakKnowledgePointDTO { private String knowledgePointId; private String knowledgePointName; private double averageMasteryScore; private long studentCount; private String statisticBasis; }
