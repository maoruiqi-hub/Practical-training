package com.neu.CoursePlatform.controller;

import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.entity.KnowledgePoint;
import com.neu.CoursePlatform.entity.Question;
import com.neu.CoursePlatform.service.KnowledgePointService;
import com.neu.CoursePlatform.service.QuestionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/questions")
public class QuestionImportController {

    private final QuestionService questionService;
    private final KnowledgePointService knowledgePointService;
    private final Auth auth;

    public QuestionImportController(QuestionService questionService, KnowledgePointService knowledgePointService, Auth auth) {
        this.questionService = questionService;
        this.knowledgePointService = knowledgePointService;
        this.auth = auth;
    }

    @PostMapping("/import")
    public Result<Integer> importQuestions(@RequestBody List<Question> questions, HttpSession session) {
        if (questions == null || questions.isEmpty()) return Result.fail("题目列表不能为空");
        String courseCode = questions.get(0).getCourseCode();
        if (courseCode == null || courseCode.isBlank() || !auth.canModifyCourse(session, courseCode)) {
            return Result.fail("无权导入该课程题目");
        }
        for (Question question : questions) {
            if (question == null || !courseCode.equals(question.getCourseCode())
                    || question.getStem() == null || question.getStem().isBlank()) {
                return Result.fail("导入题目必须属于同一课程且题干不能为空");
            }
            if (question.getDifficulty() != null && (question.getDifficulty() < 1 || question.getDifficulty() > 5)) {
                return Result.fail("题目难度必须在 1 到 5 之间");
            }
            if (question.getKnowledgePointId() != null && !question.getKnowledgePointId().isBlank()) {
                KnowledgePoint point = knowledgePointService.getById(question.getKnowledgePointId());
                if (point == null || !courseCode.equals(point.getCourseCode())) {
                    return Result.fail("题目关联的知识点不存在或不属于当前课程");
                }
            }
        }
        questions.forEach(question -> question.setQuestionId(null));
        questionService.saveBatch(questions);
        return Result.ok(questions.size());
    }
}
