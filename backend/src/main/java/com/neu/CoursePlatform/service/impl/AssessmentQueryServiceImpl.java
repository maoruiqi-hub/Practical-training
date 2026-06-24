package com.neu.CoursePlatform.service.impl;

import com.neu.CoursePlatform.dto.MistakeStatsDTO;
import com.neu.CoursePlatform.dto.ScoreRecordDTO;
import com.neu.CoursePlatform.entity.Exam;
import com.neu.CoursePlatform.entity.LearningTask;
import com.neu.CoursePlatform.entity.SubmissionAnswer;
import com.neu.CoursePlatform.entity.TaskSubmission;
import com.neu.CoursePlatform.service.AssessmentQueryService;
import com.neu.CoursePlatform.service.ExamService;
import com.neu.CoursePlatform.service.LearningTaskService;
import com.neu.CoursePlatform.service.SubmissionAnswerService;
import com.neu.CoursePlatform.service.TaskSubmissionService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AssessmentQueryServiceImpl implements AssessmentQueryService {

    private final TaskSubmissionService submissionService;
    private final SubmissionAnswerService answerService;
    private final LearningTaskService taskService;
    private final ExamService examService;

    public AssessmentQueryServiceImpl(TaskSubmissionService submissionService,
                                      SubmissionAnswerService answerService,
                                      LearningTaskService taskService,
                                      ExamService examService) {
        this.submissionService = submissionService;
        this.answerService = answerService;
        this.taskService = taskService;
        this.examService = examService;
    }

    @Override
    public List<ScoreRecordDTO> getStudentScores(String studentNo, String courseCode) {
        List<ScoreRecordDTO> result = new ArrayList<>();
        for (TaskSubmission submission : submissionService.listByStudentNo(studentNo)) {
            LearningTask task = taskService.getById(submission.getTaskNo());
            if (task == null || !courseCode.equals(task.getCourseCode()) || submission.getScore() == null) continue;
            result.add(toScoreRecord(submission, task));
        }
        return result;
    }

    @Override
    public List<SubmissionAnswer> getStudentMistakes(String studentNo, String courseCode, String knowledgePointId) {
        return answerService.listWrongByStudentNo(studentNo).stream()
                .filter(answer -> knowledgePointId == null || knowledgePointId.isBlank()
                        || knowledgePointId.equals(answer.getKnowledgePointId()))
                .filter(answer -> belongsToCourse(answer.getTaskNo(), courseCode))
                .toList();
    }

    @Override
    public List<MistakeStatsDTO> getCourseMistakeStats(String courseCode) {
        Map<String, long[]> aggregates = new LinkedHashMap<>();
        for (LearningTask task : taskService.listByCourseCode(courseCode)) {
            for (SubmissionAnswer answer : answerService.listByTaskNo(task.getTaskNo())) {
                if (!Boolean.TRUE.equals(answer.getAutoGradable())) continue;
                String key = safe(answer.getKnowledgePointId()) + "|" + safe(answer.getQuestionType());
                long[] values = aggregates.computeIfAbsent(key, ignored -> new long[2]);
                values[0]++;
                if (Boolean.FALSE.equals(answer.getCorrect())) values[1]++;
            }
        }
        List<MistakeStatsDTO> result = new ArrayList<>();
        for (Map.Entry<String, long[]> entry : aggregates.entrySet()) {
            String[] key = entry.getKey().split("\\|", -1);
            long attempts = entry.getValue()[0];
            long wrong = entry.getValue()[1];
            result.add(new MistakeStatsDTO(key[0], key[1], attempts, wrong,
                    attempts == 0 ? 0 : wrong * 100.0 / attempts));
        }
        result.sort(java.util.Comparator.comparingDouble(MistakeStatsDTO::getWrongRate).reversed());
        return result;
    }

    @Override
    public List<ScoreRecordDTO> getExamScores(String examId) {
        Exam exam = examService.getById(examId);
        if (exam == null || exam.getTaskNo() == null) return List.of();
        LearningTask task = taskService.getById(exam.getTaskNo());
        if (task == null) return List.of();
        return submissionService.listByTaskNo(exam.getTaskNo()).stream()
                .filter(submission -> submission.getScore() != null)
                .map(submission -> toScoreRecord(submission, task))
                .toList();
    }

    private ScoreRecordDTO toScoreRecord(TaskSubmission submission, LearningTask task) {
        ScoreRecordDTO dto = new ScoreRecordDTO();
        dto.setStudentNo(submission.getStudentNo());
        dto.setTaskNo(submission.getTaskNo());
        dto.setSubmissionId(submission.getSubmissionId());
        dto.setCourseCode(task.getCourseCode());
        dto.setScore(submission.getScore());
        dto.setTotalScore(task.getScore());
        dto.setStatus(submission.getStatus());
        dto.setSubmitTime(submission.getSubmitTime());
        return dto;
    }

    private boolean belongsToCourse(String taskNo, String courseCode) {
        LearningTask task = taskService.getById(taskNo);
        return task != null && courseCode.equals(task.getCourseCode());
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
