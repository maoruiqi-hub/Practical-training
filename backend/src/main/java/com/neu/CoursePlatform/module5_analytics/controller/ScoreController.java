package com.neu.CoursePlatform.module5_analytics.controller;

import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.module5_analytics.dto.ScoreOverviewDTO;
import com.neu.CoursePlatform.module5_analytics.dto.ScoreTrendDTO;
import com.neu.CoursePlatform.module5_analytics.dto.WeakPointDTO;
import com.neu.CoursePlatform.module5_analytics.service.ScoreAnalysisService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 成绩分析 Controller（R2 组需求, T3+T4）
 */
@RestController
@RequestMapping("/api/classes/{classId}")
public class ScoreController {

    private final ScoreAnalysisService scoreAnalysisService;
    private final Auth auth;

    public ScoreController(ScoreAnalysisService scoreAnalysisService, Auth auth) {
        this.scoreAnalysisService = scoreAnalysisService;
        this.auth = auth;
    }

    /** R2.1 班级成绩总览 */
    @GetMapping("/scores")
    public Result<ScoreOverviewDTO> getScores(@PathVariable String classId,
                                               @RequestParam String courseId,
                                               HttpSession session) {
        if (auth.getTeacher(session) == null) return Result.fail("请先登录");
        return Result.ok(scoreAnalysisService.getClassScoreOverview(classId, courseId));
    }

    /** R2.4 班级薄弱知识点排名 */
    @GetMapping("/weak-points")
    public Result<List<WeakPointDTO>> getWeakPoints(@PathVariable String classId,
                                                     @RequestParam String courseId,
                                                     HttpSession session) {
        if (auth.getTeacher(session) == null) return Result.fail("请先登录");
        return Result.ok(scoreAnalysisService.getWeakPoints(courseId));
    }

    /** R2.5 分数分布统计 */
    @GetMapping("/score-distribution")
    public Result<List> getDistribution(@PathVariable String classId,
                                         @RequestParam String courseId,
                                         HttpSession session) {
        if (auth.getTeacher(session) == null) return Result.fail("请先登录");
        ScoreOverviewDTO overview = scoreAnalysisService.getClassScoreOverview(classId, courseId);
        return Result.ok(overview.getDistribution());
    }

    /** R2.2 成绩趋势 */
    @GetMapping("/score-trends")
    public Result<ScoreTrendDTO> getScoreTrends(@PathVariable String classId,
                                                 @RequestParam String courseId,
                                                 @RequestParam(required = false, defaultValue = "week") String granularity,
                                                 @RequestParam(required = false) String studentId,
                                                 HttpSession session) {
        if (auth.getTeacher(session) == null) return Result.fail("请先登录");
        if (studentId != null) {
            return Result.ok(scoreAnalysisService.getStudentScoreTrends(studentId, courseId, granularity));
        }
        return Result.ok(scoreAnalysisService.getScoreTrends(classId, courseId, granularity));
    }
}
