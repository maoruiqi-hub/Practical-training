package com.neu.CoursePlatform.service.impl;

import com.neu.CoursePlatform.entity.LearningBehaviorLog;
import com.neu.CoursePlatform.entity.LearningTask;
import com.neu.CoursePlatform.entity.TaskSubmission;
import com.neu.CoursePlatform.service.BehaviorLogService;
import com.neu.CoursePlatform.service.LearningTaskService;
import com.neu.CoursePlatform.service.StatsService;
import com.neu.CoursePlatform.service.TaskAssignmentService;
import com.neu.CoursePlatform.service.TaskSubmissionService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StatsServiceImpl implements StatsService {

    private final TaskSubmissionService submissionService;
    private final LearningTaskService taskService;
    private final TaskAssignmentService assignmentService;
    private final BehaviorLogService behaviorLogService;

    public StatsServiceImpl(TaskSubmissionService submissionService, LearningTaskService taskService,
                            TaskAssignmentService assignmentService, BehaviorLogService behaviorLogService) {
        this.submissionService = submissionService;
        this.taskService = taskService;
        this.assignmentService = assignmentService;
        this.behaviorLogService = behaviorLogService;
    }

    @Override
    public Map<String, Object> buildStudentStats(String studentNo) {
        List<TaskSubmission> submissions = submissionService.listByStudentNo(studentNo);
        Set<String> taskNos = submissions.stream()
                .map(TaskSubmission::getTaskNo)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, LearningTask> taskIndex = taskNos.isEmpty()
                ? Map.of()
                : safeTaskBatch(taskNos).stream()
                        .collect(Collectors.toMap(LearningTask::getTaskNo, task -> task, (a, b) -> a));
        List<Map<String, Object>> detail = new ArrayList<>();
        int totalScore = 0;
        int gradedCount = 0;
        int overdueCount = 0;
        int completedCount = 0;

        for (TaskSubmission sub : submissions) {
            LearningTask task = taskIndex.get(sub.getTaskNo());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("taskNo", sub.getTaskNo());
            String tn = task != null ? task.getTaskName() : "";
            if ((tn == null || tn.isEmpty()) && task != null && task.getDescription() != null) tn = task.getDescription();
            if (tn == null || tn.isEmpty()) tn = task != null ? "任务#" + task.getTaskNo() : "";
            item.put("taskName", tn);
            item.put("taskType", task != null ? task.getTaskType() : "");
            item.put("score", sub.getScore());
            item.put("status", sub.getStatus());
            item.put("submitTime", sub.getSubmitTime());
            item.put("deadline", task != null ? task.getDeadline() : null);
            item.put("isOverdue", sub.getIsOverdue() != null && sub.getIsOverdue() == 1);
            if ("graded".equals(sub.getStatus()) && sub.getScore() != null) {
                totalScore += sub.getScore();
                gradedCount++;
            }
            if ("graded".equals(sub.getStatus()) || "submitted".equals(sub.getStatus())) {
                completedCount++;
            }
            if (sub.getIsOverdue() != null && sub.getIsOverdue() == 1) {
                overdueCount++;
            }
            detail.add(item);
        }

        detail.sort(Comparator.comparing(m -> (LocalDateTime) m.get("submitTime"),
                Comparator.nullsLast(Comparator.naturalOrder())));

        // 从行为日志计算总学习时长
        long totalDurationSec = totalDuration(studentNo);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("studentNo", studentNo);
        result.put("totalSubmissions", submissions.size());
        result.put("completedCount", completedCount);
        result.put("gradedCount", gradedCount);
        result.put("overdueCount", overdueCount);
        result.put("averageScore", gradedCount > 0 ? Math.round((double) totalScore / gradedCount * 10) / 10.0 : 0);
        result.put("totalStudyDuration", totalDurationSec);
        result.put("details", detail);
        return result;
    }

    @Override
    public Map<String, Object> buildStudentCourseStats(String studentNo, String courseCode) {
        List<LearningTask> tasks = assignmentService.listAssignedTasks(studentNo, courseCode, null, null, null);
        List<TaskSubmission> submissions = submissionService.listByStudentNoAndCourse(studentNo, courseCode);
        if (tasks == null || tasks.isEmpty()) {
            List<LearningTask> courseTasks = taskService.listByCourseCode(courseCode);
            if (courseTasks != null) tasks = courseTasks;
        }
        if (tasks == null) tasks = List.of();
        if (submissions == null) {
            submissions = submissionService.listByStudentNo(studentNo);
            if (submissions == null) submissions = List.of();
        }

        Set<String> submittedTaskNos = new HashSet<>();
        Map<String, TaskSubmission> subMap = new HashMap<>();
        int gradedTotal = 0;
        int gradedCount = 0;
        int overdueCount = 0;

        for (TaskSubmission sub : submissions) {
            submittedTaskNos.add(sub.getTaskNo());
            subMap.put(sub.getTaskNo(), sub);
            if ("graded".equals(sub.getStatus()) && sub.getScore() != null) {
                gradedTotal += sub.getScore();
                gradedCount++;
            }
            if (sub.getIsOverdue() != null && sub.getIsOverdue() == 1) {
                overdueCount++;
            }
        }

        int completedCount = 0;
        for (LearningTask task : tasks) {
            if (submittedTaskNos.contains(task.getTaskNo())) completedCount++;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("studentNo", studentNo);
        result.put("courseCode", courseCode);
        result.put("totalTasks", tasks.size());
        result.put("completedCount", completedCount);
        result.put("completionRate", tasks.size() > 0 ? Math.round((double) completedCount / tasks.size() * 100) : 0);
        result.put("gradedCount", gradedCount);
        result.put("averageScore", gradedCount > 0 ? Math.round((double) gradedTotal / gradedCount * 10) / 10.0 : 0);
        result.put("overdueCount", overdueCount);
        return result;
    }

    @Override
    public Map<String, Object> buildCourseStats(String courseCode) {
        List<Map<String, Object>> aggregatedStats = submissionService.aggregateCourseTaskStats(courseCode);
        if (aggregatedStats == null || aggregatedStats.isEmpty()) {
            return buildCourseStatsFallback(courseCode);
        }
        List<Map<String, Object>> taskStats = new ArrayList<>();

        int totalSubmissionsAll = 0;
        int totalOverdueAll = 0;

        for (Map<String, Object> row : aggregatedStats) {
            int submittedCount = (int) statLong(row, "submitted_count");
            int overdue = (int) statLong(row, "overdue_count");
            Map<String, Object> taskStat = new LinkedHashMap<>();
            String taskNo = statString(row, "task_no");
            taskStat.put("taskNo", taskNo);
            String tname = statString(row, "task_name");
            if (tname == null || tname.isEmpty()) tname = statString(row, "description");
            if (tname == null || tname.isEmpty()) tname = "任务#" + taskNo;
            taskStat.put("taskName", tname);
            taskStat.put("taskType", statString(row, "task_type"));
            taskStat.put("assignedCount", statLong(row, "assigned_count"));
            taskStat.put("submittedCount", submittedCount);
            taskStat.put("gradedCount", statLong(row, "graded_count"));
            taskStat.put("overdueCount", overdue);
            taskStat.put("averageScore", Math.round(statDouble(row, "average_score") * 10) / 10.0);
            taskStats.add(taskStat);

            totalSubmissionsAll += submittedCount;
            totalOverdueAll += overdue;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("courseCode", courseCode);
        result.put("taskCount", taskStats.size());
        result.put("totalSubmissions", totalSubmissionsAll);
        result.put("totalOverdue", totalOverdueAll);
        result.put("taskStats", taskStats);
        return result;
    }

    private Map<String, Object> buildCourseStatsFallback(String courseCode) {
        List<LearningTask> tasks = taskService.listByCourseCode(courseCode);
        if (tasks == null) tasks = List.of();
        List<Map<String, Object>> taskStats = new ArrayList<>();
        int totalSubmissionsAll = 0;
        int totalOverdueAll = 0;

        for (LearningTask task : tasks) {
            List<TaskSubmission> submissions = submissionService.listByTaskNo(task.getTaskNo());
            if (submissions == null) submissions = List.of();
            int graded = (int) submissions.stream().filter(s -> "graded".equals(s.getStatus())).count();
            int overdue = (int) submissions.stream().filter(s -> s.getIsOverdue() != null && s.getIsOverdue() == 1).count();
            double avg = submissions.stream()
                    .filter(s -> "graded".equals(s.getStatus()) && s.getScore() != null)
                    .mapToInt(TaskSubmission::getScore)
                    .average()
                    .orElse(0);

            Map<String, Object> taskStat = new LinkedHashMap<>();
            taskStat.put("taskNo", task.getTaskNo());
            String tname = task.getTaskName();
            if (tname == null || tname.isEmpty()) tname = task.getDescription();
            if (tname == null || tname.isEmpty()) tname = "任务#" + task.getTaskNo();
            taskStat.put("taskName", tname);
            taskStat.put("taskType", task.getTaskType());
            taskStat.put("assignedCount", countActiveAssignments(task.getTaskNo()));
            taskStat.put("submittedCount", submissions.size());
            taskStat.put("gradedCount", graded);
            taskStat.put("overdueCount", overdue);
            taskStat.put("averageScore", Math.round(avg * 10) / 10.0);
            taskStats.add(taskStat);

            totalSubmissionsAll += submissions.size();
            totalOverdueAll += overdue;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("courseCode", courseCode);
        result.put("taskCount", tasks.size());
        result.put("totalSubmissions", totalSubmissionsAll);
        result.put("totalOverdue", totalOverdueAll);
        result.put("taskStats", taskStats);
        return result;
    }

    private Collection<LearningTask> safeTaskBatch(Set<String> taskNos) {
        Collection<LearningTask> tasks = taskService.listByIds(taskNos);
        if (tasks != null) return tasks;
        return taskNos.stream()
                .map(taskService::getById)
                .filter(Objects::nonNull)
                .toList();
    }

    private long totalDuration(String studentNo) {
        try {
            return behaviorLogService.sumDurationByUserId(studentNo);
        } catch (RuntimeException e) {
            List<LearningBehaviorLog> logs = behaviorLogService.listByUserId(studentNo);
            if (logs == null) return 0;
            return logs.stream()
                    .filter(l -> l.getDuration() != null)
                    .mapToLong(LearningBehaviorLog::getDuration)
                    .sum();
        }
    }

    private long countActiveAssignments(String taskNo) {
        try {
            return assignmentService.countActiveByTaskNo(taskNo);
        } catch (RuntimeException e) {
            return 0;
        }
    }

    private long statLong(Map<String, Object> stats, String key) {
        return Math.round(statDouble(stats, key));
    }

    private double statDouble(Map<String, Object> stats, String key) {
        Object value = statValue(stats, key);
        if (value instanceof Number number) return number.doubleValue();
        if (value == null) return 0;
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String statString(Map<String, Object> stats, String key) {
        Object value = statValue(stats, key);
        if (value == null) return "";
        String str = String.valueOf(value);
        return str.isBlank() ? "" : str;
    }

    private Object statValue(Map<String, Object> stats, String key) {
        if (stats == null || stats.isEmpty()) return null;
        Object value = stats.get(key);
        if (value != null) return value;
        String normalizedKey = normalizeStatKey(key);
        for (Map.Entry<String, Object> entry : stats.entrySet()) {
            if (entry.getKey() != null
                    && (entry.getKey().equalsIgnoreCase(key)
                    || normalizeStatKey(entry.getKey()).equals(normalizedKey))) {
                return entry.getValue();
            }
        }
        return null;
    }

    private String normalizeStatKey(String key) {
        return key == null ? "" : key.replace("_", "").toLowerCase();
    }
}
