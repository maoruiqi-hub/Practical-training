package com.neu.CoursePlatform.controller;

import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.entity.LearningTask;
import com.neu.CoursePlatform.entity.Student;
import com.neu.CoursePlatform.entity.TaskAssignment;
import com.neu.CoursePlatform.service.LearningTaskService;
import com.neu.CoursePlatform.service.TaskAssignmentService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class TaskAssignmentController {

    private final TaskAssignmentService assignmentService;
    private final LearningTaskService taskService;
    private final Auth auth;

    public TaskAssignmentController(TaskAssignmentService assignmentService, LearningTaskService taskService, Auth auth) {
        this.assignmentService = assignmentService;
        this.taskService = taskService;
        this.auth = auth;
    }

    @GetMapping("/api/students/{studentNo}/assigned-tasks")
    public Result<List<LearningTask>> listAssignedTasks(@PathVariable String studentNo,
                                                        @RequestParam(required = false) String courseCode,
                                                        @RequestParam(name = "course_id", required = false) String courseId,
                                                        @RequestParam(required = false) String taskType,
                                                        @RequestParam(required = false) String status,
                                                        @RequestParam(required = false) String lessonNo,
                                                        HttpSession session) {
        Student student = auth.getStudent(session);
        if (!auth.isAdmin(session) && !auth.isTeacher(session)
                && (student == null || !student.getStudentNo().equals(studentNo))) {
            return Result.fail("无权限");
        }
        return Result.ok(assignmentService.listAssignedTasks(studentNo,
                firstNonBlank(courseCode, courseId), taskType, status, lessonNo));
    }

    @DeleteMapping("/api/task-assignments/{assignmentId}")
    public Result<String> cancelAssignment(@PathVariable String assignmentId, HttpSession session) {
        TaskAssignment assignment = assignmentService.getById(assignmentId);
        if (assignment == null) return Result.fail("任务分配不存在");
        if (!auth.canModifyCourse(session, assignment.getCourseCode())) return Result.fail("无权限");
        assignment.setStatus("cancelled");
        assignmentService.updateById(assignment);
        return Result.ok("任务分配已撤回");
    }

    @GetMapping("/api/tasks/{taskNo}/assignments/count")
    public Result<Long> countAssignments(@PathVariable String taskNo, HttpSession session) {
        LearningTask task = taskService.getById(taskNo);
        if (task == null) return Result.fail("任务不存在");
        if (!auth.canModifyCourse(session, task.getCourseCode())) return Result.fail("无权限");
        return Result.ok(assignmentService.countActiveByTaskNo(taskNo));
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }
}
