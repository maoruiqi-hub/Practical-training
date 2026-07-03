package com.neu.CoursePlatform.profile.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.neu.CoursePlatform.profile.entity.*;
import com.neu.CoursePlatform.profile.mapper.AchievementMapper;
import com.neu.CoursePlatform.profile.mapper.StudentProfileMapper;
import com.neu.CoursePlatform.profile.rule.BadgeRuleEngine;
import com.neu.CoursePlatform.profile.rule.TierTitleEngine;
import com.neu.CoursePlatform.profile.service.IncentiveService;
import com.neu.CoursePlatform.profile.service.ProfileService;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class IncentiveServiceImpl implements IncentiveService {

    private final AchievementMapper achievementMapper;
    private final StudentProfileMapper profileMapper;
    private final ProfileService profileService;
    private final BadgeRuleEngine badgeEngine;
    private final TierTitleEngine titleEngine;

    public IncentiveServiceImpl(AchievementMapper achievementMapper,
                               StudentProfileMapper profileMapper,
                               ProfileService profileService,
                               BadgeRuleEngine badgeEngine,
                               TierTitleEngine titleEngine) {
        this.achievementMapper = achievementMapper;
        this.profileMapper = profileMapper;
        this.profileService = profileService;
        this.badgeEngine = badgeEngine;
        this.titleEngine = titleEngine;
    }

    @Override
    public List<Achievement> checkAndAwardBadges(Integer studentNo, Integer courseCode,
            int totalCorrect, int consecutiveCorrect, boolean timedComplete,
            boolean fullScore, int nightSessions, int helpfulFeedback,
            int selfCorrections, int pythonicStyleCount) {
        List<Achievement> existing = getAchievements(studentNo, courseCode);
        List<String> existingNames = existing.stream()
                .filter(a -> "badge".equals(a.getAchievementType()))
                .map(Achievement::getName)
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
                a.setName(check.name());
                a.setDescription(check.description());
                a.setEarnedAt(new Date());
                achievementMapper.insert(a);
                newBadges.add(a);
            }
        }
        return newBadges;
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
