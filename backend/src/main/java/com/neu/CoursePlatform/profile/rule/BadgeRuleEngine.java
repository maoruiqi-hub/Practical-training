package com.neu.CoursePlatform.profile.rule;

import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class BadgeRuleEngine {

    public record BadgeCheck(boolean earned, String code, String name, String description) {}

    public List<BadgeCheck> checkAll(int totalCorrect, int consecutiveCorrect,
                                      boolean timedComplete, boolean fullScore,
                                      int nightSessions, int helpfulFeedback,
                                      int selfCorrections, int pythonicStyleCount,
                                      List<String> existingBadges) {
        List<BadgeCheck> results = new ArrayList<>();
        Set<String> earned = new HashSet<>(existingBadges);

        if (!has(earned, "combo_10", "连击王") && consecutiveCorrect >= 10)
            results.add(new BadgeCheck(true, "combo_10", "连击王", "连续答对10题"));
        if (!has(earned, "perfect_score", "完美主义") && fullScore)
            results.add(new BadgeCheck(true, "perfect_score", "完美主义", "单次测验满分"));
        if (!has(earned, "timed_complete", "速通者") && timedComplete)
            results.add(new BadgeCheck(true, "timed_complete", "速通者", "限时内完成测验"));
        if (!has(earned, "pythonic_3", "Pythonic") && pythonicStyleCount >= 3)
            results.add(new BadgeCheck(true, "pythonic_3", "Pythonic", "使用列表推导式解题≥3次"));
        if (!has(earned, "self_correction_5", "Debug之眼") && selfCorrections >= 5)
            results.add(new BadgeCheck(true, "self_correction_5", "Debug之眼", "自行修正错误5次"));
        if (!has(earned, "night_5", "夜枭") && nightSessions >= 5)
            results.add(new BadgeCheck(true, "night_5", "夜枭", "非上课时段完成5题"));
        if (!has(earned, "helpful_3", "助人者") && helpfulFeedback >= 3)
            results.add(new BadgeCheck(true, "helpful_3", "助人者", "有价值的提问3次"));

        return results;
    }

    private boolean has(Set<String> earned, String code, String name) {
        return earned.contains(code) || earned.contains(name);
    }
}
