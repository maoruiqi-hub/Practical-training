package com.neu.CoursePlatform.profile.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.neu.CoursePlatform.agentic.AgenticClient;
import com.neu.CoursePlatform.agentic.AgenticRequest;
import com.neu.CoursePlatform.agentic.AgenticResponse;
import com.neu.CoursePlatform.profile.entity.*;
import com.neu.CoursePlatform.profile.mapper.RecommendationMapper;
import com.neu.CoursePlatform.profile.service.ProfileService;
import com.neu.CoursePlatform.profile.service.RecommendationService;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class RecommendationServiceImpl implements RecommendationService {

    private final RecommendationMapper recommendationMapper;
    private final ProfileService profileService;
    private final AgenticClient agenticClient;

    public RecommendationServiceImpl(RecommendationMapper recommendationMapper,
                                    ProfileService profileService,
                                    AgenticClient agenticClient) {
        this.recommendationMapper = recommendationMapper;
        this.profileService = profileService;
        this.agenticClient = agenticClient;
    }

    @Override
    public List<Recommendation> generateRecommendations(Integer studentNo, Integer courseCode) {
        // 清除旧推荐
        LambdaQueryWrapper<Recommendation> delQ = new LambdaQueryWrapper<>();
        delQ.eq(Recommendation::getStudentNo, studentNo)
            .eq(Recommendation::getCourseCode, courseCode);
        recommendationMapper.delete(delQ);

        List<CompetencyScore> scores = profileService.getCompetencyScores(studentNo, courseCode);
        List<Recommendation> newRecs = new ArrayList<>();

        for (CompetencyScore cs : scores) {
            Recommendation rec = new Recommendation();
            rec.setStudentNo(studentNo);
            rec.setCourseCode(courseCode);
            rec.setTargetId(cs.getAbilityPointId());
            rec.setTargetName(cs.getAbilityPointName());

            if (cs.getScore() < 40) {
                rec.setType("review_material");
                rec.setPriority(1);
            } else if (cs.getScore() < 60) {
                rec.setType("practice");
                rec.setPriority(2);
            } else if (cs.getScore() >= 80) {
                rec.setType("extended_material");
                rec.setPriority(3);
            } else {
                rec.setType("knowledge_point");
                rec.setPriority(2);
            }
            rec.setReason(generateReason(studentNo, courseCode, cs, rec.getType()));

            rec.setCreatedAt(new Date());
            recommendationMapper.insert(rec);
            newRecs.add(rec);
        }

        return newRecs;
    }

    @Override
    public List<Recommendation> getRecommendations(Integer studentNo, Integer courseCode) {
        LambdaQueryWrapper<Recommendation> q = new LambdaQueryWrapper<>();
        q.eq(Recommendation::getStudentNo, studentNo)
         .eq(Recommendation::getCourseCode, courseCode)
         .orderByAsc(Recommendation::getPriority);
        return recommendationMapper.selectList(q);
    }

    @Override
    public void recordFeedback(String recommendationId, String feedback) {
        Recommendation rec = recommendationMapper.selectById(recommendationId);
        if (rec != null) {
            rec.setFeedback(feedback);
            recommendationMapper.updateById(rec);
        }
    }

    private String generateReason(Integer studentNo, Integer courseCode, CompetencyScore score, String type) {
        AgenticRequest request = new AgenticRequest();
        request.setCourseCode(String.valueOf(courseCode));
        request.setContext(Map.of(
                "student_id", String.valueOf(studentNo),
                "ability_point_id", score.getAbilityPointId(),
                "ability_point_name", score.getAbilityPointName(),
                "score", score.getScore(),
                "recommendation_type", type
        ));
        AgenticResponse response = agenticClient.invoke("recommend", request);
        if (response != null && response.isSuccess() && response.getData() != null) {
            Object reason = response.getData().get("reason");
            if (reason != null && !String.valueOf(reason).isBlank()) return String.valueOf(reason);
            Object text = response.getData().get("message");
            if (text != null && !String.valueOf(text).isBlank()) return String.valueOf(text);
        }
        return fallbackReason(score.getAbilityPointName(), type, score.getScore());
    }

    private String fallbackReason(String abilityPointName, String type, Integer score) {
        String name = abilityPointName == null || abilityPointName.isBlank() ? "相关能力点" : abilityPointName;
        int value = score == null ? 0 : score;
        return switch (type) {
            case "review_material" -> name + " 当前得分 " + value + "，建议优先复习基础材料。";
            case "practice" -> name + " 当前得分 " + value + "，建议通过专项练习巩固。";
            case "extended_material" -> name + " 掌握较好，可学习拓展材料继续提升。";
            default -> name + " 当前得分 " + value + "，建议回到知识点页面查漏补缺。";
        };
    }
}
