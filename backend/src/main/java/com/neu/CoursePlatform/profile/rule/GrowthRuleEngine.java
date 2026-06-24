package com.neu.CoursePlatform.profile.rule;

import org.springframework.stereotype.Component;

@Component
public class GrowthRuleEngine {

    public int calcExpGain(String taskType, boolean correct) {
        if (correct) {
            return switch (taskType) {
                case "quiz" -> 40;
                case "boss" -> 150;
                default -> 10;
            };
        }
        return 0;
    }

    public int calcCoinGain(String taskType, boolean correct) {
        if (correct) {
            return switch (taskType) {
                case "quiz" -> 80;
                case "boss" -> 300;
                default -> 20;
            };
        }
        return 0;
    }

    public int calcLevel(int exp) {
        if (exp >= 2000) return 5;  // 精通
        if (exp >= 1000) return 4;  // 熟练
        if (exp >= 500) return 3;   // 中级
        if (exp >= 200) return 2;   // 初级
        return 1;                    // 入门
    }

    public String getLevelName(int level) {
        return switch (level) {
            case 5 -> "精通";
            case 4 -> "熟练";
            case 3 -> "中级";
            case 2 -> "初级";
            default -> "入门";
        };
    }
}
