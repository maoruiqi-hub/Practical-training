package com.neu.CoursePlatform.module5_analytics.service;

import com.neu.CoursePlatform.module5_analytics.dto.ClassProgressDTO;
import com.neu.CoursePlatform.module5_analytics.dto.ScoreOverviewDTO;
import com.neu.CoursePlatform.module5_analytics.dto.WeakPointDTO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 报表导出服务（T7, R7.1-R7.4）
 * Phase 2：生成结构化JSON数据；Excel/PDF文件生成延后到Phase 4完善
 */
@Service
public class ReportExportService {

    private final ScoreAnalysisService scoreAnalysisService;
    private final ProgressService progressService;

    public ReportExportService(ScoreAnalysisService scoreAnalysisService,
                                ProgressService progressService) {
        this.scoreAnalysisService = scoreAnalysisService;
        this.progressService = progressService;
    }

    /**
     * 生成成绩报表数据（R7.1）
     */
    public Map<String, Object> generateScoreReport(String classId, String courseId) {
        ScoreOverviewDTO overview = scoreAnalysisService.getClassScoreOverview(classId, courseId);
        List<WeakPointDTO> weakPoints = scoreAnalysisService.getWeakPoints(courseId);

        return Map.of(
                "title", "成绩报表",
                "classId", classId,
                "courseId", courseId,
                "generatedAt", LocalDateTime.now().toString(),
                "scoreOverview", overview,
                "weakPoints", weakPoints
        );
    }

    /**
     * 生成综合分析报告数据（R7.2）
     */
    public Map<String, Object> generateFullReport(String classId, String courseId) {
        ScoreOverviewDTO overview = scoreAnalysisService.getClassScoreOverview(classId, courseId);
        ClassProgressDTO progress = progressService.getClassProgress(classId, courseId);
        List<WeakPointDTO> weakPoints = scoreAnalysisService.getWeakPoints(courseId);

        return Map.of(
                "title", "综合分析报告",
                "classId", classId,
                "courseId", courseId,
                "generatedAt", LocalDateTime.now().toString(),
                "scoreOverview", overview,
                "progressOverview", progress,
                "weakPoints", weakPoints
        );
    }
}
