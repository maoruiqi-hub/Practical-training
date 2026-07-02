package com.neu.CoursePlatform.profile.rule;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GrowthRuleEngineTest {

    private final GrowthRuleEngine engine = new GrowthRuleEngine();

    @Test
    void calcExpGainQuizCorrect() {
        assertEquals(40, engine.calcExpGain("quiz", true));
    }

    @Test
    void calcExpGainBossCorrect() {
        assertEquals(150, engine.calcExpGain("boss", true));
    }

    @Test
    void calcExpGainDefaultCorrect() {
        assertEquals(10, engine.calcExpGain("unknown", true));
    }

    @Test
    void calcExpGainWrong() {
        assertEquals(0, engine.calcExpGain("quiz", false));
        assertEquals(0, engine.calcExpGain("boss", false));
    }

    @Test
    void calcCoinGainQuizCorrect() {
        assertEquals(80, engine.calcCoinGain("quiz", true));
    }

    @Test
    void calcCoinGainBossCorrect() {
        assertEquals(300, engine.calcCoinGain("boss", true));
    }

    @Test
    void calcCoinGainDefaultCorrect() {
        assertEquals(20, engine.calcCoinGain("unknown", true));
    }

    @Test
    void calcCoinGainWrong() {
        assertEquals(0, engine.calcCoinGain("quiz", false));
    }

    @Test
    void calcLevelBeginner() {
        assertEquals(1, engine.calcLevel(0));
        assertEquals(1, engine.calcLevel(199));
    }

    @Test
    void calcLevelJunior() {
        assertEquals(2, engine.calcLevel(200));
        assertEquals(2, engine.calcLevel(499));
    }

    @Test
    void calcLevelIntermediate() {
        assertEquals(3, engine.calcLevel(500));
        assertEquals(3, engine.calcLevel(999));
    }

    @Test
    void calcLevelAdvanced() {
        assertEquals(4, engine.calcLevel(1000));
        assertEquals(4, engine.calcLevel(1999));
    }

    @Test
    void calcLevelMaster() {
        assertEquals(5, engine.calcLevel(2000));
        assertEquals(5, engine.calcLevel(9999));
    }

    @Test
    void calcLevelNegativeExp() {
        assertEquals(1, engine.calcLevel(-100));
    }

    @Test
    void getLevelNameAllLevels() {
        assertEquals("入门", engine.getLevelName(1));
        assertEquals("初级", engine.getLevelName(2));
        assertEquals("中级", engine.getLevelName(3));
        assertEquals("熟练", engine.getLevelName(4));
        assertEquals("精通", engine.getLevelName(5));
    }

    @Test
    void getLevelNameDefault() {
        assertEquals("入门", engine.getLevelName(0));
        assertEquals("入门", engine.getLevelName(99));
    }
}
