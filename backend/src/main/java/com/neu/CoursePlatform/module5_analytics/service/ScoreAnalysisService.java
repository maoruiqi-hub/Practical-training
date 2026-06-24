package com.neu.CoursePlatform.module5_analytics.service;

import com.neu.CoursePlatform.module5_analytics.dto.ScoreOverviewDTO;
import com.neu.CoursePlatform.module5_analytics.dto.ScoreTrendDTO;
import com.neu.CoursePlatform.module5_analytics.dto.WeakPointDTO;

import java.util.List;

/**
 * 成绩分析服务接口（T3 + T4）
 */
public interface ScoreAnalysisService {

    /** 班级成绩总览（R2.1） */
    ScoreOverviewDTO getClassScoreOverview(String classId, String courseId);

    /** 薄弱知识点排名（R2.4） */
    List<WeakPointDTO> getWeakPoints(String courseId);

    /** 成绩趋势（R2.2） */
    ScoreTrendDTO getScoreTrends(String classId, String courseId, String granularity);

    /** 学生个人成绩趋势（R2.3） */
    ScoreTrendDTO getStudentScoreTrends(String studentId, String courseId, String granularity);
}
