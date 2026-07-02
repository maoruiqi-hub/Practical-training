package com.neu.CoursePlatform.service.impl;

import com.neu.CoursePlatform.dto.AssessmentMistakeStat;
import com.neu.CoursePlatform.dto.AssessmentScoreRecord;
import com.neu.CoursePlatform.entity.KnowledgePoint;
import com.neu.CoursePlatform.entity.LearningTask;
import com.neu.CoursePlatform.entity.Student;
import com.neu.CoursePlatform.entity.SubmissionAnswer;
import com.neu.CoursePlatform.entity.TaskSubmission;
import com.neu.CoursePlatform.service.AssessmentDataService;
import com.neu.CoursePlatform.service.KnowledgePointService;
import com.neu.CoursePlatform.service.LearningTaskService;
import com.neu.CoursePlatform.service.StudentService;
import com.neu.CoursePlatform.service.SubmissionAnswerService;
import com.neu.CoursePlatform.service.TaskSubmissionService;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AssessmentDataServiceImpl implements AssessmentDataService {
    private final TaskSubmissionService submissions;
    private final LearningTaskService tasks;
    private final SubmissionAnswerService answers;
    private final StudentService students;
    private final KnowledgePointService knowledgePoints;

    public AssessmentDataServiceImpl(TaskSubmissionService submissions, LearningTaskService tasks,
                                     SubmissionAnswerService answers, StudentService students,
                                     KnowledgePointService knowledgePoints) {
        this.submissions = submissions; this.tasks = tasks; this.answers = answers;
        this.students = students; this.knowledgePoints = knowledgePoints;
    }

    @Override
    public List<AssessmentScoreRecord> getStudentScores(String studentId, String courseId) {
        Student student = students.getById(studentId);
        return submissions.listByStudentNo(studentId).stream().map(submission -> toScore(studentId, student, courseId, submission))
                .filter(java.util.Objects::nonNull).toList();
    }

    @Override
    public List<AssessmentMistakeStat> getClassMistakeStats(String courseId) {
        Map<String, int[]> counts = new LinkedHashMap<>();
        for (LearningTask task : tasks.listByCourseCode(courseId)) for (SubmissionAnswer answer : answers.listByTaskNo(task.getTaskNo())) {
            if (!Boolean.TRUE.equals(answer.getAutoGradable()) || answer.getKnowledgePointId() == null || answer.getKnowledgePointId().isBlank()) continue;
            int[] value = counts.computeIfAbsent(answer.getKnowledgePointId(), ignored -> new int[2]);
            value[0]++; if (!Boolean.TRUE.equals(answer.getCorrect())) value[1]++;
        }
        return counts.entrySet().stream().map(entry -> {
            KnowledgePoint point = knowledgePoints.getById(entry.getKey());
            int[] value = entry.getValue();
            return new AssessmentMistakeStat(entry.getKey(), point == null ? "" : point.getName(), value[0], value[1], value[1] * 1D / value[0]);
        }).sorted(Comparator.comparingDouble(AssessmentMistakeStat::mistakeRate).reversed()).toList();
    }

    private AssessmentScoreRecord toScore(String studentId, Student student, String courseId, TaskSubmission submission) {
        LearningTask task = tasks.getById(submission.getTaskNo());
        if (task == null || !courseId.equals(task.getCourseCode()) || submission.getScore() == null) return null;
        return new AssessmentScoreRecord(studentId, student == null ? "" : student.getName(), courseId,
                submission.getTaskNo(), task.getTaskType(), submission.getScore(), task.getScore() == null ? 0 : task.getScore(), submission.getSubmitTime());
    }
}
