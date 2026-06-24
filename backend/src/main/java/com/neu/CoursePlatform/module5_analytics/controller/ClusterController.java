package com.neu.CoursePlatform.module5_analytics.controller;

import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.module5_analytics.service.ProblemClusterService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 共性问题聚类 Controller（R5 组需求, T8）
 */
@RestController
@RequestMapping("/api/classes/{classId}")
public class ClusterController {

    private final ProblemClusterService clusterService;
    private final Auth auth;

    public ClusterController(ProblemClusterService clusterService, Auth auth) {
        this.clusterService = clusterService;
        this.auth = auth;
    }

    /** R5.1 触发共性问题聚类分析（→ agentic） */
    @PostMapping("/problem-cluster")
    public Result<List<Map<String, Object>>> triggerCluster(
            @PathVariable String classId,
            @RequestParam String courseId,
            HttpSession session) {
        if (auth.getTeacher(session) == null) return Result.fail("请先登录");
        List<Map<String, Object>> result = clusterService.cluster(classId, courseId);
        if (result == null) {
            // R5.5: agentic 不可用
            return Result.fail("AI 服务暂不可用，请稍后重试");
        }
        return Result.ok(result);
    }

    /** R5.4 获取最新聚类报告 */
    @GetMapping("/problem-cluster")
    public Result<List<Map<String, Object>>> getLatestCluster(
            @PathVariable String classId,
            HttpSession session) {
        if (auth.getTeacher(session) == null) return Result.fail("请先登录");
        return Result.ok(clusterService.getLatestCluster(classId));
    }
}
