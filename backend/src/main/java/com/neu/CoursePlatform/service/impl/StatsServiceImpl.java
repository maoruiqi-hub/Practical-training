package com.neu.CoursePlatform.service.impl;

import com.neu.CoursePlatform.entity.LearningBehaviorLog;
import com.neu.CoursePlatform.entity.LearningTask;
import com.neu.CoursePlatform.entity.TaskSubmission;
import com.neu.CoursePlatform.service.BehaviorLogService;
import com.neu.CoursePlatform.service.LearningTaskService;
import com.neu.CoursePlatform.service.StatsService;
import com.neu.CoursePlatform.service.TaskSubmissionService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class StatsServiceImpl implements StatsService {

    private final TaskSubmissionService submissionService;
    private final LearningTaskService taskService;
    private final BehaviorLogService behaviorLogService;

    public StatsServiceImpl(TaskSubmissionService submissionService, LearningTaskService taskService,
                             BehaviorLogService behaviorLogService) {
        this.submissionService = submissionService;
        this.taskService = taskService;
        this.behaviorLogService = behaviorLogService;
    }

    @Override
    public Map<String, Object> buildStudentStats(String studentNo) {
        List<TaskSubmission> submissions = submissionService.listByStudentNo(studentNo);
        List<Map<String, Object>> detail = new ArrayList<>();
        int totalScore = 0;
        int gradedCount = 0;
        int overdueCount = 0;
        int completedCount = 0;

        for (TaskSubmission sub : submissions) {
            LearningTask task = taskService.getById(sub.getTaskNo());
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
        List<LearningBehaviorLog> logs = behaviorLogService.listByUserId(studentNo);
        long totalDurationSec = logs.stream()
                .filter(l -> l.getDuration() != null)
                .mapToLong(LearningBehaviorLog::getDuration)
                .sum();

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
        List<LearningTask> tasks = taskService.listByCourseCode(courseCode);
        List<TaskSubmission> submissions = submissionService.listByStudentNo(studentNo);

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
        List<LearningTask> tasks = taskService.listByCourseCode(courseCode);
        List<Map<String, Object>> taskStats = new ArrayList<>();

        int totalSubmissionsAll = 0;
        int totalOverdueAll = 0;

        for (LearningTask task : tasks) {
            List<TaskSubmission> submissions = submissionService.listByTaskNo(task.getTaskNo());
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
}
