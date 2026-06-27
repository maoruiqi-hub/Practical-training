package com.neu.CoursePlatform.module5_analytics.service;

import com.neu.CoursePlatform.module5_analytics.dto.external.StudentProgressDTO;
import com.neu.CoursePlatform.module5_analytics.dto.external.StudentScoreDTO;
import com.neu.CoursePlatform.module5_analytics.entity.RiskAlert;
import com.neu.CoursePlatform.module5_analytics.service.external.ExternalDataProvider;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 风险检测引擎 — 确定性规则，7类风险（T6, R4.1-R4.7）
 */
@Service
public class RiskDetectionService {

    private final ExternalDataProvider dataProvider;
    private final RiskAlertService riskAlertService;

    /** 不活跃阈值（天） */
    private static final int INACTIVE_MEDIUM_DAYS = 3;
    private static final int INACTIVE_HIGH_DAYS = 7;
    /** 低分阈值 */
    private static final double LOW_SCORE_LINE = 60.0;
    /** 连续低分次数 */
    private static final int CONSECUTIVE_LOW_COUNT = 3;
    /** 进度落后阈值 */
    private static final double PROGRESS_LAG_RATE = 0.30;

    public RiskDetectionService(@Lazy ExternalDataProvider dataProvider,
                                 RiskAlertService riskAlertService) {
        this.dataProvider = dataProvider;
        this.riskAlertService = riskAlertService;
    }

    /**
     * 对全班学生执行风险检测（R4.2）
     */
    public List<RiskAlert> detectForClass(String classId, String courseId) {
        List<String> studentIds = dataProvider.getStudentIdsByClass(classId);
        // 计算班级平均完成率（用于 progress_lag 判定）
        List<StudentProgressDTO> allProgress = dataProvider.getClassProgressList(classId, courseId);
        double avgRate = allProgress.stream()
                .mapToDouble(StudentProgressDTO::getCompletionRate).average().orElse(0);

        List<RiskAlert> alerts = new ArrayList<>();
        for (String sid : studentIds) {
            alerts.addAll(detectForStudent(sid, courseId, avgRate));
        }
        return alerts;
    }

    /**
     * 对单个学生执行全部7类规则检测
     */
    public List<RiskAlert> detectForStudent(String studentId, String courseId, double classAvgRate) {
        List<RiskAlert> alerts = new ArrayList<>();

        // R1: 任务拖延
        checkProcrastination(studentId, courseId).ifPresent(alerts::add);

        // R2: 连续低分
        checkLowScore(studentId, courseId).ifPresent(alerts::add);

        // R3: 成绩下滑
        checkScoreDecline(studentId, courseId).ifPresent(alerts::add);

        // R4: 长期未登录
        checkInactive(studentId, courseId).ifPresent(alerts::add);

        // R5: 进度落后
        checkProgressLag(studentId, courseId, classAvgRate).ifPresent(alerts::add);

        return alerts;
    }

    /** 任务拖延检测 */
    private Optional<RiskAlert> checkProcrastination(String sid, String courseId) {
        // Phase 2 Mock：从外部数据获取任务状态
        // 实际实现需从模块2获取Task+Submission数据
        // 此处用 lastActiveTime 做近似：超过24h未活跃 → 可能拖延
        LocalDateTime lastActive = dataProvider.getLastActiveTime(sid);
        if (lastActive == null) return Optional.empty();

        long hoursSinceActive = java.time.Duration.between(lastActive, LocalDateTime.now()).toHours();
        if (hoursSinceActive > 24 * 7) {
            return createAlert(sid, courseId, "procrastination", "high",
                    "{\"hours_since_active\":" + hoursSinceActive + ",\"reason\":\"超7天未活跃，疑似严重拖延\"}");
        } else if (hoursSinceActive > 24) {
            return createAlert(sid, courseId, "procrastination", "medium",
                    "{\"hours_since_active\":" + hoursSinceActive + ",\"reason\":\"超24小时未活跃\"}");
        }
        return Optional.empty();
    }

