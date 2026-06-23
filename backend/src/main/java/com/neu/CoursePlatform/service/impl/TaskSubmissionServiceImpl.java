package com.neu.CoursePlatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neu.CoursePlatform.dto.TaskSubmissionDTO;
import com.neu.CoursePlatform.entity.LearningTask;
import com.neu.CoursePlatform.entity.Question;
import com.neu.CoursePlatform.entity.Student;
import com.neu.CoursePlatform.entity.TaskSubmission;
import com.neu.CoursePlatform.mapper.TaskSubmissionMapper;
import com.neu.CoursePlatform.service.LearningTaskService;
import com.neu.CoursePlatform.service.QuestionService;
import com.neu.CoursePlatform.service.StudentService;
import com.neu.CoursePlatform.service.TaskSubmissionService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TaskSubmissionServiceImpl extends ServiceImpl<TaskSubmissionMapper, TaskSubmission> implements TaskSubmissionService {

    private final LearningTaskService taskService;
    private final StudentService studentService;
    private final QuestionService questionService;

    public TaskSubmissionServiceImpl(LearningTaskService taskService, StudentService studentService, QuestionService questionService) {
        this.taskService = taskService;
        this.studentService = studentService;
        this.questionService = questionService;
    }

    @Override
    public List<TaskSubmission> listByStudentNo(String studentNo) {
        return baseMapper.selectByStudentNo(studentNo);
    }

    @Override
    public List<TaskSubmission> listByTaskNo(String taskNo) {
        return baseMapper.selectByTaskNo(taskNo);
    }

    @Override
    public List<TaskSubmissionDTO> listDtoByTaskNo(String taskNo) {
        LearningTask task = taskService.getById(taskNo);
        List<TaskSubmission> list = baseMapper.selectByTaskNo(taskNo);
        List<TaskSubmissionDTO> dtos = new ArrayList<>();
        for (TaskSubmission sub : list) {
            TaskSubmissionDTO dto = new TaskSubmissionDTO();
            dto.setSubmissionId(sub.getSubmissionId());
            dto.setTaskNo(sub.getTaskNo());
            dto.setTaskName(task != null ? task.getDescription() : "");
            dto.setTaskType(task != null ? task.getTaskType() : "");
            dto.setStudentNo(sub.getStudentNo());
            Student stu = studentService.getById(sub.getStudentNo());
            dto.setStudentName(stu != null ? stu.getName() : "");
            dto.setContent(sub.getContent());
            dto.setFilePath(sub.getFilePath());
            dto.setSubmitTime(sub.getSubmitTime());
            dto.setScore(sub.getScore());
            dto.setStatus(sub.getStatus());
            dto.setFeedback(sub.getFeedback());
            dtos.add(dto);
        }
        return dtos;
    }

    @Override
    public boolean isTaskOverdue(String taskNo) {
        LearningTask task = taskService.getById(taskNo);
        return task != null && task.getDeadline() != null && LocalDateTime.now().isAfter(task.getDeadline());
    }

    @Override
    public boolean hasSubmitted(String taskNo, String studentNo) {
        return baseMapper.selectCount(new QueryWrapper<TaskSubmission>()
                .eq("task_no", taskNo).eq("student_no", studentNo)) > 0;
    }

    @Override
    public String getTaskCourseCode(String taskNo) {
        LearningTask task = taskService.getById(taskNo);
        return task != null ? task.getCourseCode() : null;
    }

    @Override
    public Map<String, Object> buildGradeDetail(String submissionId) {
        TaskSubmission sub = getById(submissionId);
        if (sub == null) return null;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("submissionId", sub.getSubmissionId());
        result.put("studentNo", sub.getStudentNo());
        result.put("content", sub.getContent());
        LearningTask task = taskService.getById(sub.getTaskNo());
        result.put("taskName", task != null ? task.getDescription() : "");
        result.put("taskType", task != null ? task.getTaskType() : "");
        result.put("score", sub.getScore());
        result.put("status", sub.getStatus());
        result.put("feedback", sub.getFeedback());
        List<Map<String, Object>> details = new ArrayList<>();
        try {
            ObjectMapper mapper = new ObjectMapper();
            var answers = mapper.readValue(sub.getContent(), List.class);
            for (Object a : answers) {
                Map<String, Object> ans = (Map<String, Object>) a;
                Question q = questionService.getById(String.valueOf(ans.get("no")));
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("questionId", ans.get("no"));
                item.put("stem", q != null ? q.getStem() : "");
                item.put("type", q != null ? q.getType() : "");
                item.put("studentAnswer", ans.getOrDefault("response", ""));
                item.put("correctAnswer", q != null ? q.getAnswer() : "");
                item.put("score", q != null ? q.getScore() : 0);
                item.put("autoGradable", q != null && isAutoGradable(q));
                item.put("correct", q != null && isAnswerCorrect(q, ans.get("response")));
                details.add(item);
            }
        } catch (Exception ignored) {}
        result.put("details", details);
        result.put("autoScore", autoScoreChoices(sub));
        result.put("needsManualReview", containsManualQuestions(sub));
        return result;
    }

    @Override
    public void applyInitialGrading(TaskSubmission sub) {
        LearningTask task = taskService.getById(sub.getTaskNo());
        if (task == null || !"quiz".equals(task.getTaskType())) {
            sub.setStatus("submitted");
            return;
        }

        sub.setScore(autoScoreChoices(sub));
        if (containsManualQuestions(sub)) {
            sub.setStatus("submitted");
            sub.setFeedback("系统已自动评阅客观题，主观题/编程题待教师复核");
        } else {
            sub.setStatus("graded");
            sub.setFeedback("系统已自动评阅");
        }
    }

    @Override
    public int autoScoreChoices(TaskSubmission sub) {
        int autoScore = 0;
        try {
            ObjectMapper mapper = new ObjectMapper();
            var answers = mapper.readValue(sub.getContent(), List.class);
            for (Object a : answers) {
                Map<String, Object> ans = (Map<String, Object>) a;
                Question q = questionService.getById(String.valueOf(ans.get("no")));
                if (q != null && isAutoGradable(q) && isAnswerCorrect(q, ans.get("response"))) {
                    autoScore += q.getScore() != null ? q.getScore() : 0;
                }
            }
        } catch (Exception ignored) {}
        return autoScore;
    }

    private boolean containsManualQuestions(TaskSubmission sub) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            var answers = mapper.readValue(sub.getContent(), List.class);
            for (Object a : answers) {
                Map<String, Object> ans = (Map<String, Object>) a;
                Question q = questionService.getById(String.valueOf(ans.get("no")));
                if (q != null && !isAutoGradable(q)) return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    private boolean isAutoGradable(Question q) {
        return "single".equals(q.getType()) || "multi".equals(q.getType()) || "fill".equals(q.getType());
    }

    private boolean isAnswerCorrect(Question q, Object response) {
        if (q.getAnswer() == null) return false;
        String studentAnswer = response == null ? "" : String.valueOf(response).trim();
        String correctAnswer = q.getAnswer().trim();
        if ("multi".equals(q.getType())) {
            return splitAnswerSet(correctAnswer).equals(splitAnswerSet(studentAnswer));
        }
        return correctAnswer.equalsIgnoreCase(studentAnswer);
    }

    private Set<String> splitAnswerSet(String answer) {
        return Arrays.stream(answer.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(TreeSet::new));
    }
}
