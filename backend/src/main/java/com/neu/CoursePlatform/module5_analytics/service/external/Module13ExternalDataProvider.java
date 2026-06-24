package com.neu.CoursePlatform.module5_analytics.service.external;

import com.neu.CoursePlatform.entity.KnowledgePoint;
import com.neu.CoursePlatform.entity.Student;
import com.neu.CoursePlatform.module5_analytics.dto.external.KnowledgePointDTO;
import com.neu.CoursePlatform.module5_analytics.dto.external.MistakeStatsDTO;
import com.neu.CoursePlatform.module5_analytics.dto.external.StudentProgressDTO;
import com.neu.CoursePlatform.module5_analytics.dto.external.StudentScoreDTO;
import com.neu.CoursePlatform.service.KnowledgePointService;
import com.neu.CoursePlatform.service.AssessmentDataService;
import com.neu.CoursePlatform.service.LearningTaskService;
import com.neu.CoursePlatform.service.StudentService;
import com.neu.CoursePlatform.service.TaskSubmissionService;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 模块五消费模块一、三公开服务的真实适配器。
 * 不直接读取其他模块的 Mapper/数据表，保留模块边界。
 */
@Primary
@Service
public class Module13ExternalDataProvider implements ExternalDataProvider {
    private final AssessmentDataService assessmentDataService;
    private final TaskSubmissionService submissionService;
    private final LearningTaskService taskService;
    private final StudentService studentService;
    private final KnowledgePointService knowledgePointService;

    public Module13ExternalDataProvider(AssessmentDataService assessmentDataService, TaskSubmissionService submissionService,
                                        LearningTaskService taskService,
                                        StudentService studentService,
                                        KnowledgePointService knowledgePointService) {
        this.assessmentDataService = assessmentDataService;
        this.submissionService = submissionService;
        this.taskService = taskService;
        this.studentService = studentService;
        this.knowledgePointService = knowledgePointService;
    }

    @Override
    public List<StudentScoreDTO> getStudentScores(String studentId, String courseId) {
        return assessmentDataService.getStudentScores(studentId, courseId).stream().map(record -> {
            StudentScoreDTO dto = new StudentScoreDTO();
            dto.setStudentId(record.studentId()); dto.setStudentName(record.studentName()); dto.setCourseId(record.courseId());
            dto.setTargetId(record.targetId()); dto.setTargetType(record.targetType()); dto.setScore(record.score());
            dto.setTotalScore(record.totalScore()); dto.setScoredAt(record.scoredAt()); return dto;
        }).toList();
    }

    @Override
    public List<MistakeStatsDTO> getClassMistakeStats(String courseId) {
        return assessmentDataService.getClassMistakeStats(courseId).stream().map(record -> {
            MistakeStatsDTO dto = new MistakeStatsDTO();
            dto.setKnowledgePointId(record.knowledgePointId()); dto.setKnowledgePointName(record.knowledgePointName());
            dto.setTotalAttempts(record.totalAttempts()); dto.setMistakeCount(record.mistakeCount()); dto.setMistakeRate(record.mistakeRate());
            return dto;
        }).toList();
    }

    @Override
    public StudentProgressDTO getStudentProgress(String studentId, String courseId) {
        StudentProgressDTO result = new StudentProgressDTO();
        result.setStudentId(studentId);
        Student student = studentService.getById(studentId);
        result.setStudentName(student == null ? "" : student.getName());
        int total = taskService.listByCourseCode(courseId).size();
        int submitted = (int) submissionService.listByStudentNo(studentId).stream()
                .filter(item -> courseId.equals(submissionService.getTaskCourseCode(item.getTaskNo()))).count();
        result.setTotalTasks(total);
        result.setSubmittedTasks(submitted);
        result.setCompletedTasks(submitted);
        result.setCompletionRate(total == 0 ? 0D : submitted * 1.0D / total);
        return result;
    }

    @Override public List<StudentProgressDTO> getClassProgressList(String courseId) { return List.of(); }

    @Override
    public List<KnowledgePointDTO> getKnowledgePointsByCourse(String courseId) {
        return knowledgePointService.listByCourseCode(courseId, null).stream().map(point -> {
            KnowledgePointDTO dto = new KnowledgePointDTO();
            dto.setId(point.getKnowledgePointId());
            dto.setName(point.getName());
            dto.setCourseId(courseId);
            dto.setLevel(point.getImportance() == null ? 1 : point.getImportance());
            return dto;
        }).toList();
    }

    @Override public List<String> getStudentIdsByClass(String classId) { return List.of(); }
    @Override public LocalDateTime getLastActiveTime(String studentId) { return null; }

}
