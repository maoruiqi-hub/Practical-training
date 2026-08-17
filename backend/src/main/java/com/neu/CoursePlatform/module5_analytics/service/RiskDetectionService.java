package com.neu.CoursePlatform.module5_analytics.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neu.CoursePlatform.entity.KnowledgeMasteryHistory;
import com.neu.CoursePlatform.mapper.KnowledgeMasteryHistoryMapper;
import com.neu.CoursePlatform.module5_analytics.dto.external.StudentProgressDTO;
import com.neu.CoursePlatform.module5_analytics.dto.external.StudentScoreDTO;
import com.neu.CoursePlatform.module5_analytics.entity.RiskAlert;
import com.neu.CoursePlatform.module5_analytics.service.external.ExternalDataProvider;
import com.neu.CoursePlatform.profile.entity.CompetencyScoreHistory;
import com.neu.CoursePlatform.profile.mapper.CompetencyScoreHistoryMapper;
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
    private final CompetencyScoreHistoryMapper competencyScoreHistoryMapper;
    private final KnowledgeMasteryHistoryMapper knowledgeMasteryHistoryMapper;
    private final ObjectMapper objectMapper;

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
                                 RiskAlertService riskAlertService,
                                 CompetencyScoreHistoryMapper competencyScoreHistoryMapper,
                                 KnowledgeMasteryHistoryMapper knowledgeMasteryHistoryMapper,
                                 ObjectMapper objectMapper) {
        this.dataProvider = dataProvider;
        this.riskAlertService = riskAlertService;
        this.competencyScoreHistoryMapper = competencyScoreHistoryMapper;
        this.knowledgeMasteryHistoryMapper = knowledgeMasteryHistoryMapper;
        this.objectMapper = objectMapper;
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
        RiskContext context = buildRiskContext(studentId, courseId, classAvgRate);

        // R1: 任务拖延
        checkProcrastination(studentId, courseId).ifPresent(alerts::add);

        // R2: 连续低分
        checkLowScore(context).ifPresent(alerts::add);

        // R3: 对个人历史基线的异常波动（替代原先仅比较最近三次的平均趋势）。
        alerts.addAll(detectAnomalies(context));

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
    private Optional<RiskAlert> checkLowScore(RiskContext context) {
        List<StudentScoreDTO> scores = context.scoreRecords();
        if (scores.size() < CONSECUTIVE_LOW_COUNT) return Optional.empty();

        // 检查最近N次
        List<StudentScoreDTO> recent = scores.subList(
                Math.max(0, scores.size() - CONSECUTIVE_LOW_COUNT), scores.size());
        boolean allLow = recent.stream().allMatch(s -> s.getScore() < LOW_SCORE_LINE);
        if (allLow) {
            double avg = recent.stream().mapToDouble(StudentScoreDTO::getScore).average().orElse(0);
            return createAlert(context.studentId(), context.courseId(), "low_score", "high",
                    "{\"recent_avg\":" + String.format("%.1f", avg) + ",\"consecutive_count\":" + CONSECUTIVE_LOW_COUNT + "}");
        }
        return Optional.empty();
    }

    /**
     * Builds the exact chronological sequence used by the student score trend API,
     * then enriches it with mastery histories that are not shown in that chart.
     */
    private RiskContext buildRiskContext(String studentId, String courseId, double classAvgRate) {
        List<StudentScoreDTO> scoreRecords = dataProvider.getStudentScores(studentId, courseId).stream()
                .filter(item -> item.getScore() != null)
                .sorted(Comparator.comparing(StudentScoreDTO::getScoredAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(10)
                .toList();
        List<Double> scoreValues = scoreRecords.stream().map(this::normalizeScore).toList();

        Integer studentNo = parseInteger(studentId);
        Integer courseCode = parseInteger(courseId);
        List<KnowledgeMasteryHistory> masteryHistory = readMasteryHistory(studentId, courseId);
        List<CompetencyScoreHistory> abilityHistory = studentNo == null || courseCode == null ? List.of()
                : competencyScoreHistoryMapper.selectList(new LambdaQueryWrapper<CompetencyScoreHistory>()
                        .eq(CompetencyScoreHistory::getStudentNo, studentNo)
                        .eq(CompetencyScoreHistory::getCourseCode, courseCode)
                        .orderByAsc(CompetencyScoreHistory::getChangedAt));

        masteryHistory = tail(masteryHistory, 10);
        abilityHistory = tail(abilityHistory, 10);
        List<Double> masteryValues = masteryHistory.stream()
                .map(KnowledgeMasteryHistory::getAfterScore).filter(Objects::nonNull)
                .map(Integer::doubleValue).toList();
        List<Double> abilityValues = abilityHistory.stream()
                .map(CompetencyScoreHistory::getNewScore).filter(Objects::nonNull)
                .map(Integer::doubleValue).toList();

        StudentProgressDTO progress = dataProvider.getStudentProgress(studentId, courseId);
        LocalDateTime lastActive = dataProvider.getLastActiveTime(studentId);
        long activityGapDays = lastActive == null ? 0L
                : Math.max(0L, java.time.Duration.between(lastActive, LocalDateTime.now()).toDays());
        return new RiskContext(studentId, courseId, classAvgRate, scoreRecords, masteryHistory, abilityHistory,
                progress, activityGapDays, RiskAnomalyAnalyzer.analyze(scoreValues),
                RiskAnomalyAnalyzer.analyze(masteryValues), RiskAnomalyAnalyzer.analyze(abilityValues));
    }

    private List<RiskAlert> detectAnomalies(RiskContext context) {
        List<RiskAlert> alerts = new ArrayList<>();
        RiskAnomalyAnalyzer.SeriesSignals score = context.scoreSignals();
        RiskAnomalyAnalyzer.SeriesSignals mastery = context.masterySignals();
        RiskAnomalyAnalyzer.SeriesSignals ability = context.abilitySignals();

        if (hasScoreDecline(score)) {
            String level = supportsHighScoreDecline(score) ? "high" : "medium";
            createAlert(context.studentId(), context.courseId(), "score_decline", level,
                    anomalyDetail("score_decline", score, "个人成绩基线出现明显回落")).ifPresent(alerts::add);
        }
        if (hasScoreVolatility(score)) {
            String level = score.evidenceCount() >= 5 && score.volatility() >= 25D && score.directionChanges() >= 2
                    ? "high" : "medium";
            createAlert(context.studentId(), context.courseId(), "score_volatility", level,
                    anomalyDetail("score_volatility", score, "近期成绩上下波动明显")).ifPresent(alerts::add);
        }
        if (hasMasteryDrop(mastery) || hasMasteryDrop(ability)) {
            RiskAnomalyAnalyzer.SeriesSignals source = hasMasteryDrop(mastery) ? mastery : ability;
            String level = source.evidenceCount() >= 2 && source.maxSingleDrop() >= 30D ? "high" : "medium";
            createAlert(context.studentId(), context.courseId(), "mastery_drop", level,
                    anomalyDetail("mastery_drop", source, "掌握度或能力点分数出现回落")).ifPresent(alerts::add);
        }
        return alerts;
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

    private boolean hasScoreDecline(RiskAnomalyAnalyzer.SeriesSignals signals) {
        return signals.evidenceCount() >= 3 && signals.delta() != null
                && signals.delta() <= -20D && signals.latestValue() < 70;
    }

    private boolean supportsHighScoreDecline(RiskAnomalyAnalyzer.SeriesSignals signals) {
        return signals.evidenceCount() >= 4 && signals.delta() != null
                && signals.delta() <= -30D && signals.maxSingleDrop() >= 25D;
    }

    private boolean hasScoreVolatility(RiskAnomalyAnalyzer.SeriesSignals signals) {
        return signals.evidenceCount() >= 4 && signals.volatility() >= 18D && signals.directionChanges() >= 2;
    }

    private boolean hasMasteryDrop(RiskAnomalyAnalyzer.SeriesSignals signals) {
        return signals.evidenceCount() >= 2 && signals.maxSingleDrop() >= 15D;
    }

    private String anomalyDetail(String type, RiskAnomalyAnalyzer.SeriesSignals signals, String description) {
        return writeJson(Map.of(
                "source", "deterministic_anomaly_rule",
                "type", type,
                "description", description,
                "previousMean", nullableNumber(signals.previousMean()),
                "recentMean", nullableNumber(signals.recentMean()),
                "delta", nullableNumber(signals.delta()),
                "volatility", signals.volatility(),
                "maxSingleDrop", signals.maxSingleDrop(),
                "directionChanges", signals.directionChanges(),
                "evidenceCount", signals.evidenceCount()));
    }

    private <T> List<T> tail(List<T> items, int maxSize) {
        if (items.size() <= maxSize) return items;
        return items.subList(items.size() - maxSize, items.size());
    }

    private List<KnowledgeMasteryHistory> readMasteryHistory(String studentId, String courseId) {
        try {
            return knowledgeMasteryHistoryMapper.selectList(new LambdaQueryWrapper<KnowledgeMasteryHistory>()
                    .eq(KnowledgeMasteryHistory::getStudentNo, studentId)
                    .eq(KnowledgeMasteryHistory::getCourseCode, courseId)
                    .orderByAsc(KnowledgeMasteryHistory::getCreatedAt));
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    private double normalizeScore(StudentScoreDTO score) {
        if (score.getTotalScore() != null && score.getTotalScore() > 0D && score.getTotalScore() != 100D) {
            return Math.round(score.getScore() / score.getTotalScore() * 10000D) / 100D;
        }
        return score.getScore();
    }

    private Integer parseInteger(String value) {
        try {
            return Integer.valueOf(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String writeJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ignored) {
            return "{}";
        }
    }

    private Object nullableNumber(Double value) {
        return value == null ? "" : value;
    }

    private record RiskContext(String studentId, String courseId, double classAvgRate,
                               List<StudentScoreDTO> scoreRecords, List<KnowledgeMasteryHistory> masteryHistory,
                               List<CompetencyScoreHistory> abilityHistory, StudentProgressDTO progress,
                               long activityGapDays, RiskAnomalyAnalyzer.SeriesSignals scoreSignals,
                               RiskAnomalyAnalyzer.SeriesSignals masterySignals,
                               RiskAnomalyAnalyzer.SeriesSignals abilitySignals) {
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
