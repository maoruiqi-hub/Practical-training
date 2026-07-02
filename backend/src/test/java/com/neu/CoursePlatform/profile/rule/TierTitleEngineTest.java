package com.neu.CoursePlatform.profile.rule;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TierTitleEngineTest {

    private final TierTitleEngine engine = new TierTitleEngine();

    @Test
    void getTitleTowerConqueror() {
        assertEquals("塔之征服者", engine.getTitle(4, 0, 5));
        assertEquals("塔之征服者", engine.getTitle(5, 10, 10));
    }

    @Test
    void getTitleKnowledgeExplorer() {
        assertEquals("知识探险家", engine.getTitle(3, 0, 3));
        assertEquals("知识探险家", engine.getTitle(3, 5, 4));
    }

    @Test
    void getTitleCodingApprentice() {
        assertEquals("编程学徒", engine.getTitle(2, 0, 1));
    }

    @Test
    void getTitleComboStar() {
        assertEquals("连击新星", engine.getTitle(1, 5, 0));
        assertEquals("连击新星", engine.getTitle(2, 10, 0));
    }

    @Test
    void getTitleNewcomer() {
        assertEquals("初入塔境", engine.getTitle(1, 0, 0));
        assertEquals("初入塔境", engine.getTitle(0, 0, 0));
    }

    @Test
    void getTitlePriorityLevel4OverComboStar() {
        assertEquals("知识探险家", engine.getTitle(4, 10, 4));
    }

    @Test
    void getNextTitleHintForLevel1() {
        assertEquals("获得第1个徽章即可晋升为'编程学徒'", engine.getNextTitleHint(1, 0));
    }

    @Test
    void getNextTitleHintForLevel2() {
        assertEquals("等级达到中级并拥有3个徽章可晋升为'知识探险家'", engine.getNextTitleHint(2, 2));
    }

    @Test
    void getNextTitleHintForLevel3() {
        assertEquals("等级达到熟练并拥有5个徽章可晋升为'塔之征服者'", engine.getNextTitleHint(3, 4));
    }

    @Test
    void getNextTitleHintForLevel4() {
        assertEquals("已是最高称号！", engine.getNextTitleHint(4, 5));
        assertEquals("已是最高称号！", engine.getNextTitleHint(5, 10));
    }
}
