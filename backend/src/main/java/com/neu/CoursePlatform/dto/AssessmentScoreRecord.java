package com.neu.CoursePlatform.dto;

import java.time.LocalDateTime;

/** 模块三对外提供的成绩数据契约，供模块五适配，不依赖其 DTO。 */
public record AssessmentScoreRecord(String studentId, String studentName, String courseId,
                                    String targetId, String targetType, double score,
                                    double totalScore, LocalDateTime scoredAt) { }
