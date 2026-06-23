package com.neu.CoursePlatform.module5_analytics.controller;

import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.module5_analytics.service.TeachingSuggestionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 教学建议 Controller（R6 组需求, T9 + T10反馈汇总）
 */
@RestController
public class SuggestionController {

    private final TeachingSuggestionService suggestionService;
    private final Auth auth;

    public SuggestionController(TeachingSuggestionService suggestionService, Auth auth) {
        this.suggestionService = suggestionService;
        this.auth = auth;
    }

    /** R6.1 生成班级教学建议（→ agentic） */
    @PostMapping("/api/classes/{classId}/teaching-suggestions")
    public Result<List<Map<String, Object>>> generateSuggestions(
            @PathVariable String classId,
            @RequestParam String courseId,
            HttpSession session) {
        if (auth.getTeacher(session) == null) return Result.fail("请先登录");
        List<Map<String, Object>> suggestions = suggestionService.generateForClass(classId, courseId);
        if (suggestions == null) {
            return Result.fail("AI 服务暂不可用，请稍后重试");
        }
        return Result.ok(suggestions);
    }

    /** R6.4 获取历史教学建议 */
    @GetMapping("/api/classes/{classId}/teaching-suggestions")
    public Result<List<Map<String, Object>>> getHistory(
            @PathVariable String classId,
            HttpSession session) {
        if (auth.getTeacher(session) == null) return Result.fail("请先登录");
        return Result.ok(suggestionService.getHistory(classId));
    }

    /** R6.3 生成个别学生干预建议（→ agentic） */
    @PostMapping("/api/students/{studentId}/intervention")
    public Result<List<Map<String, Object>>> generateIntervention(
            @PathVariable String studentId,
            @RequestParam String courseId,
            HttpSession session) {
        if (auth.getTeacher(session) == null) return Result.fail("请先登录");
        List<Map<String, Object>> suggestions = suggestionService.generateForStudent(studentId, courseId);
        if (suggestions == null) {
            return Result.fail("AI 服务暂不可用，请稍后重试");
        }
        return Result.ok(suggestions);
    }

    /** R5.6 学生提问与反馈汇总（T10） */
    @GetMapping("/api/classes/{classId}/feedback-summary")
    public Result<List<Map<String, String>>> getFeedbackSummary(
            @PathVariable String classId,
            HttpSession session) {
        if (auth.getTeacher(session) == null) return Result.fail("请先登录");
        // Phase 3 Mock: 返回空列表；Phase 4 从模块2获取反馈数据
        return Result.ok(List.of());
    }
}
