package com.neu.CoursePlatform.controller;

import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.entity.LearningTask;
import com.neu.CoursePlatform.entity.Student;
import com.neu.CoursePlatform.service.AnalysisService;
import com.neu.CoursePlatform.service.LearningTaskService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

    private final AnalysisService analysisService;
    private final LearningTaskService taskService;
    private final Auth auth;

    public AnalysisController(AnalysisService analysisService, LearningTaskService taskService, Auth auth) {
        this.analysisService = analysisService;
        this.taskService = taskService;
        this.auth = auth;
    }

    /** 学生个人错题统计 */
    @GetMapping("/students/{studentNo}/mistakes")
    public Result<Map<String, Object>> studentWrongQuestions(@PathVariable String studentNo,
                                                             @RequestParam(required = false) String taskNo,
                                                             @RequestParam(required = false) String knowledgePointId,
                                                             @RequestParam(required = false) String type,
                                                             HttpSession session) {
        Student student = (Student) session.getAttribute("student");
        if (student != null && !studentNo.equals(student.getStudentNo())) return Result.fail("无权限");
        if (student == null && !auth.isAdmin(session)) return Result.fail("无权限");
        return Result.ok(analysisService.buildStudentWrongStats(studentNo, taskNo, knowledgePointId, type));
    }

    /** 单个测验错题统计 */
    @GetMapping("/tasks/{taskNo}/mistakes")
    public Result<Map<String, Object>> taskWrongQuestions(@PathVariable String taskNo, HttpSession session) {
        LearningTask task = taskService.getById(taskNo);
        if (task == null) return Result.fail("任务不存在");
        if (!auth.canModifyCourse(session, task.getCourseCode())) return Result.fail("无权限");
        return Result.ok(analysisService.buildTaskWrongStats(taskNo));
    }

    /** 课程维度班级错题与薄弱知识点统计 */
    @GetMapping("/courses/{courseCode}/mistake-stats")
    public Result<Map<String, Object>> courseWrongQuestions(@PathVariable String courseCode, HttpSession session) {
        if (!auth.canModifyCourse(session, courseCode)) return Result.fail("无权限");
        return Result.ok(analysisService.buildCourseWrongStats(courseCode));
    }
}