    /** 连续低分检测 */
    private Optional<RiskAlert> checkLowScore(String sid, String courseId) {
        List<StudentScoreDTO> scores = dataProvider.getStudentScores(sid, courseId);
        if (scores.size() < CONSECUTIVE_LOW_COUNT) return Optional.empty();

        // 检查最近N次
        List<StudentScoreDTO> recent = scores.subList(
                Math.max(0, scores.size() - CONSECUTIVE_LOW_COUNT), scores.size());
        boolean allLow = recent.stream().allMatch(s -> s.getScore() < LOW_SCORE_LINE);
        if (allLow) {
            double avg = recent.stream().mapToDouble(StudentScoreDTO::getScore).average().orElse(0);
            return createAlert(sid, courseId, "low_score", "high",
                    "{\"recent_avg\":" + String.format("%.1f", avg) + ",\"consecutive_count\":" + CONSECUTIVE_LOW_COUNT + "}");
        }
        return Optional.empty();
    }

    /** 成绩下滑检测 */
    private Optional<RiskAlert> checkScoreDecline(String sid, String courseId) {
        List<StudentScoreDTO> scores = dataProvider.getStudentScores(sid, courseId);
        if (scores.size() < 3) return Optional.empty();

        List<StudentScoreDTO> recent = scores.subList(Math.max(0, scores.size() - 3), scores.size());
        // 检查是否持续下降且最新得分 < 70
        boolean declining = recent.get(0).getScore() > recent.get(1).getScore()
                && recent.get(1).getScore() > recent.get(2).getScore();
        double latest = recent.get(2).getScore();
        if (declining && latest < 70) {
            return createAlert(sid, courseId, "score_decline", "medium",
                    "{\"trend\":[" + recent.get(0).getScore() + "," + recent.get(1).getScore() + "," + recent.get(2).getScore() + "]}");
        }
        return Optional.empty();
    }

    /** 长期未登录检测 */
    private Optional<RiskAlert> checkInactive(String sid, String courseId) {
        LocalDateTime lastActive = dataProvider.getLastActiveTime(sid);
        if (lastActive == null) return Optional.empty();

        long days = java.time.Duration.between(lastActive, LocalDateTime.now()).toDays();
        if (days >= INACTIVE_HIGH_DAYS) {
            return createAlert(sid, courseId, "inactive", "high",
                    "{\"days_since_active\":" + days + "}");
        } else if (days >= INACTIVE_MEDIUM_DAYS) {
            return createAlert(sid, courseId, "inactive", "medium",
                    "{\"days_since_active\":" + days + "}");
        }
        return Optional.empty();
    }

    /** 进度落后检测 */
    private Optional<RiskAlert> checkProgressLag(String sid, String courseId, double classAvgRate) {
        StudentProgressDTO progress = dataProvider.getStudentProgress(sid, courseId);
        if (progress == null) return Optional.empty();

        double gap = classAvgRate - progress.getCompletionRate();
        if (gap > PROGRESS_LAG_RATE) {
            return createAlert(sid, courseId, "progress_lag", "medium",
                    "{\"student_rate\":" + String.format("%.2f", progress.getCompletionRate())
                            + ",\"class_avg\":" + String.format("%.2f", classAvgRate) + "}");
        }
        return Optional.empty();
    }

    /** 创建预警（带去重） */
    private Optional<RiskAlert> createAlert(String sid, String courseId,
                                             String riskType, String riskLevel, String detail) {
        // 同类型活跃预警去重
        if (riskAlertService.hasActiveAlert(sid, riskType)) {
            return Optional.empty();
        }
        RiskAlert alert = riskAlertService.receiveEvent(sid, courseId, riskType, riskLevel, detail);
        return Optional.ofNullable(alert);
    }
}
