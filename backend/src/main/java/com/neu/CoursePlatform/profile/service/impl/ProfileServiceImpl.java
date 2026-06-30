package com.neu.CoursePlatform.profile.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.neu.CoursePlatform.common.GameEventTypes;
import com.neu.CoursePlatform.common.SharedIds;
import com.neu.CoursePlatform.common.event.GameEvent;
import com.neu.CoursePlatform.common.event.GameEventPublisher;
import com.neu.CoursePlatform.entity.AbilityPoint;
import com.neu.CoursePlatform.entity.KnowledgePointFloorStatus;
import com.neu.CoursePlatform.entity.Student;
import com.neu.CoursePlatform.mapper.KnowledgePointFloorStatusMapper;
import com.neu.CoursePlatform.mapper.StudentMapper;
import com.neu.CoursePlatform.profile.entity.*;
import com.neu.CoursePlatform.profile.mapper.*;
import com.neu.CoursePlatform.profile.rule.GrowthRuleEngine;
import com.neu.CoursePlatform.profile.service.ProfileService;
import com.neu.CoursePlatform.service.AbilityPointService;
import com.neu.CoursePlatform.service.CourseGameConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ProfileServiceImpl implements ProfileService {

    private final StudentProfileMapper profileMapper;
    private final CompetencyScoreMapper competencyMapper;
    private final CompetencyScoreHistoryMapper historyMapper;
    private final GrowthHistoryMapper growthHistoryMapper;
    private final AchievementMapper achievementMapper;
    private final StudentMapper studentMapper;
    private final AbilityPointService abilityPointService;
    private final GrowthRuleEngine growthEngine;
    private final GameEventPublisher eventPublisher;
    private final CourseGameConfigService gameConfigService;
    private final KnowledgePointFloorStatusMapper floorStatusMapper;

    @Autowired
    public ProfileServiceImpl(StudentProfileMapper profileMapper,
                             CompetencyScoreMapper competencyMapper,
                             CompetencyScoreHistoryMapper historyMapper,
                             GrowthHistoryMapper growthHistoryMapper,
                             AchievementMapper achievementMapper,
                             StudentMapper studentMapper,
                             AbilityPointService abilityPointService,
                             GrowthRuleEngine growthEngine,
                             GameEventPublisher eventPublisher,
                             CourseGameConfigService gameConfigService,
                             KnowledgePointFloorStatusMapper floorStatusMapper) {
        this.profileMapper = profileMapper;
        this.competencyMapper = competencyMapper;
        this.historyMapper = historyMapper;
        this.growthHistoryMapper = growthHistoryMapper;
        this.achievementMapper = achievementMapper;
        this.studentMapper = studentMapper;
        this.abilityPointService = abilityPointService;
        this.growthEngine = growthEngine;
        this.eventPublisher = eventPublisher;
        this.gameConfigService = gameConfigService;
        this.floorStatusMapper = floorStatusMapper;
    }

    public ProfileServiceImpl(StudentProfileMapper profileMapper,
                             CompetencyScoreMapper competencyMapper,
                             CompetencyScoreHistoryMapper historyMapper,
                             GrowthHistoryMapper growthHistoryMapper,
                             AchievementMapper achievementMapper,
                             StudentMapper studentMapper,
                             AbilityPointService abilityPointService,
                             GrowthRuleEngine growthEngine,
                             GameEventPublisher eventPublisher,
                             CourseGameConfigService gameConfigService) {
        this(profileMapper, competencyMapper, historyMapper, growthHistoryMapper, achievementMapper,
                studentMapper, abilityPointService, growthEngine, eventPublisher, gameConfigService, null);
    }

    @Override
    public StudentProfile getOrCreateProfile(Integer studentNo, Integer courseCode) {
        LambdaQueryWrapper<StudentProfile> q = new LambdaQueryWrapper<>();
        q.eq(StudentProfile::getStudentNo, studentNo)
         .eq(StudentProfile::getCourseCode, courseCode);
        StudentProfile profile = profileMapper.selectOne(q);
        if (profile != null) return profile;

        profile = new StudentProfile();
        profile.setStudentNo(studentNo);
        profile.setCourseCode(courseCode);
        profile.setHp(100);
        profile.setAtk(50);
        profile.setDef(50);
        profile.setExp(0);
        profile.setLevel(1);
        profile.setCoins(0);
        profile.setEnergy(5);
        profile.setStatus("正常学习");
        profile.setConsecutiveCorrect(0);
        profile.setRecentAnswers("");
        profileMapper.insert(profile);

        // 初始化能力评分
        List<AbilityPoint> abilityPoints = abilityPointService.listByCourseCode(String.valueOf(courseCode));
        for (AbilityPoint ap : abilityPoints) {
            CompetencyScore cs = new CompetencyScore();
            cs.setStudentNo(studentNo);
            cs.setCourseCode(courseCode);
            cs.setAbilityPointId(ap.getAbilityPointId());
            cs.setAbilityPointName(ap.getName());
            cs.setScore(50);
            competencyMapper.insert(cs);
        }

        return profile;
    }

    @Override
    public void updateProfileFromSubmission(Integer studentNo, Integer courseCode,
                                           boolean correct, String taskType) {
        StudentProfile profile = getOrCreateProfile(studentNo, courseCode);

        int expGain = growthEngine.calcExpGain(taskType, correct);
        int coinGain = growthEngine.calcCoinGain(taskType, correct);

        // HP: 初始100, 错题-10, 连续正确+5 (R2.2)
        if (correct) {
            profile.setConsecutiveCorrect(
                (profile.getConsecutiveCorrect() != null ? profile.getConsecutiveCorrect() : 0) + 1);
            profile.setHp(Math.min(100, profile.getHp() + 5));
        } else {
            profile.setConsecutiveCorrect(0);
            profile.setHp(Math.max(0, profile.getHp() - 10));
        }

        // ATK: 最近10次答题加权正确率 (R2.3)
        int newAtk = computeAtk(profile.getRecentAnswers(), correct);
        profile.setAtk(newAtk);

        // 维护最近答题记录 (最多保留10条)
        String recent = (profile.getRecentAnswers() != null ? profile.getRecentAnswers() : "");
        recent = recent.isEmpty() ? (correct ? "1" : "0")
                : recent + "," + (correct ? "1" : "0");
        // 只保留最近10条
        String[] parts = recent.split(",");
        if (parts.length > 10) {
            recent = String.join(",", Arrays.copyOfRange(parts, parts.length - 10, parts.length));
        }
        profile.setRecentAnswers(recent);

        profile.setExp(profile.getExp() + expGain);
        profile.setCoins(profile.getCoins() + coinGain);
        profile.setLevel(growthEngine.calcLevel(profile.getExp()));
        profile.setLastActivityDate(new Date());
        profile.setUpdatedAt(new Date());

        // 检查点 3.5: HP < 30 → 模块4 → 模块5 发布风险事件
        if (profile.getHp() < 30) {
            publishRiskEvent(GameEventTypes.HP_CRITICAL, studentNo, courseCode,
                    Map.of("hp", profile.getHp(), "threshold", 30));
        }

        // 维护最近测验成绩记录 for R3.4/R3.5
        String scores = (profile.getRecentScores() != null ? profile.getRecentScores() : "");
        scores = scores.isEmpty() ? (correct ? "100" : "0")
                : scores + "," + (correct ? "100" : "0");
        String[] scoreParts = scores.split(",");
        if (scoreParts.length > 20) {
            scores = String.join(",", Arrays.copyOfRange(scoreParts, scoreParts.length - 20, scoreParts.length));
        }
        profile.setRecentScores(scores);

        // R3: 动态评估学习状态
        evaluateStatus(profile);

        profileMapper.updateById(profile);

        // R6.6: 记录成长值变更明细
        if (expGain > 0 || coinGain > 0) {
            recordGrowthHistory(studentNo, courseCode, expGain, "exp", taskType, null);
            recordGrowthHistory(studentNo, courseCode, coinGain, "coins", taskType, null);
        }
    }

    /** R3.1-R3.5: 动态评估学习状态 */
    private void evaluateStatus(StudentProfile profile) {
        String previousStatus = profile.getStatus();
        String recentScores = profile.getRecentScores();
        if (recentScores == null || recentScores.isEmpty()) {
            profile.setStatus("正常学习");
            return;
        }
        String[] scoreParts = recentScores.split(",");

        // R3.4: 连续3次测验正确率 < 40% → 存在风险
        if (scoreParts.length >= 3) {
            int recentCount = Math.min(scoreParts.length, 3);
            int lowCount = 0;
            for (int i = scoreParts.length - recentCount; i < scoreParts.length; i++) {
                if (Integer.parseInt(scoreParts[i].trim()) < 40) lowCount++;
            }
            if (lowCount == recentCount) {
                profile.setStatus("存在风险");
                // 模块4 → 模块5: 发布卡顿风险事件
                if (!"存在风险".equals(previousStatus)) {
                    publishRiskEvent(GameEventTypes.STUCK_DETECTED,
                            profile.getStudentNo(), profile.getCourseCode(),
                            Map.of("knowledge_point", "unknown", "consecutive_fails", recentCount));
                }
                return;
            }
        }

        // R3.5: 最近5次正确率较前5次提升 ≥ 15% → 能力提升
        if (scoreParts.length >= 10) {
            int mid = scoreParts.length - 5;
            double prevAvg = 0, recentAvg = 0;
            for (int i = mid - 5; i < mid; i++) prevAvg += Integer.parseInt(scoreParts[i].trim());
            for (int i = mid; i < scoreParts.length; i++) recentAvg += Integer.parseInt(scoreParts[i].trim());
            prevAvg /= 5;
            recentAvg /= 5;
            if (recentAvg - prevAvg >= 15) {
                profile.setStatus("能力提升");
                return;
            }
        }

        // R3.3: 连续7天未活动 → 进度滞后
        Date lastActivity = profile.getLastActivityDate();
        if (lastActivity != null) {
            long daysSince = (System.currentTimeMillis() - lastActivity.getTime()) / (1000 * 60 * 60 * 24);
            if (daysSince >= 7) {
                profile.setStatus("进度滞后");
                return;
            }
        }

        profile.setStatus("正常学习");
    }

    private void publishRiskEvent(String eventType, Integer studentNo, Integer courseCode, Map<String, Object> payload) {
        String courseId = String.valueOf(courseCode);
        if (!gameConfigService.isEnabled(courseId)) return;
        eventPublisher.publish(GameEvent.builder()
                .eventId(SharedIds.newId())
                .eventType(eventType)
                .studentId(String.valueOf(studentNo))
                .courseId(courseId)
                .sourceId("profile")
                .occurredAt(LocalDateTime.now())
                .payload(payload)
                .build());
    }

    /** R6.6: 记录成长值变更明细 */
    private void recordGrowthHistory(Integer studentNo, Integer courseCode, int amount, String type, String source, String sourceId) {
        GrowthHistory gh = new GrowthHistory();
        gh.setStudentNo(studentNo);
        gh.setCourseCode(courseCode);
        gh.setAmount(amount);
        gh.setType(type);
        gh.setSource(source);
        gh.setSourceId(sourceId);
        gh.setCreatedAt(new Date());
        growthHistoryMapper.insert(gh);
    }

    @Override
    public List<CompetencyScore> getCompetencyScores(Integer studentNo, Integer courseCode) {
        getOrCreateProfile(studentNo, courseCode);
        LambdaQueryWrapper<CompetencyScore> q = new LambdaQueryWrapper<>();
        q.eq(CompetencyScore::getStudentNo, studentNo)
         .eq(CompetencyScore::getCourseCode, courseCode);
        return competencyMapper.selectList(q);
    }

    @Override
    public void updateCompetencyScores(Integer studentNo, Integer courseCode,
                                      String abilityPointId, boolean correct) {
        LambdaQueryWrapper<CompetencyScore> q = new LambdaQueryWrapper<>();
        q.eq(CompetencyScore::getStudentNo, studentNo)
         .eq(CompetencyScore::getCourseCode, courseCode)
         .eq(CompetencyScore::getAbilityPointId, abilityPointId);
        CompetencyScore cs = competencyMapper.selectOne(q);
        if (cs != null) {
            int oldScore = cs.getScore();
            int newScore = correct
                ? Math.min(100, cs.getScore() + 2)
                : Math.max(0, cs.getScore() - 1);
            cs.setScore(newScore);
            cs.setLastUpdated(new Date());
            competencyMapper.updateById(cs);

            // 记录变更历史 (R4.6)
            CompetencyScoreHistory history = new CompetencyScoreHistory();
            history.setStudentNo(studentNo);
            history.setCourseCode(courseCode);
            history.setAbilityPointId(abilityPointId);
            history.setOldScore(oldScore);
            history.setNewScore(newScore);
            history.setChangeReason(correct ? "答题正确+2" : "答题错误-1");
            history.setChangedAt(new Date());
            historyMapper.insert(history);

            // DEF: 前置知识点评均掌握度 (R2.4)
            recomputeDef(studentNo, courseCode);
        }
    }

    /** 重新计算DEF = 基础层级(level 1)能力点的平均分 */
    private void recomputeDef(Integer studentNo, Integer courseCode) {
        List<CompetencyScore> allScores = getCompetencyScores(studentNo, courseCode);
        double avgDef = allScores.stream()
            .mapToInt(CompetencyScore::getScore)
            .average()
            .orElse(50);

        StudentProfile profile = getOrCreateProfile(studentNo, courseCode);
        profile.setDef((int) Math.round(avgDef));
        profileMapper.updateById(profile);
    }

    /** 计算ATK: 最近10次答题的加权正确率 (R2.3) */
    static int computeAtk(String recentAnswers, boolean lastCorrect) {
        String recent = (recentAnswers != null && !recentAnswers.isEmpty())
            ? recentAnswers + "," + (lastCorrect ? "1" : "0") : (lastCorrect ? "1" : "0");
        String[] parts = recent.split(",");
        // 只取最近10条
        if (parts.length > 10) {
            parts = Arrays.copyOfRange(parts, parts.length - 10, parts.length);
        }

        int n = parts.length;
        if (n == 0) return 50;

        // 权重: 越近权重越高, 最近权重=n, 最远权重=1
        int weightedSum = 0;
        int totalWeight = 0;
        for (int i = 0; i < n; i++) {
            int weight = i + 1;
            totalWeight += weight;
            if ("1".equals(parts[i].trim())) {
                weightedSum += weight;
            }
        }

        return (int) Math.round((double) weightedSum / totalWeight * 100);
    }

    @Override
    public Map<String, Object> getProfileSummary(Integer studentNo, Integer courseCode) {
        StudentProfile profile = getOrCreateProfile(studentNo, courseCode);
        List<CompetencyScore> scores = getCompetencyScores(studentNo, courseCode);
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("profile", profile);
        summary.put("competencyScores", scores);
        summary.put("abilityMap", getCourseAbilityMap(courseCode));
        return summary;
    }

    @Override
    public void addGrowth(Integer studentNo, Integer courseCode, int amount, String source, String sourceId) {
        StudentProfile profile = getOrCreateProfile(studentNo, courseCode);
        int expGain = 0, coinGain = 0;
        switch (source != null ? source : "") {
            case "task_complete":
                expGain = amount;
                coinGain = amount / 2;
                break;
            case "exam_pass":
                expGain = amount;
                coinGain = amount;
                break;
            case "streak":
                expGain = amount;
                coinGain = amount / 4;
                break;
            default:
                expGain = amount;
                break;
        }
        profile.setExp(profile.getExp() + expGain);
        profile.setCoins(profile.getCoins() + coinGain);
        profile.setLevel(growthEngine.calcLevel(profile.getExp()));
        profile.setUpdatedAt(new Date());
        profileMapper.updateById(profile);

        if (amount < 0) {
            profile.setHp(Math.max(0, profile.getHp() + amount));
            profileMapper.updateById(profile);
        }

        if (expGain != 0) recordGrowthHistory(studentNo, courseCode, expGain, "exp", source, sourceId);
        if (coinGain != 0) recordGrowthHistory(studentNo, courseCode, coinGain, "coins", source, sourceId);
    }

    @Override
    public void applyGameDelta(Integer studentNo, Integer courseCode,
                               int hpDelta, int atkDelta, int defDelta, int expDelta, int coinDelta, int energyDelta,
                               String source, String sourceId) {
        StudentProfile profile = getOrCreateProfile(studentNo, courseCode);
        profile.setHp(clamp(intValue(profile.getHp(), 100) + hpDelta, 0, 100));
        profile.setAtk(clamp(intValue(profile.getAtk(), 50) + atkDelta, 0, 100));
        profile.setDef(clamp(intValue(profile.getDef(), 50) + defDelta, 0, 100));
        profile.setExp(Math.max(0, intValue(profile.getExp(), 0) + expDelta));
        profile.setCoins(Math.max(0, intValue(profile.getCoins(), 0) + coinDelta));
        profile.setEnergy(Math.max(0, intValue(profile.getEnergy(), 0) + energyDelta));
        profile.setLevel(growthEngine.calcLevel(profile.getExp()));
        profile.setLastActivityDate(new Date());
        profile.setUpdatedAt(new Date());
        profileMapper.updateById(profile);

        if (hpDelta != 0) recordGrowthHistory(studentNo, courseCode, hpDelta, "hp", source, sourceId);
        if (atkDelta != 0) recordGrowthHistory(studentNo, courseCode, atkDelta, "atk", source, sourceId);
        if (defDelta != 0) recordGrowthHistory(studentNo, courseCode, defDelta, "def", source, sourceId);
        if (expDelta != 0) recordGrowthHistory(studentNo, courseCode, expDelta, "exp", source, sourceId);
        if (coinDelta != 0) recordGrowthHistory(studentNo, courseCode, coinDelta, "coins", source, sourceId);
        if (energyDelta != 0) recordGrowthHistory(studentNo, courseCode, energyDelta, "energy", source, sourceId);
    }

    private static int intValue(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public Map<String, Object> generateProfile(Integer studentNo, Integer courseCode) {
        StudentProfile profile = getOrCreateProfile(studentNo, courseCode);
        profile.setUpdatedAt(new Date());
        profileMapper.updateById(profile);
        recomputeDef(studentNo, courseCode);
        return getProfileSummary(studentNo, courseCode);
    }

    @Override
    public List<CompetencyScore> updateAllCompetencyScores(Integer studentNo, Integer courseCode) {
        getOrCreateProfile(studentNo, courseCode);
        LambdaQueryWrapper<CompetencyScore> q = new LambdaQueryWrapper<>();
        q.eq(CompetencyScore::getStudentNo, studentNo)
         .eq(CompetencyScore::getCourseCode, courseCode);
        List<CompetencyScore> scores = competencyMapper.selectList(q);
        for (CompetencyScore cs : scores) {
            cs.setLastUpdated(new Date());
            competencyMapper.updateById(cs);
        }
        recomputeDef(studentNo, courseCode);
        return scores;
    }

    @Override
    public List<Map<String, Object>> getCompetencyHistory(Integer studentNo, Integer courseCode, String abilityPointId) {
        LambdaQueryWrapper<CompetencyScoreHistory> q = new LambdaQueryWrapper<>();
        q.eq(CompetencyScoreHistory::getStudentNo, studentNo)
         .eq(CompetencyScoreHistory::getCourseCode, courseCode);
        if (abilityPointId != null && !abilityPointId.isEmpty()) {
            q.eq(CompetencyScoreHistory::getAbilityPointId, abilityPointId);
        }
        q.orderByDesc(CompetencyScoreHistory::getChangedAt);
        List<CompetencyScoreHistory> records = historyMapper.selectList(q);
        List<Map<String, Object>> result = new ArrayList<>();
        for (CompetencyScoreHistory h : records) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("abilityPointId", h.getAbilityPointId());
            entry.put("oldScore", h.getOldScore());
            entry.put("newScore", h.getNewScore());
            entry.put("changeReason", h.getChangeReason());
            entry.put("changedAt", h.getChangedAt());
            result.add(entry);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getGrowthHistory(Integer studentNo, Integer courseCode) {
        LambdaQueryWrapper<GrowthHistory> q = new LambdaQueryWrapper<>();
        q.eq(GrowthHistory::getStudentNo, studentNo)
         .eq(GrowthHistory::getCourseCode, courseCode)
         .orderByDesc(GrowthHistory::getCreatedAt);
        List<GrowthHistory> records = growthHistoryMapper.selectList(q);
        List<Map<String, Object>> result = new ArrayList<>();
        for (GrowthHistory g : records) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("amount", g.getAmount());
            entry.put("type", g.getType());
            entry.put("source", g.getSource());
            entry.put("sourceId", g.getSourceId());
            entry.put("createdAt", g.getCreatedAt());
            result.add(entry);
        }
        return result;
    }

    @Override
    public Map<String, Object> generateTestFeedback(Integer studentNo, Integer courseCode) {
        StudentProfile profile = getOrCreateProfile(studentNo, courseCode);
        List<CompetencyScore> scores = getCompetencyScores(studentNo, courseCode);

        Map<String, Object> feedback = new LinkedHashMap<>();
        feedback.put("status", profile.getStatus());
        feedback.put("hp", profile.getHp());
        feedback.put("atk", profile.getAtk());
        feedback.put("def", profile.getDef());
        feedback.put("level", profile.getLevel());
        feedback.put("exp", profile.getExp());

        // 找最薄弱的能力点 (score < 60)
        List<Map<String, String>> weakPoints = new ArrayList<>();
        for (CompetencyScore cs : scores) {
            if (cs.getScore() < 60) {
                Map<String, String> wp = new LinkedHashMap<>();
                wp.put("name", cs.getAbilityPointName());
                wp.put("score", String.valueOf(cs.getScore()));
                wp.put("suggestion", cs.getScore() < 40 ? "建议重点复习基础知识" : "建议做专项练习巩固");
                weakPoints.add(wp);
            }
        }
        feedback.put("weakPoints", weakPoints);

        // 下一步建议
        String nextAction;
        if (profile.getHp() <= 20) {
            nextAction = "你的HP值较低，建议先复习基础知识再继续答题";
        } else if (!weakPoints.isEmpty()) {
            nextAction = "建议优先复习：" + weakPoints.get(0).get("name");
        } else if (profile.getExp() >= 2000) {
            nextAction = "已达到精通等级！尝试挑战更多综合测验吧";
        } else {
            nextAction = "继续保持当前的学习节奏，做得好！";
        }
        feedback.put("nextAction", nextAction);
        feedback.put("generatedAt", new Date());
        return feedback;
    }

    @Override
    public List<Map<String, Object>> listCourseStudentProfiles(Integer courseCode) {
        List<Map<String, Object>> result = new ArrayList<>();

        // 查询所有学生
        List<Student> students = studentMapper.selectList(null);

        for (Student s : students) {
            Integer numericStudentNo = parseStudentNo(s.getStudentNo());
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("studentNo", s.getStudentNo());
            entry.put("name", s.getName());
            entry.put("className", s.getClassName());
            entry.put("college", s.getCollege());
            entry.put("phone", s.getPhone());

            if (numericStudentNo == null) {
                entry.put("hasProfile", false);
                entry.put("hp", 0);
                entry.put("atk", 0);
                entry.put("def", 0);
                entry.put("exp", 0);
                entry.put("level", 1);
                entry.put("coins", 0);
                entry.put("status", "学号暂不支持画像");
                entry.put("badgeCount", 0);
                result.add(entry);
                continue;
            }

            // 查询画像
            LambdaQueryWrapper<StudentProfile> pq = new LambdaQueryWrapper<>();
            pq.eq(StudentProfile::getStudentNo, numericStudentNo)
              .eq(StudentProfile::getCourseCode, courseCode);
            StudentProfile profile = profileMapper.selectOne(pq);
            if (profile != null) {
                entry.put("hasProfile", true);
                entry.put("hp", profile.getHp());
                entry.put("atk", profile.getAtk());
                entry.put("def", profile.getDef());
                entry.put("exp", profile.getExp());
                entry.put("level", profile.getLevel());
                entry.put("coins", profile.getCoins());
                entry.put("status", profile.getStatus());
                entry.put("lastActivityDate", profile.getLastActivityDate());

                // 徽章数量
                LambdaQueryWrapper<Achievement> aq = new LambdaQueryWrapper<>();
                aq.eq(Achievement::getStudentNo, numericStudentNo)
                  .eq(Achievement::getCourseCode, courseCode)
                  .eq(Achievement::getAchievementType, "badge");
                entry.put("badgeCount", achievementMapper.selectCount(aq));
            } else {
                entry.put("hasProfile", false);
                entry.put("hp", 0);
                entry.put("atk", 0);
                entry.put("def", 0);
                entry.put("exp", 0);
                entry.put("level", 1);
                entry.put("coins", 0);
                entry.put("status", "未激活");
                entry.put("badgeCount", 0);
            }
            result.add(entry);
        }

        // 按经验值降序排序
        result.sort((a, b) -> Integer.compare(
            (Integer) b.getOrDefault("exp", 0),
            (Integer) a.getOrDefault("exp", 0)));
        return result;
    }

    @Override
    public List<Map<String, Object>> getTowerMap(Integer studentNo, Integer courseCode) {
        // 确画像已初始化
        getOrCreateProfile(studentNo, courseCode);
        List<CompetencyScore> scores = getCompetencyScores(studentNo, courseCode);
        List<Map<String, Object>> abilityMap = getCourseAbilityMap(courseCode);
        List<Map<String, Object>> towerMap = new ArrayList<>();

        // 构建 competency score 索引
        Map<String, Integer> scoreIndex = new HashMap<>();
        for (CompetencyScore cs : scores) {
            scoreIndex.put(cs.getAbilityPointId(), cs.getScore());
        }

        abilityMap.sort(Comparator.comparing(ap -> String.valueOf(ap.getOrDefault("id", ""))));

        Map<String, String> storedStatus = new HashMap<>();
        if (floorStatusMapper != null) {
            List<KnowledgePointFloorStatus> statuses = floorStatusMapper.selectList(
                    new LambdaQueryWrapper<KnowledgePointFloorStatus>()
                            .eq(KnowledgePointFloorStatus::getStudentId, String.valueOf(studentNo))
                            .eq(KnowledgePointFloorStatus::getCourseId, String.valueOf(courseCode)));
            for (KnowledgePointFloorStatus status : statuses) {
                storedStatus.put(status.getKnowledgePointId(), status.getStatus());
            }
        }

        boolean previousCleared = true;
        for (int i = 0; i < abilityMap.size(); i++) {
            Map<String, Object> ap = abilityMap.get(i);
            String apId = (String) ap.get("id");
            int mastery = scoreIndex.getOrDefault(apId, 50);
            int level = i + 1;
            String saved = storedStatus.get(apId);
            String floorStatus;
            boolean masteryCleared = mastery >= 85;

            if ("cleared".equals(saved) || masteryCleared) {
                floorStatus = "cleared";
                previousCleared = true;
            } else if (!previousCleared) {
                floorStatus = "locked";
            } else if (mastery < 40 || "weak".equals(saved)) {
                floorStatus = "weak";
                previousCleared = false;
            } else {
                floorStatus = "available";
                previousCleared = false;
            }
            boolean isAccessible = !"locked".equals(floorStatus);

            Map<String, Object> floor = new LinkedHashMap<>();
            floor.put("kpId", apId);
            floor.put("kpName", ap.get("name"));
            floor.put("description", ap.getOrDefault("description", ""));
            floor.put("level", level);
            floor.put("floorStatus", floorStatus);
            floor.put("masteryRate", mastery);
            floor.put("isAccessible", isAccessible);
            towerMap.add(floor);
        }

        return towerMap;
    }

    private List<Map<String, Object>> getCourseAbilityMap(Integer courseCode) {
        List<AbilityPoint> abilityPoints = abilityPointService.listByCourseCode(String.valueOf(courseCode));
        List<Map<String, Object>> abilityMap = new ArrayList<>();
        for (AbilityPoint ap : abilityPoints) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", ap.getAbilityPointId());
            item.put("name", ap.getName());
            item.put("description", ap.getDescription() != null ? ap.getDescription() : "");
            item.put("level", 1);
            abilityMap.add(item);
        }
        return abilityMap;
    }

    private Integer parseStudentNo(String studentNo) {
        if (studentNo == null || studentNo.isBlank()) return null;
        try {
            return Integer.parseInt(studentNo);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
