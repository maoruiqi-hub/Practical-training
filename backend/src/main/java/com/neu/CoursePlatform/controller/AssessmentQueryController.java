package com.neu.CoursePlatform.controller;

import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.dto.MistakeStatsDTO;
import com.neu.CoursePlatform.dto.ScoreRecordDTO;
import com.neu.CoursePlatform.entity.Exam;
import com.neu.CoursePlatform.entity.Student;
import com.neu.CoursePlatform.entity.SubmissionAnswer;
import com.neu.CoursePlatform.service.AssessmentQueryService;
import com.neu.CoursePlatform.service.ExamService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Read APIs consumed later by Module 4/5 and the tower-game client. */
@RestController
@RequestMapping("/api")
public class AssessmentQueryController {

    private final AssessmentQueryService assessmentQueryService;
    private final ExamService examService;
    private final Auth auth;

    public AssessmentQueryController(AssessmentQueryService assessmentQueryService,
                                     ExamService examService,
                                     Auth auth) {
        this.assessmentQueryService = assessmentQueryService;
        this.examService = examService;
        this.auth = auth;
    }

    @GetMapping("/students/{studentNo}/scores")
    public Result<List<ScoreRecordDTO>> studentScores(@PathVariable String studentNo,
                                                       @RequestParam(name = "course_id") String courseCode,
                                                       HttpSession session) {
        if (!canViewStudentCourse(studentNo, courseCode, session)) return Result.fail("无权查看成绩数据");
        return Result.ok(assessmentQueryService.getStudentScores(studentNo, courseCode));
    }

    @GetMapping("/students/{studentNo}/mistakes")
    public Result<List<SubmissionAnswer>> studentMistakes(@PathVariable String studentNo,
                                                           @RequestParam(name = "course_id") String courseCode,
                                                           @RequestParam(name = "knowledge_point_id", required = false) String knowledgePointId,
                                                           HttpSession session) {
        if (!canViewStudentCourse(studentNo, courseCode, session)) return Result.fail("无权查看错题数据");
        return Result.ok(assessmentQueryService.getStudentMistakes(studentNo, courseCode, knowledgePointId));
    }

    @GetMapping("/courses/{courseCode}/mistake-stats")
    public Result<List<MistakeStatsDTO>> courseMistakeStats(@PathVariable String courseCode, HttpSession session) {
        if (!auth.canModifyCourse(session, courseCode)) return Result.fail("无权查看课程错题统计");
        return Result.ok(assessmentQueryService.getCourseMistakeStats(courseCode));
    }

    @GetMapping("/exams/{examId}/scores")
    public Result<List<ScoreRecordDTO>> examScores(@PathVariable String examId, HttpSession session) {
        Exam exam = examService.getById(examId);
        if (exam == null) return Result.fail("试卷不存在");
        if (!auth.canModifyCourse(session, exam.getCourseCode())) return Result.fail("无权查看试卷成绩");
        return Result.ok(assessmentQueryService.getExamScores(examId));
    }

    private boolean canViewStudentCourse(String studentNo, String courseCode, HttpSession session) {
        Student student = auth.getStudent(session);
        if (student != null) return studentNo.equals(student.getStudentNo());
        return auth.canModifyCourse(session, courseCode);
    }
}
