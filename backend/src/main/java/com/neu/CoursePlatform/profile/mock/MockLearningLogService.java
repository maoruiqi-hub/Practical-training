package com.neu.CoursePlatform.profile.mock;

import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import java.util.*;

@Service
@Profile("mock")
public class MockLearningLogService {

    /** Mock 学生行为日志（对齐模块2 getStudentLogs 契约） */
    public List<Map<String, Object>> getStudentLogs(Integer studentNo, Integer courseCode) {
        return List.of(
            Map.of("actionType", "answer", "resourceType", "quiz",
                   "durationMs", 300000, "timestamp", new Date(),
                   "detail", "完成选择题练习"),
            Map.of("actionType", "answer", "resourceType", "quiz",
                   "durationMs", 180000, "timestamp", new Date(),
                   "detail", "完成填空题练习"),
            Map.of("actionType", "video_play", "resourceType", "video",
                   "durationMs", 600000, "timestamp", new Date(),
                   "detail", "观看Python基础视频"),
            Map.of("actionType", "code_submit", "resourceType", "program",
                   "durationMs", 900000, "timestamp", new Date(),
                   "detail", "提交编程作业")
        );
    }
}
