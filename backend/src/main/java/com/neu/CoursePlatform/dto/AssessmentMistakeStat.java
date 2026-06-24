package com.neu.CoursePlatform.dto;

/** 模块三对外提供的班级错题统计契约。 */
public record AssessmentMistakeStat(String knowledgePointId, String knowledgePointName,
                                    int totalAttempts, int mistakeCount, double mistakeRate) { }
