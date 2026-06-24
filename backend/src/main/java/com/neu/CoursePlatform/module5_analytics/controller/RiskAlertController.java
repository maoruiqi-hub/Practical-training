package com.neu.CoursePlatform.module5_analytics.controller;

import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.module5_analytics.entity.RiskAlert;
import com.neu.CoursePlatform.module5_analytics.service.ClassInfoService;
import com.neu.CoursePlatform.module5_analytics.service.RiskAlertService;
import com.neu.CoursePlatform.module5_analytics.service.RiskDetectionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 风险预警 Controller（R4 组 + R8 组需求）
 */
@RestController
public class RiskAlertController {

    private final RiskAlertService riskAlertService;
    private final RiskDetectionService riskDetectionService;
    private final ClassInfoService classInfoService;
    private final Auth auth;

    public RiskAlertController(RiskAlertService riskAlertService,
                                RiskDetectionService riskDetectionService,
                                ClassInfoService classInfoService,
                                Auth auth) {
        this.riskAlertService = riskAlertService;
        this.riskDetectionService = riskDetectionService;
        this.classInfoService = classInfoService;
        this.auth = auth;
    }

    /** R4.9 / R8.1 接收模块4游戏风险事件 */
    @PostMapping("/api/risk-alerts")
    public Result<RiskAlert> receiveEvent(@RequestBody Map<String, Object> body) {
        String studentId = (String) body.get("student_id");
        String courseId = (String) body.get("course_id");
        String riskType = (String) body.get("risk_type");
        String detail = body.get("detail_json") != null ? body.get("detail_json").toString() : "{}";

        if (studentId == null || riskType == null) {
            return Result.fail("student_id 和 risk_type 不能为空");
        }
        String riskLevel = mapRiskLevel(riskType);
        RiskAlert alert = riskAlertService.receiveEvent(
                studentId, courseId, riskType, riskLevel, detail);
        if (alert == null) {
            return Result.fail("预警已存在或处于冷却期，未重复创建");
        }
        return Result.ok(alert);
    }

    /** R8.5 查询学生当前风险状态（供模块4调用） */
    @GetMapping("/api/students/{id}/risk-status")
    public Result<RiskAlertService.RiskStatus> getStudentRiskStatus(@PathVariable String id) {
        return Result.ok(riskAlertService.getStudentRiskStatus(id));
    }

    /** R4.5 查询班级活跃预警列表（教师端） */
    @GetMapping("/api/classes/{id}/risk-alerts")
    public Result<List<RiskAlert>> getClassRiskAlerts(
            @PathVariable String id,
            @RequestParam(required = false, defaultValue = "active") String status,
            HttpSession session) {
        if (auth.getTeacher(session) == null) return Result.fail("请先登录");
        List<String> studentIds = classInfoService.getStudentIds(id);
        List<RiskAlert> alerts = riskAlertService.getActiveByClass(id, studentIds);
        return Result.ok(alerts);
    }

    /** R4.2 手动触发风险检测（教师端） */
    @PostMapping("/api/classes/{id}/risk-detect")
    public Result<List<RiskAlert>> detectRisks(@PathVariable String id,
                                                @RequestParam String courseId,
                                                HttpSession session) {
        if (auth.getTeacher(session) == null) return Result.fail("请先登录");
        List<RiskAlert> alerts = riskDetectionService.detectForClass(id, courseId);
        return Result.ok(alerts);
    }

    /** R4.7 标记预警为已处理 */
    @PutMapping("/api/risk-alerts/{id}/resolve")
    public Result<Void> resolve(@PathVariable String id, HttpSession session) {
        if (auth.getTeacher(session) == null) return Result.fail("请先登录");
        String teacherNo = String.valueOf(auth.getTeacher(session).getTeacherNo());
        boolean ok = riskAlertService.resolve(id, teacherNo);
        return ok ? Result.ok() : Result.fail("预警不存在或已处理");
    }

    /**
     * 风险类型 → 风险等级映射。
     * 游戏事件类型引用 {@link com.neu.CoursePlatform.common.GameEventTypes}，
     * 确保与模块4 发送的事件字符串完全一致。
     */
    private String mapRiskLevel(String riskType) {
        return switch (riskType) {
            case com.neu.CoursePlatform.common.GameEventTypes.HP_CRITICAL, "low_score" -> "high";
            case "procrastination", com.neu.CoursePlatform.common.GameEventTypes.STUCK_DETECTED,
                 com.neu.CoursePlatform.common.GameEventTypes.INACTIVE, "stuck" -> "medium";
            default -> "low";
        };
    }
}
