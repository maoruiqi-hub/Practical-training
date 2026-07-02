package com.neu.CoursePlatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.neu.CoursePlatform.entity.LearningBehaviorLog;
import com.neu.CoursePlatform.entity.LearningTask;
import com.neu.CoursePlatform.entity.Student;
import com.neu.CoursePlatform.entity.TaskAssignment;
import com.neu.CoursePlatform.entity.TaskSubmission;
import com.neu.CoursePlatform.service.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LearningProgressServiceImpl implements LearningProgressService {

    private final LearningTaskService taskService;
    private final TaskSubmissionService submissionService;
    private final TaskAssignmentService assignmentService;
    private final BehaviorLogService behaviorLogService;
    private final StudentService studentService;

    public LearningProgressServiceImpl(LearningTaskService taskService, TaskSubmissionService submissionService,
                                       TaskAssignmentService assignmentService, BehaviorLogService behaviorLogService,
                                       StudentService studentService) {
        this.taskService = taskService;
        this.submissionService = submissionService;
        this.assignmentService = assignmentService;
        this.behaviorLogService = behaviorLogService;
        this.studentService = studentService;
    }

    @Override
    public Map<String, Object> buildStudentProgress(String studentNo, String courseCode) {
        List<LearningTask> tasks = assignmentService.listAssignedTasks(studentNo, courseCode, null, null, null);
        List<TaskSubmission> submissions = submissionService.listByStudentNo(studentNo);

        // 建立 taskNo → submission 映射（取最新一次提交）
        Map<String, TaskSubmission> subMap = new HashMap<>();
        for (TaskSubmission sub : submissions) {
            TaskSubmission existing = subMap.get(sub.getTaskNo());
            if (existing == null || sub.getSubmitTime().isAfter(existing.getSubmitTime())) {
                subMap.put(sub.getTaskNo(), sub);
            }
        }

        List<Map<String, Object>> taskStatusList = new ArrayList<>();
        int completedCount = 0;

        for (LearningTask task : tasks) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("taskNo", task.getTaskNo());
            String taskName = task.getTaskName();
            if (taskName == null || taskName.isEmpty()) {
                taskName = task.getDescription();
            }
            if (taskName == null || taskName.isEmpty()) {
                taskName = "任务#" + task.getTaskNo();
            }
            item.put("taskName", taskName);
            item.put("taskType", task.getTaskType());
            item.put("deadline", task.getDeadline());
            item.put("score", task.getScore());

            TaskSubmission sub = subMap.get(task.getTaskNo());
            if (sub != null) {
                item.put("submitted", true);
                item.put("submissionId", sub.getSubmissionId());
                item.put("submitTime", sub.getSubmitTime());
                item.put("status", sub.getStatus());
                item.put("studentScore", sub.getScore());
                item.put("isOverdue", sub.getIsOverdue() != null && sub.getIsOverdue() == 1);
                // 判断逾期
                if (task.getDeadline() != null && sub.getSubmitTime().isAfter(task.getDeadline())) {
                    item.put("submissionStatus", "overdue");
                } else if ("graded".equals(sub.getStatus())) {
                    item.put("submissionStatus", "completed");
                } else {
                    item.put("submissionStatus", "submitted");
                }
                completedCount++;
            } else {
                item.put("submitted", false);
                if (task.getDeadline() != null && LocalDateTime.now().isAfter(task.getDeadline())) {
                    item.put("submissionStatus", "overdue_missing");
                } else {
                    item.put("submissionStatus", "pending");
                }
            }
            taskStatusList.add(item);
        }

        // 获取最近行为日志（最近10条，作为学习时间线）
        List<LearningBehaviorLog> logs = behaviorLogService.listByUserId(studentNo);
        List<Map<String, Object>> timeline = logs.stream()
                .sorted(Comparator.comparing(LearningBehaviorLog::getCreatedAt).reversed())
                .limit(10)
                .map(log -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("actionType", log.getActionType());
                    entry.put("resourceType", log.getResourceType());
                    entry.put("time", log.getCreatedAt());
                    entry.put("duration", log.getDuration());
                    return entry;
                })
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("studentNo", studentNo);
        result.put("courseCode", courseCode);
        result.put("totalTasks", tasks.size());
        result.put("completedCount", completedCount);
        result.put("completionRate", tasks.size() > 0 ? Math.round((double) completedCount / tasks.size() * 100) : 0);
        result.put("taskStatusList", taskStatusList);
        result.put("timeline", timeline);
        return result;
    }

    @Override
    public Map<String, Object> buildCourseProgress(String courseCode) {
        List<LearningTask> tasks = taskService.listByCourseCode(courseCode);
        List<TaskAssignment> assignments = assignmentService.list(new LambdaQueryWrapper<TaskAssignment>()
                .eq(TaskAssignment::getCourseCode, courseCode)
                .ne(TaskAssignment::getStatus, "cancelled"));

        // 课程进度矩阵以学生花名册为行来源，再叠加任务分配和提交状态。
        Set<String> studentNoSet = studentService.list().stream()
                .map(Student::getStudentNo)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, Map<String, TaskSubmission>> studentSubMap = new HashMap<>();
        Map<String, Set<String>> assignedTaskNosByStudent = new HashMap<>();

        for (TaskAssignment assignment : assignments) {
            studentNoSet.add(assignment.getStudentNo());
            assignedTaskNosByStudent.computeIfAbsent(assignment.getStudentNo(), key -> new HashSet<>())
                    .add(assignment.getTaskNo());
        }

        for (LearningTask task : tasks) {
            List<TaskSubmission> submissions = submissionService.listByTaskNo(task.getTaskNo());
            for (TaskSubmission sub : submissions) {
                studentNoSet.add(sub.getStudentNo());
                studentSubMap.computeIfAbsent(sub.getStudentNo(), k -> new HashMap<>())
                        .put(sub.getTaskNo(), sub);
            }
        }

        // 构建每个学生的进度行
        List<Map<String, Object>> studentRows = new ArrayList<>();
        for (String studentNo : studentNoSet) {
            Student s = studentService.getById(studentNo);
            Map<String, TaskSubmission> subMap = studentSubMap.getOrDefault(studentNo, Collections.emptyMap());

            int completedCount = 0;
            int overdueCount = 0;
            LocalDateTime lastActive = null;
            List<Map<String, Object>> taskCells = new ArrayList<>();
            Set<String> assignedTaskNos = assignedTaskNosByStudent.getOrDefault(studentNo, Collections.emptySet());

            for (LearningTask task : tasks) {
                TaskSubmission sub = subMap.get(task.getTaskNo());
                Map<String, Object> cell = new LinkedHashMap<>();
                cell.put("taskNo", task.getTaskNo());
                if (!assignedTaskNos.contains(task.getTaskNo())) {
                    cell.put("status", "unassigned");
                    cell.put("score", null);
                    cell.put("isOverdue", false);
                } else if (sub != null) {
                    cell.put("status", "graded".equals(sub.getStatus()) ? "completed" : "submitted");
                    cell.put("score", sub.getScore());
                    cell.put("isOverdue", sub.getIsOverdue() != null && sub.getIsOverdue() == 1);
                    completedCount++;
                    if (sub.getIsOverdue() != null && sub.getIsOverdue() == 1) overdueCount++;
                    if (lastActive == null || sub.getSubmitTime().isAfter(lastActive)) {
                        lastActive = sub.getSubmitTime();
                    }
                } else {
                    // 检查是否已逾期未提交
                    boolean isOverdueMissing = task.getDeadline() != null
                            && LocalDateTime.now().isAfter(task.getDeadline());
                    cell.put("status", isOverdueMissing ? "overdue_missing" : "not_started");
                    cell.put("score", null);
                    cell.put("isOverdue", false);
                }
                taskCells.add(cell);
            }

            int assignedCount = assignedTaskNos.size();
            double completionRate = assignedCount > 0 ? Math.round((double) completedCount / assignedCount * 100) : 0;
            boolean isLagging = assignedCount > 0 && completionRate < 50;
            boolean hasOverdueRisk = overdueCount >= 2;

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("studentNo", studentNo);
            row.put("studentName", s != null ? s.getName() : "");
            row.put("className", s != null ? s.getClassName() : "");
            row.put("completedCount", completedCount);
            row.put("totalTasks", assignedCount);
            row.put("completionRate", completionRate);
            row.put("overdueCount", overdueCount);
            row.put("lastActive", lastActive);
            row.put("isLagging", isLagging);
            row.put("hasOverdueRisk", hasOverdueRisk);
            row.put("taskCells", taskCells);
            studentRows.add(row);
        }

        // 按完成率排序，落后学生排前面
        studentRows.sort(Comparator
                .comparing((Map<String, Object> r) -> String.valueOf(r.getOrDefault("className", "")))
                .thenComparing(r -> String.valueOf(r.getOrDefault("studentNo", ""))));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("courseCode", courseCode);
        result.put("totalTasks", tasks.size());
        result.put("tasks", tasks.stream().map(t -> {
            Map<String, Object> taskInfo = new LinkedHashMap<>();
            taskInfo.put("taskNo", t.getTaskNo());
            // 优先用 taskName，为空则用 description 截短，再不行用 ID
            String displayName = t.getTaskName();
            if (displayName == null || displayName.isEmpty()) {
                displayName = t.getDescription();
            }
            if (displayName == null || displayName.isEmpty()) {
                displayName = "任务#" + t.getTaskNo();
            }
            if (displayName.length() > 12) {
                displayName = displayName.substring(0, 12) + "...";
            }
            taskInfo.put("taskName", displayName);
            taskInfo.put("taskType", t.getTaskType());
            taskInfo.put("deadline", t.getDeadline());
            return taskInfo;
        }).collect(Collectors.toList()));
        result.put("totalStudents", studentRows.size());
        result.put("studentRows", studentRows);

        // 汇总统计
        long laggingCount = studentRows.stream().filter(r -> (boolean) r.get("isLagging")).count();
        long riskCount = studentRows.stream().filter(r -> (boolean) r.get("hasOverdueRisk")).count();
        result.put("laggingCount", laggingCount);
        result.put("riskCount", riskCount);

        return result;
    }
}
