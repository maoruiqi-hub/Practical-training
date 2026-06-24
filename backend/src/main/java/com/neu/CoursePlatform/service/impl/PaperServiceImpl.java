package com.neu.CoursePlatform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neu.CoursePlatform.dto.PaperGenerateRequest;
import com.neu.CoursePlatform.dto.PaperGenerateResult;
import com.neu.CoursePlatform.entity.Paper;
import com.neu.CoursePlatform.entity.PaperQuestion;
import com.neu.CoursePlatform.entity.Question;
import com.neu.CoursePlatform.mapper.PaperMapper;
import com.neu.CoursePlatform.service.PaperQuestionService;
import com.neu.CoursePlatform.service.PaperService;
import com.neu.CoursePlatform.service.QuestionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class PaperServiceImpl extends ServiceImpl<PaperMapper, Paper> implements PaperService {

    private final QuestionService questionService;
    private final PaperQuestionService paperQuestionService;

    public PaperServiceImpl(QuestionService questionService, PaperQuestionService paperQuestionService) {
        this.questionService = questionService;
        this.paperQuestionService = paperQuestionService;
    }

    @Override
    @Transactional
    public PaperGenerateResult generateAndSave(String courseCode, PaperGenerateRequest request) {
        List<Question> questions = questionService.generatePaper(courseCode, request);
        Paper paper = new Paper();
        paper.setCourseCode(courseCode);
        paper.setTitle(buildTitle(request));
        paper.setStrategy(request.getStrategy() == null || request.getStrategy().isBlank() ? "random" : request.getStrategy());
        paper.setTargetCount(questions.size());
        paper.setTotalScore(questions.stream().mapToInt(q -> q.getScore() == null ? 0 : q.getScore()).sum());
        paper.setStatus("draft");
        paper.setCreateTime(LocalDateTime.now());
        save(paper);

        List<PaperQuestion> snapshots = new ArrayList<>();
        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            PaperQuestion pq = new PaperQuestion();
            pq.setPaperId(paper.getPaperId());
            pq.setQuestionId(q.getQuestionId());
            pq.setSortOrder(i + 1);
            pq.setScoreSnapshot(q.getScore());
            pq.setQuestionType(q.getType());
            pq.setKnowledgePointId(q.getKnowledgePointId());
            pq.setDifficulty(q.getDifficulty());
            snapshots.add(pq);
        }
        paperQuestionService.saveBatch(snapshots);
        return new PaperGenerateResult(paper, questions);
    }

    @Override
    public void bindToTask(String paperId, String taskNo) {
        Paper paper = getById(paperId);
        if (paper == null) throw new IllegalArgumentException("试卷版本不存在");
        paper.setTaskNo(taskNo);
        paper.setStatus("published");
        updateById(paper);
    }

    private String buildTitle(PaperGenerateRequest request) {
        String strategy = request.getStrategy() == null || request.getStrategy().isBlank() ? "random" : request.getStrategy();
        return switch (strategy) {
            case "knowledge" -> "按知识点组卷";
            case "difficulty" -> "难度平衡组卷";
            default -> "随机组卷";
        };
    }
}
