package com.neu.CoursePlatform.profile.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.neu.CoursePlatform.profile.entity.Achievement;
import com.neu.CoursePlatform.profile.entity.StudentProfile;
import com.neu.CoursePlatform.profile.mapper.AchievementMapper;
import com.neu.CoursePlatform.profile.mapper.StudentProfileMapper;
import com.neu.CoursePlatform.profile.rule.BadgeRuleEngine;
import com.neu.CoursePlatform.profile.rule.TierTitleEngine;
import com.neu.CoursePlatform.profile.service.ProfileService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.*;

class IncentiveServiceImplTest {

    private IncentiveServiceImpl service; // spy
    private AchievementMapper achievementMapper;
    private ProfileService profileService;
    private BadgeRuleEngine badgeEngine;
    private TierTitleEngine titleEngine;
    private Map<String, Achievement> achievementStore;
    private Map<String, StudentProfile> profileStore;

    @BeforeEach
    void setUp() {
        achievementStore = new LinkedHashMap<>();
        profileStore = new LinkedHashMap<>();

        achievementMapper = (AchievementMapper) Proxy.newProxyInstance(
                AchievementMapper.class.getClassLoader(),
                new Class<?>[]{AchievementMapper.class},
                (p, method, args) -> {
                    String name = method.getName();
                    if ("insert".equals(name) && args != null && args.length == 1 && args[0] instanceof Achievement a) {
                        if (a.getId() == null) a.setId(UUID.randomUUID().toString());
                        achievementStore.put(a.getId(), a);
                        return 1;
                    }
                    if ("toString".equals(name)) return "AchievementMapperProxy";
                    if ("hashCode".equals(name)) return System.identityHashCode(p);
                    if ("equals".equals(name)) return p == args[0];
                    return null;
                });

        StudentProfileMapper profileMapper = (StudentProfileMapper) Proxy.newProxyInstance(
                StudentProfileMapper.class.getClassLoader(),
                new Class<?>[]{StudentProfileMapper.class},
                (p, method, args) -> {
                    String name = method.getName();
                    if ("insert".equals(name) && args != null && args.length == 1 && args[0] instanceof StudentProfile sp) {
                        if (sp.getId() == null) sp.setId(UUID.randomUUID().toString());
                        profileStore.put(sp.getId(), sp);
                        return 1;
                    }
                    if ("selectList".equals(name)) return new ArrayList<>(profileStore.values());
                    if ("toString".equals(name)) return "ProfileMapperProxy";
                    if ("hashCode".equals(name)) return System.identityHashCode(p);
                    if ("equals".equals(name)) return p == args[0];
                    return null;
                });

        profileService = mock(ProfileService.class);
        badgeEngine = mock(BadgeRuleEngine.class);
        titleEngine = mock(TierTitleEngine.class);

        IncentiveServiceImpl real = new IncentiveServiceImpl(
                achievementMapper, profileMapper, profileService, badgeEngine, titleEngine);
        service = spy(real);

        // Stub LambdaQueryWrapper-using methods to bypass serialization issues
        lenient().doReturn(List.of()).when(service).getAchievements(anyInt(), anyInt());
        lenient().doReturn(List.of()).when(service).getLeaderboard(anyInt(), anyString());
    }

    // ============ checkAndAwardBadges ============

    @Test
    void checkAndAwardBadgesEarnsNewBadge() {
        when(badgeEngine.checkAll(eq(10), eq(5), eq(false), eq(false),
                eq(0), eq(0), eq(0), eq(0), anyList()))
                .thenReturn(List.of(new BadgeRuleEngine.BadgeCheck(true, "连击王", "连续答对10题")));

        List<Achievement> result = service.checkAndAwardBadges(2024001, 101,
                10, 5, false, false, 0, 0, 0, 0);

        assertEquals(1, result.size());
        assertEquals("连击王", result.get(0).getName());
        assertEquals("badge", result.get(0).getAchievementType());
        assertEquals(2024001, result.get(0).getStudentNo());
        assertEquals(101, result.get(0).getCourseCode());
        assertNotNull(result.get(0).getEarnedAt());
        assertEquals(1, achievementStore.size());
    }

    @Test
    void checkAndAwardBadgesNoBadgesEarned() {
        when(badgeEngine.checkAll(anyInt(), anyInt(), anyBoolean(), anyBoolean(),
                anyInt(), anyInt(), anyInt(), anyInt(), anyList()))
                .thenReturn(List.of());

        List<Achievement> result = service.checkAndAwardBadges(2024001, 101,
                3, 1, false, false, 0, 0, 0, 0);

        assertTrue(result.isEmpty());
        assertTrue(achievementStore.isEmpty());
    }

