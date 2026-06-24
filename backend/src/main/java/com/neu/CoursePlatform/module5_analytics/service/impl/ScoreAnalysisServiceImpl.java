package com.neu.CoursePlatform.module5_analytics.service.impl;

import com.neu.CoursePlatform.module5_analytics.dto.*;
import com.neu.CoursePlatform.module5_analytics.dto.external.MistakeStatsDTO;
import com.neu.CoursePlatform.module5_analytics.dto.external.StudentScoreDTO;
import com.neu.CoursePlatform.module5_analytics.service.ScoreAnalysisService;
import com.neu.CoursePlatform.module5_analytics.service.external.ExternalDataProvider;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ScoreAnalysisServiceImpl implements ScoreAnalysisService {

    private final ExternalDataProvider dataProvider;

    /**
     * @Lazy 预留给 ExternalDataProvider 的真实实现：
     * 当模块4 的 Service 替换 Mock 后，模块4↔模块5 产生循环依赖，
     * @Lazy 打破该循环。不影响 Mock 阶段行为。
     */
    public ScoreAnalysisServiceImpl(@Lazy ExternalDataProvider dataProvider) {
        this.dataProvider = dataProvider;
    }

    @Override
    public ScoreOverviewDTO getClassScoreOverview(String classId, String courseId) {
        List<String> studentIds = dataProvider.getStudentIdsByClass(classId);

        // 聚合每个学生的总分
        List<ScoreOverviewDTO.StudentRanking> rankings = new ArrayList<>();
        List<Double> allScores = new ArrayList<>();

        for (String sid : studentIds) {
            List<StudentScoreDTO> scores = dataProvider.getStudentScores(sid, courseId);
            double total = scores.stream().mapToDouble(StudentScoreDTO::getScore).average().orElse(0);
            allScores.add(total);
            rankings.add(buildRanking(sid, total));
        }

        // 按总分降序排名
        rankings.sort((a, b) -> Double.compare(b.getTotalScore(), a.getTotalScore()));
        for (int i = 0; i < rankings.size(); i++) {
            rankings.get(i).setRank(i + 1);
        }

        // 统计指标
        double avg = allScores.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double max = allScores.stream().mapToDouble(Double::doubleValue).max().orElse(0);
        double min = allScores.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        double variance = allScores.stream().mapToDouble(s -> Math.pow(s - avg, 2)).average().orElse(0);
        long passCount = allScores.stream().filter(s -> s >= 60).count();

        ScoreOverviewDTO dto = new ScoreOverviewDTO();
        dto.setAvgScore(round2(avg));
        dto.setMaxScore(round2(max));
        dto.setMinScore(round2(min));
        dto.setStdDev(round2(Math.sqrt(variance)));
        dto.setPassRate(round2((double) passCount / allScores.size() * 100));
        dto.setDistribution(buildDistribution(allScores));
        dto.setRankings(rankings);
        return dto;
    }

    @Override
    public List<WeakPointDTO> getWeakPoints(String courseId) {
        List<MistakeStatsDTO> stats = dataProvider.getClassMistakeStats(courseId);
        var kpMap = dataProvider.getKnowledgePointsByCourse(courseId).stream()
                .collect(Collectors.toMap(k -> k.getId(), k -> k.getName(), (a, b) -> a));

        return stats.stream().map(s -> {
            WeakPointDTO wp = new WeakPointDTO();
            wp.setKnowledgePointId(s.getKnowledgePointId());
            wp.setKnowledgePointName(
                    kpMap.getOrDefault(s.getKnowledgePointId(), s.getKnowledgePointName()));
            wp.setScoreRate(round2(1.0 - s.getMistakeRate()));
            wp.setTotalAttempts(s.getTotalAttempts());
            wp.setMistakeCount(s.getMistakeCount());
            return wp;
        }).sorted(Comparator.comparingDouble(WeakPointDTO::getScoreRate)).toList();
    }

    @Override
    public ScoreTrendDTO getScoreTrends(String classId, String courseId, String granularity) {
        List<String> studentIds = dataProvider.getStudentIdsByClass(classId);
        // 聚合为班级均分趋势（简化为按周）
        ScoreTrendDTO dto = new ScoreTrendDTO();
        dto.setGranularity(granularity != null ? granularity : "week");

        List<ScoreTrendDTO.TrendPoint> points = new ArrayList<>();
        for (int w = 1; w <= 6; w++) {
            ScoreTrendDTO.TrendPoint pt = new ScoreTrendDTO.TrendPoint();
            pt.setLabel("W" + w);
            // 聚合所有学生第w周的成绩
            final int weekIdx = w;
            double weekAvg = studentIds.stream()
                    .mapToDouble(sid -> {
                        List<StudentScoreDTO> scores = dataProvider.getStudentScores(sid, courseId);
                        int idx = Math.min(weekIdx - 1, scores.size() - 1);
                        return idx >= 0 ? scores.get(idx).getScore() : 0;
                    })
                    .average().orElse(0);
            pt.setValue(round2(weekAvg));
            points.add(pt);
        }
        dto.setClassAvg(points);
        return dto;
    }

    @Override
    public ScoreTrendDTO getStudentScoreTrends(String studentId, String courseId, String granularity) {
        List<StudentScoreDTO> scores = dataProvider.getStudentScores(studentId, courseId);
        ScoreTrendDTO dto = new ScoreTrendDTO();
        dto.setGranularity(granularity != null ? granularity : "exam");

        List<ScoreTrendDTO.TrendPoint> studentPoints = new ArrayList<>();
        for (int i = 0; i < scores.size(); i++) {
            ScoreTrendDTO.TrendPoint pt = new ScoreTrendDTO.TrendPoint();
            pt.setLabel(scores.get(i).getTargetId());
            pt.setValue(round2(scores.get(i).getScore()));
            studentPoints.add(pt);
        }
        dto.setStudentScore(studentPoints);
        return dto;
    }

    // ---- helpers ----

    private ScoreOverviewDTO.StudentRanking buildRanking(String sid, double total) {
        ScoreOverviewDTO.StudentRanking r = new ScoreOverviewDTO.StudentRanking();
        r.setStudentId(sid);
        r.setStudentName(sid); // Phase 2 Mock：用ID代名
        r.setTotalScore(round2(total));
        return r;
    }

    private List<ScoreDistributionDTO> buildDistribution(List<Double> scores) {
        int[] counts = new int[5];
        String[] ranges = {"0-59", "60-69", "70-79", "80-89", "90-100"};
        for (double s : scores) {
            if (s < 60) counts[0]++;
            else if (s < 70) counts[1]++;
            else if (s < 80) counts[2]++;
            else if (s < 90) counts[3]++;
            else counts[4]++;
        }
        List<ScoreDistributionDTO> dist = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            dist.add(new ScoreDistributionDTO(
                    ranges[i], counts[i],
                    round2((double) counts[i] / scores.size() * 100)));
        }
        return dist;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
