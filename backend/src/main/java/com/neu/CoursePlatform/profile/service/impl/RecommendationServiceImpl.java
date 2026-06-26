package com.neu.CoursePlatform.profile.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.neu.CoursePlatform.profile.entity.*;
import com.neu.CoursePlatform.profile.mapper.RecommendationMapper;
import com.neu.CoursePlatform.profile.mock.MockAgenticClient;
import com.neu.CoursePlatform.profile.service.ProfileService;
import com.neu.CoursePlatform.profile.service.RecommendationService;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class RecommendationServiceImpl implements RecommendationService {

    private final RecommendationMapper recommendationMapper;
    private final ProfileService profileService;
    private final MockAgenticClient mockAgentic;

    public RecommendationServiceImpl(RecommendationMapper recommendationMapper,
                                    ProfileService profileService,
                                    MockAgenticClient mockAgentic) {
        this.recommendationMapper = recommendationMapper;
        this.profileService = profileService;
        this.mockAgentic = mockAgentic;
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
                rec.setReason(mockAgentic.generateRecommendReason(cs.getAbilityPointName(), "review_material", cs.getScore()));
            } else if (cs.getScore() < 60) {
                rec.setType("practice");
                rec.setPriority(2);
                rec.setReason(mockAgentic.generateRecommendReason(cs.getAbilityPointName(), "practice", cs.getScore()));
            } else if (cs.getScore() >= 80) {
                rec.setType("extended_material");
                rec.setPriority(3);
                rec.setReason(mockAgentic.generateRecommendReason(cs.getAbilityPointName(), "extended_material", cs.getScore()));
            } else {
                rec.setType("knowledge_point");
                rec.setPriority(2);
                rec.setReason(mockAgentic.generateRecommendReason(cs.getAbilityPointName(), "knowledge_point", cs.getScore()));
            }

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
}