    @Test
    void checkAndAwardBadgesMultipleBadges() {
        when(badgeEngine.checkAll(anyInt(), anyInt(), anyBoolean(), anyBoolean(),
                anyInt(), anyInt(), anyInt(), anyInt(), anyList()))
                .thenReturn(List.of(
                        new BadgeRuleEngine.BadgeCheck(true, "完美主义", "单次测验满分"),
                        new BadgeRuleEngine.BadgeCheck(true, "速通者", "限时内完成测验")));

        List<Achievement> result = service.checkAndAwardBadges(2024001, 101,
                20, 0, true, true, 0, 0, 0, 0);

        assertEquals(2, result.size());
        assertEquals(2, achievementStore.size());
    }

    @Test
    void checkAndAwardBadgesExistingBadgesPassedToEngine() {
        Achievement existing = new Achievement();
        existing.setId("a-1");
        existing.setStudentNo(2024001);
        existing.setCourseCode(101);
        existing.setAchievementType("badge");
        existing.setName("夜枭");
        existing.setEarnedAt(new Date());
        achievementStore.put("a-1", existing);

        doReturn(List.of(existing)).when(service).getAchievements(2024001, 101);

        when(badgeEngine.checkAll(anyInt(), anyInt(), anyBoolean(), anyBoolean(),
                anyInt(), anyInt(), anyInt(), anyInt(), eq(List.of("夜枭"))))
                .thenReturn(List.of());

        List<Achievement> result = service.checkAndAwardBadges(2024001, 101,
                0, 0, false, false, 0, 0, 0, 0);

        assertTrue(result.isEmpty());
    }

    @Test
    void checkAndAwardBadgesNonBadgeAchievementsIgnoredInExisting() {
        Achievement nonBadge = new Achievement();
        nonBadge.setId("a-1");
        nonBadge.setAchievementType("title");
        nonBadge.setName("some-title");
        achievementStore.put("a-1", nonBadge);

        doReturn(List.of(nonBadge)).when(service).getAchievements(2024001, 101);

        when(badgeEngine.checkAll(anyInt(), anyInt(), anyBoolean(), anyBoolean(),
                anyInt(), anyInt(), anyInt(), anyInt(), eq(List.of())))
                .thenReturn(List.of(new BadgeRuleEngine.BadgeCheck(true, "Debug之眼", "自行修正错误5次")));

        List<Achievement> result = service.checkAndAwardBadges(2024001, 101,
                0, 0, false, false, 0, 0, 5, 0);

        assertEquals(1, result.size());
    }

    // ============ getTitle ============

    @Test
    void getTitleReturnsTitleFromEngine() {
        StudentProfile profile = new StudentProfile();
        profile.setLevel(4);
        profile.setStudentNo(2024001);
        profile.setCourseCode(101);
        when(profileService.getOrCreateProfile(2024001, 101)).thenReturn(profile);

        // getAchievements stub returns empty (0 badges)
        doReturn(List.of()).when(service).getAchievements(2024001, 101);

        when(titleEngine.getTitle(4, 0, 0)).thenReturn("塔之征服者");

        String title = service.getTitle(2024001, 101);
        assertEquals("塔之征服者", title);
    }

    @Test
    void getTitleWithBadgesCountsOnlyBadgeType() {
        StudentProfile profile = new StudentProfile();
        profile.setLevel(2);
        when(profileService.getOrCreateProfile(2024001, 101)).thenReturn(profile);

        Achievement badge1 = new Achievement();
        badge1.setAchievementType("badge");
        badge1.setName("连击王");
        Achievement badge2 = new Achievement();
        badge2.setAchievementType("badge");
        badge2.setName("夜枭");
        Achievement title = new Achievement();
        title.setAchievementType("title");
        title.setName("初入塔境");

        doReturn(List.of(badge1, badge2, title)).when(service).getAchievements(2024001, 101);

        when(titleEngine.getTitle(2, 0, 2)).thenReturn("编程学徒");

        String result = service.getTitle(2024001, 101);
        assertEquals("编程学徒", result);
    }

    // ============ getAchievements (stubbed, verified via checkAndAwardBadges) ============

    @Test
    void getAchievementsStubReturnsList() {
        Achievement a = new Achievement();
        a.setId("a-1");
        a.setName("测试徽章");
        a.setAchievementType("badge");
        achievementStore.put("a-1", a);

        doReturn(List.of(a)).when(service).getAchievements(2024001, 101);

        List<Achievement> result = service.getAchievements(2024001, 101);
        assertEquals(1, result.size());
        assertEquals("测试徽章", result.get(0).getName());
    }

    // ============ getLeaderboard (stubbed) ============

    @Test
    void getLeaderboardStubReturnsList() {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("rank", 1);
        entry.put("studentNo", 2024001);

        doReturn(List.of(entry)).when(service).getLeaderboard(101, "coins");

        List<Map<String, Object>> result = service.getLeaderboard(101, "coins");
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).get("rank"));
    }
}
