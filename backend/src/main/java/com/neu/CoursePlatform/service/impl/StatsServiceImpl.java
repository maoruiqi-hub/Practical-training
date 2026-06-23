package com.neu.CoursePlatform.service.impl;

import com.neu.CoursePlatform.entity.LearningTask;
import com.neu.CoursePlatform.entity.TaskSubmission;
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

    public StatsServiceImpl(TaskSubmissionService submissionService, LearningTaskService taskService) {
        this.submissionService = submissionService;
        this.taskService = taskService;
    }

    @Override
    public Map<String, Object> buildStudentStats(String studentNo) {
        List<TaskSubmission> submissions = submissionService.listByStudentNo(studentNo);
        List<Map<String, Object>> detail = new ArrayList<>();
        int totalScore = 0;
        int gradedCount = 0;

        for (TaskSubmission sub : submissions) {
            LearningTask task = taskService.getById(sub.getTaskNo());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("taskNo", sub.getTaskNo());
            item.put("taskName", task != null ? task.getDescription() : "");
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
        return result;
    }

    @Override
    public Map<String, Object> buildCourseStats(String courseCode) {
        List<LearningTask> tasks = taskService.listByCourseCode(courseCode);
        List<Map<String, Object>> taskStats = new ArrayList<>();

        for (LearningTask task : tasks) {
            List<TaskSubmission> submissions = submissionService.listByTaskNo(task.getTaskNo());
            int graded = (int) submissions.stream().filter(s -> "graded".equals(s.getStatus())).count();
            double avg = submissions.stream()
                    .filter(s -> "graded".equals(s.getStatus()) && s.getScore() != null)
                    .mapToInt(TaskSubmission::getScore)
                    .average()
                    .orElse(0);

            Map<String, Object> taskStat = new LinkedHashMap<>();
            taskStat.put("taskNo", task.getTaskNo());
            taskStat.put("taskName", task.getDescription());
            taskStat.put("taskType", task.getTaskType());
            taskStat.put("submittedCount", submissions.size());
            taskStat.put("gradedCount", graded);
            taskStat.put("averageScore", Math.round(avg * 10) / 10.0);
            taskStats.add(taskStat);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("courseCode", courseCode);
        result.put("taskCount", tasks.size());
        result.put("taskStats", taskStats);
        return result;
    }
}
