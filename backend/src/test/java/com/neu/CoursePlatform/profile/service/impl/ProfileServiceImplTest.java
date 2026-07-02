package com.neu.CoursePlatform.profile.service.impl;

import com.neu.CoursePlatform.common.event.GameEvent;
import com.neu.CoursePlatform.common.event.GameEventPublisher;
import com.neu.CoursePlatform.entity.*;
import com.neu.CoursePlatform.mapper.*;
import com.neu.CoursePlatform.profile.entity.*;
import com.neu.CoursePlatform.profile.mapper.*;
import com.neu.CoursePlatform.profile.rule.GrowthRuleEngine;
import com.neu.CoursePlatform.service.AbilityPointService;
import com.neu.CoursePlatform.service.CourseGameConfigService;
import com.neu.CoursePlatform.service.KnowledgePointService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceImplTest {

    @Mock private StudentProfileMapper profileMapper;
    @Mock private CompetencyScoreMapper competencyMapper;
    @Mock private CompetencyScoreHistoryMapper historyMapper;
    @Mock private GrowthHistoryMapper growthHistoryMapper;
    @Mock private AchievementMapper achievementMapper;
    @Mock private StudentMapper studentMapper;
    @Mock private AbilityPointService abilityPointService;
    @Mock private GameEventPublisher eventPublisher;
    @Mock private CourseGameConfigService gameConfigService;
    @Mock private KnowledgePointFloorStatusMapper floorStatusMapper;
    @Mock private KnowledgePointService knowledgePointService;
    @Mock private AbilityKnowledgePointMapper abilityKnowledgePointMapper;
    @Mock private KnowledgeMasteryMapper knowledgeMasteryMapper;

    // ============ getOrCreateProfile ============

    @Test
    void getOrCreateProfileReturnsExistingProfile() {
        StudentProfile existing = profile(1, 101, 100, 50, 50, 1, 300);
        when(profileMapper.selectOne(any())).thenReturn(existing);

        ProfileServiceImpl service = createService();
        StudentProfile result = service.getOrCreateProfile(1, 101);

        assertSame(existing, result);
        verify(profileMapper, never()).insert(any(com.neu.CoursePlatform.profile.entity.StudentProfile.class));
    }

    @Test
    void getOrCreateProfileCreatesNewWhenNotFound() {
        when(profileMapper.selectOne(any())).thenReturn(null);
        lenient().when(abilityPointService.listByCourseCode(anyString())).thenReturn(List.of());

        ProfileServiceImpl service = createService();
        StudentProfile result = service.getOrCreateProfile(1, 101);

        assertNotNull(result);
        assertEquals(1, result.getStudentNo());
        assertEquals(101, result.getCourseCode());
        assertEquals(100, result.getHp());
        assertEquals(50, result.getAtk());
        assertEquals(50, result.getDef());
        assertEquals(0, result.getExp());
        assertEquals(1, result.getLevel());
        assertEquals(0, result.getCoins());
        assertEquals(5, result.getEnergy());
        assertEquals("正常学习", result.getStatus());
        verify(profileMapper).insert(any(com.neu.CoursePlatform.profile.entity.StudentProfile.class));
    }

    @Test
    void getOrCreateProfileInitializesCompetencyScores() {
        when(profileMapper.selectOne(any())).thenReturn(null);
        AbilityPoint ap1 = abilityPoint("ap-1", "101", "编程基础");
        AbilityPoint ap2 = abilityPoint("ap-2", "101", "数据结构");
        when(abilityPointService.listByCourseCode("101")).thenReturn(List.of(ap1, ap2));

        ProfileServiceImpl service = createService();
        service.getOrCreateProfile(1, 101);

        ArgumentCaptor<CompetencyScore> captor = ArgumentCaptor.forClass(CompetencyScore.class);
        verify(competencyMapper, times(2)).insert(captor.capture());
        List<CompetencyScore> scores = captor.getAllValues();
        assertEquals(2, scores.size());
        assertEquals(50, scores.get(0).getScore());
        assertEquals(50, scores.get(1).getScore());
    }

    // ============ updateProfileFromSubmission ============

    @Test
    void updateProfileFromSubmissionCorrectAnswerIncreasesHP() {
        StudentProfile p = profile(1, 101, 80, 50, 50, 1, 100);
        when(profileMapper.selectOne(any())).thenReturn(p);

        ProfileServiceImpl service = createService();
        service.updateProfileFromSubmission(1, 101, true, "quiz");

        assertEquals(85, p.getHp());
        assertEquals(1, p.getConsecutiveCorrect());
    }

    @Test
    void updateProfileFromSubmissionWrongAnswerDecreasesHP() {
        StudentProfile p = profile(1, 101, 80, 50, 50, 5, 100);
        when(profileMapper.selectOne(any())).thenReturn(p);

        ProfileServiceImpl service = createService();
        service.updateProfileFromSubmission(1, 101, false, "quiz");

        assertEquals(70, p.getHp());
        assertEquals(0, p.getConsecutiveCorrect());
    }

    @Test
    void updateProfileFromSubmissionGrantsExpAndCoins() {
        StudentProfile p = profile(1, 101, 100, 50, 50, 1, 0);
        p.setCoins(0);
        when(profileMapper.selectOne(any())).thenReturn(p);

        ProfileServiceImpl service = createService();
        service.updateProfileFromSubmission(1, 101, true, "quiz");

        assertEquals(40, p.getExp());
        assertEquals(80, p.getCoins());
    }

    @Test
    void updateProfileFromSubmissionBossGrantsMoreExp() {
        StudentProfile p = profile(1, 101, 100, 50, 50, 1, 0);
        p.setCoins(0);
        when(profileMapper.selectOne(any())).thenReturn(p);

        ProfileServiceImpl service = createService();
        service.updateProfileFromSubmission(1, 101, true, "boss");

        assertEquals(150, p.getExp());
        assertEquals(300, p.getCoins());
    }

    @Test
    void updateProfileFromSubmissionLevelsUpAtThresholds() {
        StudentProfile p = profile(1, 101, 100, 50, 50, 4, 1990);
        p.setCoins(0);
        when(profileMapper.selectOne(any())).thenReturn(p);

        ProfileServiceImpl service = createService();
        service.updateProfileFromSubmission(1, 101, true, "boss");

        assertEquals(2140, p.getExp());
        assertEquals(5, p.getLevel());
    }

    @Test
    void updateProfileFromSubmissionHPBelow30PublishesRiskEvent() {
        StudentProfile p = profile(1, 101, 35, 50, 50, 1, 100);
        when(profileMapper.selectOne(any())).thenReturn(p);
        when(gameConfigService.isEnabled("101")).thenReturn(true);

        ProfileServiceImpl service = createService();
        service.updateProfileFromSubmission(1, 101, false, "quiz");

        assertEquals(25, p.getHp());
        verify(eventPublisher).publish(any(GameEvent.class));
    }

    @Test
    void updateProfileFromSubmissionHPBelow30SkipsEventWhenGameDisabled() {
        StudentProfile p = profile(1, 101, 35, 50, 50, 1, 100);
        when(profileMapper.selectOne(any())).thenReturn(p);
        when(gameConfigService.isEnabled("101")).thenReturn(false);

        ProfileServiceImpl service = createService();
        service.updateProfileFromSubmission(1, 101, false, "quiz");

        verify(eventPublisher, never()).publish(any(GameEvent.class));
    }

    @Test
    void updateProfileFromSubmissionMaintainsRecentAnswersHistory() {
        StudentProfile p = profile(1, 101, 100, 50, 50, 1, 100);
        p.setRecentAnswers("1,0,1,1,0,1,1,0,1,1");
        when(profileMapper.selectOne(any())).thenReturn(p);

        ProfileServiceImpl service = createService();
        service.updateProfileFromSubmission(1, 101, true, "quiz");

        String recent = p.getRecentAnswers();
        assertNotNull(recent);
        String[] parts = recent.split(",");
        assertEquals(10, parts.length);
        assertEquals("1", parts[parts.length - 1]);
    }

    @Test
    void updateProfileFromSubmissionEvaluatesStuckStatus() {
        StudentProfile p = profile(1, 101, 100, 50, 50, 1, 100);
        p.setRecentScores("30,35,25");
        when(profileMapper.selectOne(any())).thenReturn(p);
        when(gameConfigService.isEnabled("101")).thenReturn(true);

        ProfileServiceImpl service = createService();
        service.updateProfileFromSubmission(1, 101, false, "quiz");

        assertEquals("存在风险", p.getStatus());
    }

    // ============ computeAtk ============

    @Test
    void computeAtkReturnsFullScoreForSingleCorrect() {
        int atk = ProfileServiceImpl.computeAtk(null, true);
        assertEquals(100, atk);
    }

    @Test
    void computeAtkReturnsZeroForSingleWrong() {
        int atk = ProfileServiceImpl.computeAtk("", false);
        assertEquals(0, atk);
    }

    @Test
    void computeAtkWeightsRecentAnswersHigher() {
        int atk = ProfileServiceImpl.computeAtk("1,1,1,1", true);
        assertTrue(atk >= 80);
    }

    @Test
    void computeAtkWithTenAnswers() {
        int atk = ProfileServiceImpl.computeAtk("0,1,0,1,0,1,0,1,0,1", true);
        assertTrue(atk >= 0 && atk <= 100);
    }

    // ============ getCompetencyScores ============

    @Test
    void getCompetencyScoresReturnsScoresAndCreatesProfileIfNeeded() {
        when(profileMapper.selectOne(any())).thenReturn(null);
        lenient().when(abilityPointService.listByCourseCode(anyString())).thenReturn(List.of());
        CompetencyScore score = new CompetencyScore();
        score.setStudentNo(1);
        score.setCourseCode(101);
        score.setAbilityPointId("ap-1");
        score.setScore(80);
        when(competencyMapper.selectList(any())).thenReturn(List.of(score));

        ProfileServiceImpl service = createService();
        List<CompetencyScore> result = service.getCompetencyScores(1, 101);

        assertEquals(1, result.size());
        assertEquals(80, result.get(0).getScore());
    }

    // ============ updateCompetencyScores ============

    @Test
    void updateCompetencyScoresCorrectIncreasesScore() {
        CompetencyScore cs = new CompetencyScore();
        cs.setStudentNo(1);
        cs.setCourseCode(101);
        cs.setAbilityPointId("ap-1");
        cs.setScore(50);
        when(competencyMapper.selectOne(any())).thenReturn(cs);
        when(profileMapper.selectOne(any())).thenReturn(profile(1, 101, 100, 50, 50, 1, 100));
        lenient().when(competencyMapper.selectList(any())).thenReturn(List.of(cs));

        ProfileServiceImpl service = createService();
        service.updateCompetencyScores(1, 101, "ap-1", true);

        assertEquals(52, cs.getScore());
        verify(competencyMapper).updateById(cs);
        verify(historyMapper).insert(any(CompetencyScoreHistory.class));
    }

    @Test
    void updateCompetencyScoresWrongDecreasesScore() {
        CompetencyScore cs = new CompetencyScore();
        cs.setStudentNo(1);
        cs.setCourseCode(101);
        cs.setAbilityPointId("ap-1");
        cs.setScore(50);
        when(competencyMapper.selectOne(any())).thenReturn(cs);
        when(profileMapper.selectOne(any())).thenReturn(profile(1, 101, 100, 50, 50, 1, 100));
        lenient().when(competencyMapper.selectList(any())).thenReturn(List.of(cs));

        ProfileServiceImpl service = createService();
        service.updateCompetencyScores(1, 101, "ap-1", false);

        assertEquals(49, cs.getScore());
    }

    @Test
    void updateCompetencyScoresCorrectDoesNotExceed100() {
        CompetencyScore cs = new CompetencyScore();
        cs.setStudentNo(1);
        cs.setCourseCode(101);
        cs.setAbilityPointId("ap-1");
        cs.setScore(99);
        when(competencyMapper.selectOne(any())).thenReturn(cs);
        when(profileMapper.selectOne(any())).thenReturn(profile(1, 101, 100, 50, 50, 1, 100));
        lenient().when(competencyMapper.selectList(any())).thenReturn(List.of(cs));

        ProfileServiceImpl service = createService();
        service.updateCompetencyScores(1, 101, "ap-1", true);

        assertEquals(100, cs.getScore());
    }

    // ============ updateAllCompetencyScores ============

    @Test
    void updateAllCompetencyScoresRefreshesTimestamps() {
        when(profileMapper.selectOne(any())).thenReturn(profile(1, 101, 100, 50, 50, 1, 100));
        lenient().when(abilityPointService.listByCourseCode(anyString())).thenReturn(List.of());
        CompetencyScore cs = new CompetencyScore();
        cs.setStudentNo(1);
        cs.setCourseCode(101);
        cs.setAbilityPointId("ap-1");
        cs.setScore(60);
        when(competencyMapper.selectList(any())).thenReturn(List.of(cs));

        ProfileServiceImpl service = createService();
        List<CompetencyScore> result = service.updateAllCompetencyScores(1, 101);

        assertEquals(1, result.size());
        assertNotNull(cs.getLastUpdated());
    }

    // ============ generateProfile ============

    @Test
    void generateProfileReturnsSummary() {
        when(profileMapper.selectOne(any())).thenReturn(profile(1, 101, 100, 50, 50, 2, 500));
        lenient().when(abilityPointService.listByCourseCode(anyString())).thenReturn(List.of());
        when(competencyMapper.selectList(any())).thenReturn(List.of());

        ProfileServiceImpl service = createService();
        Map<String, Object> summary = service.generateProfile(1, 101);

        assertNotNull(summary);
        assertNotNull(summary.get("profile"));
        assertNotNull(summary.get("competencyScores"));
    }

    // ============ generateTestFeedback ============

    @Test
    void generateTestFeedbackForHealthyProfile() {
        StudentProfile p = profile(1, 101, 100, 80, 70, 3, 800);
        when(profileMapper.selectOne(any())).thenReturn(p);
        lenient().when(abilityPointService.listByCourseCode(anyString())).thenReturn(List.of());
        when(competencyMapper.selectList(any())).thenReturn(List.of());

        ProfileServiceImpl service = createService();
        Map<String, Object> feedback = service.generateTestFeedback(1, 101);

        assertEquals("正常学习", feedback.get("status"));
        assertEquals(100, feedback.get("hp"));
        assertNotNull(feedback.get("nextAction"));
    }

    @Test
    void generateTestFeedbackForLowHPProfile() {
        StudentProfile p = profile(1, 101, 15, 50, 50, 1, 50);
        when(profileMapper.selectOne(any())).thenReturn(p);
        lenient().when(abilityPointService.listByCourseCode(anyString())).thenReturn(List.of());
        when(competencyMapper.selectList(any())).thenReturn(List.of());

        ProfileServiceImpl service = createService();
        Map<String, Object> feedback = service.generateTestFeedback(1, 101);

        assertTrue(((String) feedback.get("nextAction")).contains("HP"));
    }

    @Test
    void generateTestFeedbackForMasteredLevel() {
        StudentProfile p = profile(1, 101, 100, 90, 90, 5, 2500);
        when(profileMapper.selectOne(any())).thenReturn(p);
        lenient().when(abilityPointService.listByCourseCode(anyString())).thenReturn(List.of());
        when(competencyMapper.selectList(any())).thenReturn(List.of());

        ProfileServiceImpl service = createService();
        Map<String, Object> feedback = service.generateTestFeedback(1, 101);

        assertTrue(((String) feedback.get("nextAction")).contains("精通"));
    }

    // ============ addGrowth ============

    @Test
    void addGrowthForTaskComplete() {
        StudentProfile p = profile(1, 101, 100, 50, 50, 1, 100);
        p.setCoins(0);
        p.setExp(0);
        when(profileMapper.selectOne(any())).thenReturn(p);

        ProfileServiceImpl service = createService();
        service.addGrowth(1, 101, 100, "task_complete", "task-1");

        assertEquals(100, p.getExp());
        assertEquals(50, p.getCoins());
    }

    @Test
    void addGrowthForExamPass() {
        StudentProfile p = profile(1, 101, 100, 50, 50, 1, 100);
        p.setCoins(0);
        p.setExp(0);
        when(profileMapper.selectOne(any())).thenReturn(p);

        ProfileServiceImpl service = createService();
        service.addGrowth(1, 101, 200, "exam_pass", "exam-1");

        assertEquals(200, p.getExp());
        assertEquals(200, p.getCoins());
    }

    @Test
    void addGrowthForNegativeAmountDamagesHP() {
        StudentProfile p = profile(1, 101, 80, 50, 50, 1, 100);
        when(profileMapper.selectOne(any())).thenReturn(p);

        ProfileServiceImpl service = createService();
        service.addGrowth(1, 101, -20, "penalty", null);

        assertEquals(60, p.getHp());
    }

    // ============ applyGameDelta ============

    @Test
    void applyGameDeltaUpdatesAllFields() {
        StudentProfile p = profile(1, 101, 80, 50, 50, 2, 300);
        p.setCoins(300);
        p.setEnergy(5);
        when(profileMapper.selectOne(any())).thenReturn(p);

        ProfileServiceImpl service = createService();
        service.applyGameDelta(1, 101, 10, 5, 3, 100, 50, 2, "boss_reward", "run-1");

        assertEquals(90, p.getHp());
        assertEquals(55, p.getAtk());
        assertEquals(53, p.getDef());
        assertEquals(400, p.getExp());
        assertEquals(350, p.getCoins());
        assertEquals(7, p.getEnergy());
    }

    @Test
    void applyGameDeltaClampsValues() {
        StudentProfile p = profile(1, 101, 95, 5, 5, 2, 300);
        p.setCoins(5);
        p.setEnergy(5);
        when(profileMapper.selectOne(any())).thenReturn(p);

        ProfileServiceImpl service = createService();
        service.applyGameDelta(1, 101, 20, -50, -50, -500, -500, -10, "test", null);

        assertEquals(100, p.getHp());
        assertEquals(0, p.getAtk());
        assertEquals(0, p.getDef());
        assertEquals(0, p.getExp());
        assertEquals(0, p.getCoins());
        assertEquals(0, p.getEnergy());
    }

    // ============ getCompetencyHistory ============

    @Test
    void getCompetencyHistoryReturnsRecords() {
        CompetencyScoreHistory record = new CompetencyScoreHistory();
        record.setAbilityPointId("ap-1");
        record.setOldScore(50);
        record.setNewScore(52);
        record.setChangeReason("答题正确+2");
        record.setChangedAt(new Date());
        when(historyMapper.selectList(any())).thenReturn(List.of(record));

        ProfileServiceImpl service = createService();
        List<Map<String, Object>> result = service.getCompetencyHistory(1, 101, null);

        assertEquals(1, result.size());
        assertEquals("ap-1", result.get(0).get("abilityPointId"));
        assertEquals(50, result.get(0).get("oldScore"));
        assertEquals(52, result.get(0).get("newScore"));
    }

    // ============ getGrowthHistory ============

    @Test
    void getGrowthHistoryReturnsRecords() {
        GrowthHistory gh = new GrowthHistory();
        gh.setAmount(40);
        gh.setType("exp");
        gh.setSource("quiz");
        gh.setSourceId("task-1");
        gh.setCreatedAt(new Date());
        when(growthHistoryMapper.selectList(any())).thenReturn(List.of(gh));

        ProfileServiceImpl service = createService();
        List<Map<String, Object>> result = service.getGrowthHistory(1, 101);

        assertEquals(1, result.size());
        assertEquals(40, result.get(0).get("amount"));
        assertEquals("exp", result.get(0).get("type"));
    }

    // ============ getProfileSummary ============

    @Test
    void getProfileSummaryIncludesAbilityMap() {
        StudentProfile p = profile(1, 101, 100, 50, 50, 1, 100);
        when(profileMapper.selectOne(any())).thenReturn(p);
        lenient().when(abilityPointService.listByCourseCode(anyString())).thenReturn(List.of());
        when(competencyMapper.selectList(any())).thenReturn(List.of());

        ProfileServiceImpl service = createService();
        Map<String, Object> summary = service.getProfileSummary(1, 101);

        assertNotNull(summary.get("abilityMap"));
    }

    // ============ listCourseStudentProfiles ============

    @Test
    void listCourseStudentProfilesHandlesStudentWithoutProfile() {
        Student student = new Student();
        student.setStudentNo("2024001");
        student.setName("张三");
        when(studentMapper.selectList(any())).thenReturn(List.of(student));
        when(profileMapper.selectOne(any())).thenReturn(null);

        ProfileServiceImpl service = createService();
        List<Map<String, Object>> result = service.listCourseStudentProfiles(101);

        assertEquals(1, result.size());
        assertEquals("2024001", result.get(0).get("studentNo"));
        assertEquals(false, result.get(0).get("hasProfile"));
        assertEquals(0, result.get(0).get("exp"));
    }

    @Test
    void listCourseStudentProfilesHandlesNonParseableStudentNo() {
        Student student = new Student();
        student.setStudentNo("ABC-NAME");
        student.setName("特殊学号");
        when(studentMapper.selectList(any())).thenReturn(List.of(student));

        ProfileServiceImpl service = createService();
        List<Map<String, Object>> result = service.listCourseStudentProfiles(101);

        assertEquals(1, result.size());
        assertEquals("ABC-NAME", result.get(0).get("studentNo"));
        assertEquals(false, result.get(0).get("hasProfile"));
        assertEquals("学号暂不支持画像", result.get(0).get("status"));
    }

    // ============ towerMap fallback ============

    @Test
    void towerMapUsesFallbackWhenKnowledgePointServiceMissing() {
        when(profileMapper.selectOne(any())).thenReturn(profile(1, 101, 100, 50, 50, 1, 100));
        AbilityPoint ap = abilityPoint("ap-a", "101", "综合能力A");
        lenient().when(abilityPointService.listByCourseCode("101")).thenReturn(List.of(ap));
        lenient().when(competencyMapper.selectList(any())).thenReturn(List.of());

        ProfileServiceImpl service = createServiceWithoutKnowledgePointService();
        List<Map<String, Object>> floors = service.getTowerMap(1, 101);

        assertFalse(floors.isEmpty());
        assertEquals("ap-a", floors.get(0).get("kpId"));
    }

    // ============ profileInitializationUsesRealAbilityPoints (existing) ============

    @Test
    void profileInitializationUsesRealAbilityPoints() {
        AbilityPoint abilityPoint = new AbilityPoint();
        abilityPoint.setAbilityPointId("AP-REAL-1");
        abilityPoint.setCourseCode("101");
        abilityPoint.setName("真实能力点");
        abilityPoint.setDescription("来自模块1");
        when(profileMapper.selectOne(any())).thenReturn(null);
        when(abilityPointService.listByCourseCode("101")).thenReturn(List.of(abilityPoint));
        ProfileServiceImpl service = new ProfileServiceImpl(
                profileMapper, competencyMapper, historyMapper, growthHistoryMapper,
                achievementMapper, studentMapper, abilityPointService,
                new GrowthRuleEngine(), eventPublisher, gameConfigService);

        service.getOrCreateProfile(2024001, 101);

        ArgumentCaptor<CompetencyScore> captor = ArgumentCaptor.forClass(CompetencyScore.class);
        verify(competencyMapper).insert(captor.capture());
        CompetencyScore saved = captor.getValue();
        assertEquals(2024001, saved.getStudentNo());
        assertEquals(101, saved.getCourseCode());
        assertEquals("AP-REAL-1", saved.getAbilityPointId());
        assertEquals("真实能力点", saved.getAbilityPointName());
        assertEquals(50, saved.getScore());
    }

    @Test
    void towerMapUsesKnowledgePointStatusAndMasteryBeforeAbilityScore() {
        StudentProfile p = new StudentProfile();
        p.setStudentNo(1);
        p.setCourseCode(1);
        when(profileMapper.selectOne(any())).thenReturn(p);

        KnowledgePoint knowledgePoint = new KnowledgePoint();
        knowledgePoint.setKnowledgePointId("kp-list");
        knowledgePoint.setCourseCode("1");
        knowledgePoint.setName("列表与元组");
        knowledgePoint.setDescription("Python 序列基础");
        when(knowledgePointService.listByCourseCode("1", null)).thenReturn(List.of(knowledgePoint));

        AbilityPoint abilityPoint = new AbilityPoint();
        abilityPoint.setAbilityPointId("ap-data");
        abilityPoint.setCourseCode("1");
        abilityPoint.setName("数据结构基础");
        when(abilityPointService.listByCourseCode("1")).thenReturn(List.of(abilityPoint));

        AbilityKnowledgePoint mapping = new AbilityKnowledgePoint();
        mapping.setAbilityPointId("ap-data");
        mapping.setKnowledgePointId("kp-list");
        when(abilityKnowledgePointMapper.selectList(any())).thenReturn(List.of(mapping));

        CompetencyScore score = new CompetencyScore();
        score.setStudentNo(1);
        score.setCourseCode(1);
        score.setAbilityPointId("ap-data");
        score.setScore(88);
        when(competencyMapper.selectList(any())).thenReturn(List.of(score));

        KnowledgeMastery mastery = new KnowledgeMastery();
        mastery.setStudentNo("1");
        mastery.setCourseCode("1");
        mastery.setKnowledgePointId("kp-list");
        mastery.setMasteryScore(45);
        when(knowledgeMasteryMapper.selectList(any())).thenReturn(List.of(mastery));

        KnowledgePointFloorStatus status = new KnowledgePointFloorStatus();
        status.setStudentId("1");
        status.setCourseId("1");
        status.setKnowledgePointId("kp-list");
        status.setStatus("weak");
        when(floorStatusMapper.selectList(any())).thenReturn(List.of(status));

        ProfileServiceImpl service = new ProfileServiceImpl(
                profileMapper, competencyMapper, historyMapper, growthHistoryMapper,
                achievementMapper, studentMapper, abilityPointService,
                new GrowthRuleEngine(), eventPublisher, gameConfigService,
                floorStatusMapper, knowledgePointService,
                abilityKnowledgePointMapper, knowledgeMasteryMapper);

        List<Map<String, Object>> floors = service.getTowerMap(1, 1);
        Map<String, Object> floor = floors.get(0);

        assertEquals("kp-list", floor.get("kpId"));
        assertEquals("kp-list", floor.get("knowledgePointId"));
        assertEquals("weak", floor.get("floorStatus"));
        assertEquals(45, floor.get("masteryRate"));
        assertEquals("knowledge_mastery", floor.get("masterySource"));
        assertEquals("ap-data", floor.get("abilityPointId"));
        assertEquals("数据结构基础", floor.get("abilityPointName"));
    }

    // ============ helpers ============

    private ProfileServiceImpl createService() {
        return new ProfileServiceImpl(
                profileMapper, competencyMapper, historyMapper, growthHistoryMapper,
                achievementMapper, studentMapper, abilityPointService,
                new GrowthRuleEngine(), eventPublisher, gameConfigService,
                floorStatusMapper, knowledgePointService,
                abilityKnowledgePointMapper, knowledgeMasteryMapper);
    }

    private ProfileServiceImpl createServiceWithoutKnowledgePointService() {
        return new ProfileServiceImpl(
                profileMapper, competencyMapper, historyMapper, growthHistoryMapper,
                achievementMapper, studentMapper, abilityPointService,
                new GrowthRuleEngine(), eventPublisher, gameConfigService);
    }

    private StudentProfile profile(int studentNo, int courseCode, int hp, int atk, int def, int level, int exp) {
        StudentProfile p = new StudentProfile();
        p.setStudentNo(studentNo);
        p.setCourseCode(courseCode);
        p.setHp(hp);
        p.setAtk(atk);
        p.setDef(def);
        p.setLevel(level);
        p.setExp(exp);
        p.setCoins(100);
        p.setEnergy(5);
        p.setStatus("正常学习");
        p.setConsecutiveCorrect(0);
        p.setRecentAnswers("");
        p.setRecentScores("");
        p.setUpdatedAt(new Date());
        return p;
    }

    private AbilityPoint abilityPoint(String id, String courseCode, String name) {
        AbilityPoint ap = new AbilityPoint();
        ap.setAbilityPointId(id);
        ap.setCourseCode(courseCode);
        ap.setName(name);
        return ap;
    }
}
