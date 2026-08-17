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
        List<Map<String, Object>> recommendationInputs = new ArrayList<>();
        Map<String, Integer> scoreByTarget = new LinkedHashMap<>();

        // 先按分数决定 type/priority，收集批量推荐对象，理由稍后一次性生成。
        for (CompetencyScore cs : scores) {
            Recommendation rec = buildRecommendation(studentNo, courseCode, cs);
            newRecs.add(rec);
            scoreByTarget.put(cs.getAbilityPointId(), cs.getScore());

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("targetId", cs.getAbilityPointId());
            item.put("targetName", cs.getAbilityPointName());
            item.put("score", cs.getScore());
            item.put("type", rec.getType());
            item.put("priority", rec.getPriority());
            recommendationInputs.add(item);
        }

        // 批量生成理由，按 targetId 回填；缺失走本地模板兜底。
        Map<String, String> reasons = generateReasons(studentNo, courseCode, recommendationInputs);
        for (Recommendation rec : newRecs) {
            String reason = reasons.get(rec.getTargetId());
            if (reason == null || reason.isBlank()) {
                reason = fallbackReason(rec.getTargetName(), rec.getType(), scoreByTarget.get(rec.getTargetId()));
            }
            rec.setReason(reason);
            recommendationMapper.insert(rec);
        }

        return newRecs;
    }

    private Recommendation buildRecommendation(Integer studentNo, Integer courseCode, CompetencyScore cs) {
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
        rec.setCreatedAt(new Date());
        return rec;
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

    private Map<String, String> generateReasons(Integer studentNo, Integer courseCode,
                                                List<Map<String, Object>> recommendationInputs) {
        if (recommendationInputs == null || recommendationInputs.isEmpty()) return Map.of();
        AgenticRequest request = new AgenticRequest();
        request.setCourseCode(String.valueOf(courseCode));
        request.setContext(Map.of(
                "student", Map.of("anonymousId", String.valueOf(studentNo)),
                "recommendations", recommendationInputs
        ));
        AgenticResponse response = agenticClient.invoke("recommend", request);
        Map<String, String> reasons = new LinkedHashMap<>();
        if (response != null && response.isSuccess() && response.getData() != null) {
            Object rawReasons = response.getData().get("reasons");
            if (rawReasons instanceof List<?> list) {
                for (Object item : list) {
                    if (!(item instanceof Map<?, ?> m)) continue;
                    Object targetId = m.get("targetId");
                    Object reason = m.get("reason");
                    if (targetId != null && reason != null && !String.valueOf(reason).isBlank()) {
                        reasons.put(String.valueOf(targetId), String.valueOf(reason));
                    }
                }
            }
        }
        return reasons;
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
