package com.neu.CoursePlatform.profile.mock;

import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class MockAgenticClient {

    /** Mock 推荐理由生成（对齐 agentic /recommend 契约） */
    public String generateRecommendReason(String targetName, String type, int score) {
        Map<String, String> reasons = Map.of(
            "knowledge_point", "根据你最近的学习表现，" + targetName + "是需要重点巩固的内容",
            "review_material", targetName + "的正确率低于60%，建议复习相关基础内容",
            "extended_material", targetName + "掌握得很好，可以挑战进阶内容了",
            "practice", targetName + "建议通过专项练习巩固薄弱环节"
        );
        return reasons.getOrDefault(type, "系统为你推荐: " + targetName);
    }

    /** Mock 学习反馈生成（对齐 agentic /chat 契约） */
    public String generateFeedback(Map<String, Object> profileData) {
        int hp = (int) profileData.getOrDefault("hp", 80);
        if (hp < 40) return "你的信心值偏低，建议先休息或复习基础内容再继续挑战。";
        if (hp < 70) return "状态不错，继续保持！薄弱知识点建议优先复习。";
        return "表现优秀！可以考虑挑战进阶内容或拓展材料。";
    }
}
