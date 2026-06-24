package com.neu.CoursePlatform.profile.service;

import com.neu.CoursePlatform.profile.entity.Recommendation;
import java.util.List;

public interface RecommendationService {
    List<Recommendation> generateRecommendations(Integer studentNo, Integer courseCode);
    List<Recommendation> getRecommendations(Integer studentNo, Integer courseCode);
    void recordFeedback(Integer recommendationId, String feedback);
}
