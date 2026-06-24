package com.neu.CoursePlatform.module5_analytics.controller;

import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.module5_analytics.service.ReportExportService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 报表导出 Controller（R7 组需求, T7）
 */
@RestController
@RequestMapping("/api/classes/{classId}/reports")
public class ReportController {

    private final ReportExportService reportExportService;
    private final Auth auth;

    public ReportController(ReportExportService reportExportService, Auth auth) {
        this.reportExportService = reportExportService;
        this.auth = auth;
    }

    /** R7.1/R7.2 导出报表 */
    @PostMapping("/export")
    public Result<Map<String, Object>> exportReport(@PathVariable String classId,
                                                     @RequestBody Map<String, String> body,
                                                     HttpSession session) {
        if (auth.getTeacher(session) == null) return Result.fail("请先登录");
        String format = body.getOrDefault("format", "excel");
        String reportType = body.getOrDefault("report_type", "scores");
        String courseId = body.get("course_id");

        if (courseId == null) return Result.fail("course_id 不能为空");

        Map<String, Object> data;
        if ("full_analysis".equals(reportType)) {
            data = reportExportService.generateFullReport(classId, courseId);
        } else {
            data = reportExportService.generateScoreReport(classId, courseId);
        }
        data.put("format", format);
        return Result.ok(data);
    }
}
