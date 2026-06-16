package com.neu.CoursePlatform.controller;

import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.entity.LearningTask;
import com.neu.CoursePlatform.entity.Student;
import com.neu.CoursePlatform.entity.TaskSubmission;
import com.neu.CoursePlatform.service.LearningTaskService;
import com.neu.CoursePlatform.service.TaskSubmissionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/stats")
public class StatsController {

    private final TaskSubmissionService submissionService;
    private final LearningTaskService taskService;
    private final Auth auth;

    public StatsController(TaskSubmissionService submissionService, LearningTaskService taskService, Auth auth) {
        this.submissionService = submissionService;
        this.taskService = taskService;
        this.auth = auth;
    }

    /** 学生成绩总览 | student本人 / admin */
    @GetMapping("/student/{studentNo}")
    public Result<Map<String, Object>> studentStats(@PathVariable String studentNo, HttpSession session) {
        Student loginStudent = (Student) session.getAttribute("student");
        if (!auth.isAdmin(session) && (loginStudent == null || !loginStudent.getStudentNo().equals(studentNo))) {
            return Result.fail("无权限");
        }

        List<TaskSubmission> submissions = submissionService.listByStudentNo(studentNo);

        List<Map<String, Object>> detail = new ArrayList<>();
        int totalScore = 0;
        int gradedCount = 0;

        for (TaskSubmission sub : submissions) {
            LearningTask task = taskService.getById(sub.getTaskNo());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("taskNo", sub.getTaskNo());
            item.put("taskType", task != null ? task.getTaskType() : "");
            item.put("score", sub.getScore());
            item.put("status", sub.getStatus());
            item.put("submitTime", sub.getSubmitTime());
            item.put("deadline", task != null ? task.getDeadline() : null);
            if ("graded".equals(sub.getStatus()) && sub.getScore() != null) {
                totalScore += sub.getScore();
                gradedCount++;
            }
            detail.add(item);
        }

        detail.sort(Comparator.comparing(m -> (LocalDateTime) m.get("submitTime"),
                Comparator.nullsLast(Comparator.naturalOrder())));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("studentNo", studentNo);
        result.put("totalSubmissions", submissions.size());
        result.put("gradedCount", gradedCount);
        result.put("averageScore", gradedCount > 0 ? (double) totalScore / gradedCount : 0);
        result.put("details", detail);
        return Result.ok(result);
    }

    /** 课程成绩总览 | admin/授课教师 */
    @GetMapping("/course/{courseCode}")
    public Result<Map<String, Object>> courseStats(@PathVariable String courseCode, HttpSession session) {
        if (!auth.canModifyCourse(session, courseCode)) return Result.fail("无权限");

        List<LearningTask> tasks = taskService.listByCourseCode(courseCode);
        List<Map<String, Object>> taskStats = new ArrayList<>();

        for (LearningTask task : tasks) {
            List<TaskSubmission> subs = submissionService.listByTaskNo(task.getTaskNo());
            int graded = (int) subs.stream().filter(s -> "graded".equals(s.getStatus())).count();
            double avg = subs.stream().filter(s -> "graded".equals(s.getStatus()) && s.getScore() != null)
                    .mapToInt(TaskSubmission::getScore).average().orElse(0);

            Map<String, Object> ts = new LinkedHashMap<>();
            ts.put("taskNo", task.getTaskNo());
            ts.put("taskType", task.getTaskType());
            ts.put("submittedCount", subs.size());
            ts.put("gradedCount", graded);
            ts.put("averageScore", Math.round(avg * 10) / 10.0);
            taskStats.add(ts);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("courseCode", courseCode);
        result.put("taskCount", tasks.size());
        result.put("taskStats", taskStats);
        return Result.ok(result);
    }
}
