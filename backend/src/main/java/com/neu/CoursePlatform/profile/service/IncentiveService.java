package com.neu.CoursePlatform.profile.service;

import com.neu.CoursePlatform.profile.entity.Achievement;
import java.util.*;

public interface IncentiveService {
    List<Achievement> checkAndAwardBadges(Integer studentNo, Integer courseCode,
            int totalCorrect, int consecutiveCorrect, boolean timedComplete,
            boolean fullScore, int nightSessions, int helpfulFeedback,
            int selfCorrections, int pythonicStyleCount);
    List<Achievement> getAchievements(Integer studentNo, Integer courseCode);
    List<Map<String, Object>> getLeaderboard(Integer courseCode, String type);
    String getTitle(Integer studentNo, Integer courseCode);
}
