package com.neu.CoursePlatform.module5_analytics.controller;

import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.module5_analytics.entity.RiskAlert;
import com.neu.CoursePlatform.module5_analytics.service.RiskAlertService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 风险预警 Controller（R4.8-R4.9, R8 组需求）
 */
@RestController
public class RiskAlertController {

    private final RiskAlertService riskAlertService;
    private final Auth auth;

    public RiskAlertController(RiskAlertService riskAlertService, Auth auth) {
        this.riskAlertService = riskAlertService;
        this.auth = auth;
    }

    /**
     * R4.9 / R8.1 接收模块4游戏风险事件
     * POST /api/risk-alerts
     * body: { student_id, course_id, risk_type, detail_json }
     */
    @PostMapping("/api/risk-alerts")
    public Result<RiskAlert> receiveEvent(@RequestBody Map<String, Object> body) {
        String studentId = (String) body.get("student_id");
        String courseId = (String) body.get("course_id");
        String riskType = (String) body.get("risk_type");
        String detail = body.get("detail_json") != null ? body.get("detail_json").toString() : "{}";

        if (studentId == null || riskType == null) {
            return Result.fail("student_id 和 risk_type 不能为空");
        }

        // 根据 risk_type 映射风险等级
        String riskLevel = mapRiskLevel(riskType);

        RiskAlert alert = riskAlertService.receiveEvent(
                studentId, courseId, riskType, riskLevel, detail);
        if (alert == null) {
            return Result.fail("预警已存在或处于冷却期，未重复创建");
        }
        return Result.ok(alert);
    }

    /**
     * R8.5 查询学生当前风险状态（供模块4调用）
     * GET /api/students/{id}/risk-status
     */
    @GetMapping("/api/students/{id}/risk-status")
    public Result<RiskAlertService.RiskStatus> getStudentRiskStatus(@PathVariable String id) {
        return Result.ok(riskAlertService.getStudentRiskStatus(id));
    }

    /**
     * 查询班级活跃预警列表（教师端）
     * GET /api/classes/{id}/risk-alerts
     */
    @GetMapping("/api/classes/{id}/risk-alerts")
    public Result<List<RiskAlert>> getClassRiskAlerts(
            @PathVariable String id,
            @RequestParam(required = false, defaultValue = "active") String status,
            HttpSession session) {
        if (auth.getTeacher(session) == null) return Result.fail("请先登录");
        // Phase 1: studentIds 从 classInfoService 获取，此处先返回空列表
        // Phase 2: 完整实现时注入 ClassInfoService 获取班级学生ID列表
        return Result.ok(List.of());
    }

    /**
     * 标记预警为已处理（教师端）
     * PUT /api/risk-alerts/{id}/resolve
     */
    @PutMapping("/api/risk-alerts/{id}/resolve")
    public Result<Void> resolve(@PathVariable String id, HttpSession session) {
        if (auth.getTeacher(session) == null) return Result.fail("请先登录");
        String teacherNo = String.valueOf(auth.getTeacher(session).getTeacherNo());
        boolean ok = riskAlertService.resolve(id, teacherNo);
        return ok ? Result.ok() : Result.fail("预警不存在或已处理");
    }

    /** 风险类型 → 风险等级映射 */
    private String mapRiskLevel(String riskType) {
        return switch (riskType) {
            case "hp_critical", "low_score" -> "high";
            case "procrastination", "stuck_detected", "inactive", "stuck" -> "medium";
            default -> "low";
        };
    }
}
