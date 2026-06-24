package com.neu.CoursePlatform.profile.rule;

import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class BadgeRuleEngine {

    public record BadgeCheck(boolean earned, String name, String description) {}

    public List<BadgeCheck> checkAll(int totalCorrect, int consecutiveCorrect,
                                      boolean timedComplete, boolean fullScore,
                                      int nightSessions, int helpfulFeedback,
                                      int selfCorrections, int pythonicStyleCount,
                                      List<String> existingBadges) {
        List<BadgeCheck> results = new ArrayList<>();
        String earned = String.join(",", existingBadges);

        if (!earned.contains("连击王") && consecutiveCorrect >= 10)
            results.add(new BadgeCheck(true, "连击王", "连续答对10题"));
        if (!earned.contains("完美主义") && fullScore)
            results.add(new BadgeCheck(true, "完美主义", "单次测验满分"));
        if (!earned.contains("速通者") && timedComplete)
            results.add(new BadgeCheck(true, "速通者", "限时内完成测验"));
        if (!earned.contains("Pythonic") && pythonicStyleCount >= 3)
            results.add(new BadgeCheck(true, "Pythonic", "使用列表推导式解题≥3次"));
        if (!earned.contains("Debug之眼") && selfCorrections >= 5)
            results.add(new BadgeCheck(true, "Debug之眼", "自行修正错误5次"));
        if (!earned.contains("夜枭") && nightSessions >= 5)
            results.add(new BadgeCheck(true, "夜枭", "非上课时段完成5题"));
        if (!earned.contains("助人者") && helpfulFeedback >= 3)
            results.add(new BadgeCheck(true, "助人者", "有价值的提问3次"));

        return results;
    }
}
