package com.neu.CoursePlatform.profile.rule;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class BadgeRuleEngineTest {

    private final BadgeRuleEngine engine = new BadgeRuleEngine();

    @Test
    void checkAllGrantsConsecutiveKingAt10Consecutive() {
        List<BadgeRuleEngine.BadgeCheck> badges = engine.checkAll(10, 10, false, false, 0, 0, 0, 0, List.of());
        assertTrue(badges.stream().anyMatch(b -> b.name().equals("连击王")));
    }

    @Test
    void checkAllNoConsecutiveKingBelow10() {
        List<BadgeRuleEngine.BadgeCheck> badges = engine.checkAll(9, 9, false, false, 0, 0, 0, 0, List.of());
        assertFalse(badges.stream().anyMatch(b -> b.name().equals("连击王")));
    }

    @Test
    void checkAllGrantsPerfectionistOnFullScore() {
        List<BadgeRuleEngine.BadgeCheck> badges = engine.checkAll(10, 5, false, true, 0, 0, 0, 0, List.of());
        assertTrue(badges.stream().anyMatch(b -> b.name().equals("完美主义")));
    }

    @Test
    void checkAllGrantsSpeedRunnerOnTimedComplete() {
        List<BadgeRuleEngine.BadgeCheck> badges = engine.checkAll(5, 3, true, false, 0, 0, 0, 0, List.of());
        assertTrue(badges.stream().anyMatch(b -> b.name().equals("速通者")));
    }

    @Test
    void checkAllGrantsPythonicAt3PythonicStyle() {
        List<BadgeRuleEngine.BadgeCheck> badges = engine.checkAll(3, 2, false, false, 0, 0, 0, 3, List.of());
        assertTrue(badges.stream().anyMatch(b -> b.name().equals("Pythonic")));
    }

    @Test
    void checkAllNoPythonicBelow3() {
        List<BadgeRuleEngine.BadgeCheck> badges = engine.checkAll(2, 2, false, false, 0, 0, 0, 2, List.of());
        assertFalse(badges.stream().anyMatch(b -> b.name().equals("Pythonic")));
    }

    @Test
    void checkAllGrantsDebugEyeAt5SelfCorrections() {
        List<BadgeRuleEngine.BadgeCheck> badges = engine.checkAll(5, 3, false, false, 0, 0, 5, 0, List.of());
        assertTrue(badges.stream().anyMatch(b -> b.name().equals("Debug之眼")));
    }

    @Test
    void checkAllGrantsNightOwlAt5NightSessions() {
        List<BadgeRuleEngine.BadgeCheck> badges = engine.checkAll(5, 3, false, false, 5, 0, 0, 0, List.of());
        assertTrue(badges.stream().anyMatch(b -> b.name().equals("夜枭")));
    }

    @Test
    void checkAllGrantsHelperAt3HelpfulFeedback() {
        List<BadgeRuleEngine.BadgeCheck> badges = engine.checkAll(3, 2, false, false, 0, 3, 0, 0, List.of());
        assertTrue(badges.stream().anyMatch(b -> b.name().equals("助人者")));
    }

    @Test
    void checkAllDoesNotDuplicateExisting() {
        List<BadgeRuleEngine.BadgeCheck> badges = engine.checkAll(10, 10, false, false, 0, 0, 0, 0, List.of("连击王"));
        assertFalse(badges.stream().anyMatch(b -> b.name().equals("连击王")));
    }

    @Test
    void checkAllMultipleBadgesAtOnce() {
        List<BadgeRuleEngine.BadgeCheck> badges = engine.checkAll(10, 10, true, true, 5, 3, 5, 3, List.of());
        assertTrue(badges.size() > 1);
    }

    @Test
    void checkAllNoBadgesForMinimalProfile() {
        List<BadgeRuleEngine.BadgeCheck> badges = engine.checkAll(1, 1, false, false, 0, 0, 0, 0, List.of());
        assertTrue(badges.isEmpty());
    }

    @Test
    void checkAllAllThresholdsExactValues() {
        List<BadgeRuleEngine.BadgeCheck> badges = engine.checkAll(5, 5, false, false, 0, 0, 4, 2, List.of());
        assertTrue(badges.isEmpty());
    }

    @Test
    void checkAllBadgeHasDescription() {
        List<BadgeRuleEngine.BadgeCheck> badges = engine.checkAll(10, 10, false, false, 0, 0, 0, 0, List.of());
        for (BadgeRuleEngine.BadgeCheck badge : badges) {
            assertNotNull(badge.name());
            assertNotNull(badge.description());
            assertTrue(badge.earned());
        }
    }
}
