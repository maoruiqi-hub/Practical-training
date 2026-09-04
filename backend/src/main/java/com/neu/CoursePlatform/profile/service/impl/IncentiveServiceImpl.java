package com.neu.CoursePlatform.profile.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.neu.CoursePlatform.entity.LearningAnswerEvidence;
import com.neu.CoursePlatform.entity.LearningTask;
import com.neu.CoursePlatform.entity.TaskSubmission;
import com.neu.CoursePlatform.mapper.LearningAnswerEvidenceMapper;
import com.neu.CoursePlatform.mapper.TaskSubmissionMapper;
import com.neu.CoursePlatform.profile.entity.*;
import com.neu.CoursePlatform.profile.mapper.AchievementMapper;
import com.neu.CoursePlatform.profile.mapper.StudentProfileMapper;
import com.neu.CoursePlatform.profile.rule.BadgeRuleEngine;
import com.neu.CoursePlatform.profile.rule.TierTitleEngine;
import com.neu.CoursePlatform.profile.service.IncentiveService;
import com.neu.CoursePlatform.profile.service.ProfileService;
import com.neu.CoursePlatform.service.LearningTaskService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class IncentiveServiceImpl implements IncentiveService {

    private final AchievementMapper achievementMapper;
    private final StudentProfileMapper profileMapper;
    private final ProfileService profileService;
    private final BadgeRuleEngine badgeEngine;
    private final TierTitleEngine titleEngine;
    private final LearningAnswerEvidenceMapper evidenceMapper;
    private final TaskSubmissionMapper submissionMapper;
    private final LearningTaskService taskService;

    public IncentiveServiceImpl(AchievementMapper achievementMapper,
                               StudentProfileMapper profileMapper,
                               ProfileService profileService,
                               BadgeRuleEngine badgeEngine,
                               TierTitleEngine titleEngine,
                               LearningAnswerEvidenceMapper evidenceMapper,
                               TaskSubmissionMapper submissionMapper,
                               LearningTaskService taskService) {
        this.achievementMapper = achievementMapper;
        this.profileMapper = profileMapper;
        this.profileService = profileService;
        this.badgeEngine = badgeEngine;
        this.titleEngine = titleEngine;
        this.evidenceMapper = evidenceMapper;
        this.submissionMapper = submissionMapper;
        this.taskService = taskService;
    }

    @Override
    public List<Achievement> checkAndAwardBadges(Integer studentNo, Integer courseCode,
            int totalCorrect, int consecutiveCorrect, boolean timedComplete,
            boolean fullScore, int nightSessions, int helpfulFeedback,
            int selfCorrections, int pythonicStyleCount) {
        List<Achievement> existing = getAchievements(studentNo, courseCode);
        List<String> existingNames = existing.stream()
                .filter(a -> "badge".equals(a.getAchievementType()))
                .flatMap(a -> java.util.stream.Stream.of(a.getBadgeCode(), a.getName()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<BadgeRuleEngine.BadgeCheck> checks = badgeEngine.checkAll(
                totalCorrect, consecutiveCorrect, timedComplete, fullScore,
                nightSessions, helpfulFeedback, selfCorrections,
                pythonicStyleCount, existingNames);

        List<Achievement> newBadges = new ArrayList<>();
        for (BadgeRuleEngine.BadgeCheck check : checks) {
            if (check.earned()) {
                Achievement a = new Achievement();
                a.setStudentNo(studentNo);
                a.setCourseCode(courseCode);
                a.setAchievementType("badge");
                a.setBadgeCode(check.code());
                a.setName(check.name());
                a.setDescription(check.description());
                a.setEarnedAt(new Date());
                a.setMetadata(badgeAuditMetadata(check.code(), totalCorrect, consecutiveCorrect,
                        timedComplete, fullScore, nightSessions, helpfulFeedback,
                        selfCorrections, pythonicStyleCount));
                try {
                    achievementMapper.insert(a);
                    newBadges.add(a);
                } catch (DuplicateKeyException ignored) {
                    // 并发评估同一事实时由数据库唯一约束保证只授予一次。
                }
            }
        }
        return newBadges;
    }

    private String badgeAuditMetadata(String badgeCode, int totalCorrect, int consecutiveCorrect,
                                      boolean timedComplete, boolean fullScore, int nightSessions,
                                      int helpfulFeedback, int selfCorrections, int pythonicStyleCount) {
        return String.format(Locale.ROOT,
                "{\"source\":\"trusted_facts_v1\",\"badgeCode\":\"%s\",\"facts\":{" +
                        "\"totalCorrect\":%d,\"consecutiveCorrect\":%d,\"timedComplete\":%s," +
                        "\"fullScore\":%s,\"nightAnswers\":%d,\"helpfulFeedback\":%d," +
                        "\"selfCorrections\":%d,\"pythonicStyleCount\":%d}}",
                badgeCode, totalCorrect, consecutiveCorrect, timedComplete, fullScore,
                nightSessions, helpfulFeedback, selfCorrections, pythonicStyleCount);
    }

    @Override
    @Transactional
    public List<Achievement> evaluateAndAward(Integer studentNo, Integer courseCode) {
        if (studentNo == null || courseCode == null) return List.of();
        // 同一学生的并发答题提交可能同时触发评估；行锁使“查询已有徽章 → 授予”串行化。
        achievementMapper.lockStudentForBadgeEvaluation(String.valueOf(studentNo));
        List<LearningAnswerEvidence> evidence = evidenceMapper.selectList(
                new LambdaQueryWrapper<LearningAnswerEvidence>()
                        .eq(LearningAnswerEvidence::getStudentNo, String.valueOf(studentNo))
                        .eq(LearningAnswerEvidence::getCourseCode, String.valueOf(courseCode))
                        .orderByAsc(LearningAnswerEvidence::getAnsweredAt));
        if (evidence == null) evidence = List.of();

        int totalCorrect = (int) evidence.stream().filter(item -> Boolean.TRUE.equals(item.getCorrect())).count();
        int consecutiveCorrect = trailingCorrect(evidence);
        int nightAnswers = (int) evidence.stream().filter(this::isNightAnswer).count();
        int selfCorrections = selfCorrectionCount(evidence);
        boolean fullScore = hasFullScoreSubmission(studentNo, courseCode);

        // 尚无可信计时、反馈质量和代码风格事实源，对应徽章保持不可授予。
        return checkAndAwardBadges(studentNo, courseCode, totalCorrect, consecutiveCorrect,
                false, fullScore, nightAnswers, 0, selfCorrections, 0);
    }

    private int trailingCorrect(List<LearningAnswerEvidence> evidence) {
        int count = 0;
        for (int i = evidence.size() - 1; i >= 0; i--) {
            if (!Boolean.TRUE.equals(evidence.get(i).getCorrect())) break;
            count++;
        }
        return count;
    }

    private boolean isNightAnswer(LearningAnswerEvidence evidence) {
        if (evidence.getAnsweredAt() == null) return false;
        LocalTime time = evidence.getAnsweredAt().toLocalTime();
        return time.isBefore(LocalTime.of(6, 0)) || !time.isBefore(LocalTime.of(22, 0));
    }

    private int selfCorrectionCount(List<LearningAnswerEvidence> evidence) {
        Set<String> wrongQuestions = new HashSet<>();
        Set<String> correctedQuestions = new HashSet<>();
        for (LearningAnswerEvidence item : evidence) {
            if (item.getQuestionId() == null) continue;
            if (Boolean.TRUE.equals(item.getCorrect())) {
                if (wrongQuestions.contains(item.getQuestionId())) correctedQuestions.add(item.getQuestionId());
            } else {
                wrongQuestions.add(item.getQuestionId());
            }
        }
        return correctedQuestions.size();
    }

    private boolean hasFullScoreSubmission(Integer studentNo, Integer courseCode) {
        List<TaskSubmission> submissions = submissionMapper.selectByStudentNoAndCourse(
                String.valueOf(studentNo), String.valueOf(courseCode));
        if (submissions == null) return false;
        for (TaskSubmission submission : submissions) {
            if (submission.getScore() == null || "superseded".equalsIgnoreCase(submission.getStatus())) continue;
            LearningTask task = taskService.getById(submission.getTaskNo());
            if (task != null && task.getScore() != null && task.getScore() > 0
                    && submission.getScore() >= task.getScore()) return true;
        }
        return false;
    }

    @Override
    public List<Achievement> getAchievements(Integer studentNo, Integer courseCode) {
        LambdaQueryWrapper<Achievement> q = new LambdaQueryWrapper<>();
        q.eq(Achievement::getStudentNo, studentNo)
         .eq(Achievement::getCourseCode, courseCode)
         .orderByDesc(Achievement::getEarnedAt);
        return achievementMapper.selectList(q);
    }

    @Override
    public List<Map<String, Object>> getLeaderboard(Integer courseCode, String type) {
        LambdaQueryWrapper<StudentProfile> q = new LambdaQueryWrapper<>();
        q.eq(StudentProfile::getCourseCode, courseCode);
        if ("coins".equals(type)) {
            q.orderByDesc(StudentProfile::getCoins);
        } else if ("exp".equals(type)) {
            q.orderByDesc(StudentProfile::getExp);
        } else {
            q.orderByDesc(StudentProfile::getExp);
        }
        List<StudentProfile> profiles = profileMapper.selectList(q);
        List<Integer> studentNos = profiles.stream()
                .limit(20)
                .map(StudentProfile::getStudentNo)
                .filter(Objects::nonNull)
                .toList();
        Map<Integer, Long> badgeCounts = studentNos.isEmpty()
                ? Map.of()
                : achievementMapper.selectList(new LambdaQueryWrapper<Achievement>()
                        .eq(Achievement::getCourseCode, courseCode)
                        .eq(Achievement::getAchievementType, "badge")
                        .in(Achievement::getStudentNo, studentNos))
                        .stream()
                        .collect(Collectors.groupingBy(Achievement::getStudentNo, Collectors.counting()));

        List<Map<String, Object>> board = new ArrayList<>();
        int rank = 1;
        for (StudentProfile p : profiles.stream().limit(20).toList()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("rank", rank++);
            entry.put("studentNo", p.getStudentNo());
            entry.put("level", p.getLevel());
            entry.put("exp", p.getExp());
            entry.put("coins", p.getCoins());
            entry.put("badgeCount", badgeCounts.getOrDefault(p.getStudentNo(), 0L));
            board.add(entry);
        }
        return board;
    }

    @Override
    public String getTitle(Integer studentNo, Integer courseCode) {
        StudentProfile profile = profileService.getOrCreateProfile(studentNo, courseCode);
        int badgeCount = (int) getAchievements(studentNo, courseCode).stream()
                .filter(a -> "badge".equals(a.getAchievementType())).count();
        return titleEngine.getTitle(profile.getLevel(), 0, badgeCount);
    }
}
