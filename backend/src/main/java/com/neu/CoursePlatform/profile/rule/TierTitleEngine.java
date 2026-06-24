package com.neu.CoursePlatform.profile.rule;

import org.springframework.stereotype.Component;

@Component
public class TierTitleEngine {

    public String getTitle(int level, int consecutiveCorrect, int badgeCount) {
        if (level >= 4 && badgeCount >= 5) return "塔之征服者";
        if (level >= 3 && badgeCount >= 3) return "知识探险家";
        if (level >= 2 && badgeCount >= 1) return "编程学徒";
        if (consecutiveCorrect >= 5) return "连击新星";
        return "初入塔境";
    }

    public String getNextTitleHint(int level, int badgeCount) {
        if (level < 2) return "获得第1个徽章即可晋升为'编程学徒'";
        if (level < 3) return "等级达到中级并拥有3个徽章可晋升为'知识探险家'";
        if (level < 4) return "等级达到熟练并拥有5个徽章可晋升为'塔之征服者'";
        return "已是最高称号！";
    }
}
