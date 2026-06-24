package com.neu.CoursePlatform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neu.CoursePlatform.dto.ExamGenerateRequest;
import com.neu.CoursePlatform.dto.ExamGenerateResult;
import com.neu.CoursePlatform.entity.Exam;
import com.neu.CoursePlatform.entity.ExamQuestion;
import com.neu.CoursePlatform.entity.Question;
import com.neu.CoursePlatform.mapper.ExamMapper;
import com.neu.CoursePlatform.service.ExamQuestionService;
import com.neu.CoursePlatform.service.ExamService;
import com.neu.CoursePlatform.service.QuestionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExamServiceImpl extends ServiceImpl<ExamMapper, Exam> implements ExamService {

    private final QuestionService questionService;
    private final ExamQuestionService examQuestionService;

    public ExamServiceImpl(QuestionService questionService, ExamQuestionService examQuestionService) {
        this.questionService = questionService;
        this.examQuestionService = examQuestionService;
    }

    @Override
    @Transactional
    public ExamGenerateResult generateAndSave(String courseCode, ExamGenerateRequest request) {
        List<Question> questions = questionService.generateExam(courseCode, request);
        Exam exam = new Exam();
        exam.setCourseCode(courseCode);
        exam.setTitle(buildTitle(request));
        exam.setGenerateType(request.getStrategy() == null || request.getStrategy().isBlank() ? "random" : request.getStrategy());
        exam.setTargetCount(questions.size());
        exam.setTotalScore(questions.stream().mapToInt(q -> q.getScore() == null ? 0 : q.getScore()).sum());
        exam.setStatus("draft");
        exam.setCreateTime(LocalDateTime.now());
        save(exam);

        List<ExamQuestion> snapshots = new ArrayList<>();
        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            ExamQuestion examQuestion = new ExamQuestion();
            examQuestion.setExamId(exam.getExamId());
            examQuestion.setQuestionId(q.getQuestionId());
            examQuestion.setSortOrder(i + 1);
            examQuestion.setScoreSnapshot(q.getScore());
            examQuestion.setQuestionType(q.getType());
            examQuestion.setKnowledgePointId(q.getKnowledgePointId());
            examQuestion.setDifficulty(q.getDifficulty());
            snapshots.add(examQuestion);
        }
        examQuestionService.saveBatch(snapshots);
        return new ExamGenerateResult(exam, questions);
    }

    @Override
    public void bindToTask(String examId, String taskNo) {
        Exam exam = getById(examId);
        if (exam == null) throw new IllegalArgumentException("试卷版本不存在");
        exam.setTaskNo(taskNo);
        exam.setStatus("published");
        updateById(exam);
    }

    private String buildTitle(ExamGenerateRequest request) {
        String strategy = request.getStrategy() == null || request.getStrategy().isBlank() ? "random" : request.getStrategy();
        return switch (strategy) {
            case "knowledge" -> "按知识点组卷";
            case "difficulty" -> "难度平衡组卷";
            default -> "随机组卷";
        };
    }
}
