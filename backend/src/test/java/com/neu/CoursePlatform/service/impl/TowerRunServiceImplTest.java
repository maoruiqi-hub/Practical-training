package com.neu.CoursePlatform.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.neu.CoursePlatform.agentic.AgenticClient;
import com.neu.CoursePlatform.agentic.AgenticResponse;
import com.neu.CoursePlatform.common.event.GameEvent;
import com.neu.CoursePlatform.common.event.GameEventPublisher;
import com.neu.CoursePlatform.entity.AbilityKnowledgePoint;
import com.neu.CoursePlatform.entity.AbilityPoint;
import com.neu.CoursePlatform.entity.KnowledgeMastery;
import com.neu.CoursePlatform.entity.KnowledgePoint;
import com.neu.CoursePlatform.entity.StudentAbilityDeltaLog;
import com.neu.CoursePlatform.entity.StudentTowerAttempt;
import com.neu.CoursePlatform.entity.StudentTowerNode;
import com.neu.CoursePlatform.entity.StudentTowerRun;
import com.neu.CoursePlatform.mapper.AbilityKnowledgePointMapper;
import com.neu.CoursePlatform.mapper.KnowledgeMasteryMapper;
import com.neu.CoursePlatform.mapper.StudentAbilityDeltaLogMapper;
import com.neu.CoursePlatform.mapper.StudentTowerAttemptMapper;
import com.neu.CoursePlatform.mapper.StudentTowerNodeMapper;
import com.neu.CoursePlatform.mapper.StudentTowerRunMapper;
import com.neu.CoursePlatform.profile.entity.CompetencyScore;
import com.neu.CoursePlatform.profile.service.ProfileService;
import com.neu.CoursePlatform.service.AbilityPointService;
import com.neu.CoursePlatform.service.FloorProgressService;
import com.neu.CoursePlatform.service.KnowledgePointService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class TowerRunServiceImplTest {

    private StudentTowerRunMapper runMapper;
    private StudentTowerNodeMapper nodeMapper;
    private StudentTowerAttemptMapper attemptMapper;
    private StudentAbilityDeltaLogMapper deltaMapper;
    private KnowledgePointService knowledgePointService;
    private AbilityPointService abilityPointService;
    private AbilityKnowledgePointMapper abilityKnowledgePointMapper;
    private KnowledgeMasteryMapper knowledgeMasteryMapper;
    private ProfileService profileService;
    private FloorProgressService floorProgressService;
    private GameEventPublisher eventPublisher;
    private AgenticClient agenticClient;
    private TowerRunServiceImpl service;
    private final Map<String, StudentTowerRun> runs = new LinkedHashMap<>();
    private final Map<String, StudentTowerNode> nodes = new LinkedHashMap<>();
    private final List<StudentTowerAttempt> attempts = new ArrayList<>();
    private final List<StudentAbilityDeltaLog> deltas = new ArrayList<>();

    @BeforeEach
    void setUp() {
        runMapper = mock(StudentTowerRunMapper.class);
        nodeMapper = mock(StudentTowerNodeMapper.class);
        attemptMapper = mock(StudentTowerAttemptMapper.class);
        deltaMapper = mock(StudentAbilityDeltaLogMapper.class);
        knowledgePointService = mock(KnowledgePointService.class);
        abilityPointService = mock(AbilityPointService.class);
        abilityKnowledgePointMapper = mock(AbilityKnowledgePointMapper.class);
        knowledgeMasteryMapper = mock(KnowledgeMasteryMapper.class);
        profileService = mock(ProfileService.class);
        floorProgressService = mock(FloorProgressService.class);
        eventPublisher = mock(GameEventPublisher.class);
        agenticClient = mock(AgenticClient.class);
        service = new TowerRunServiceImpl(runMapper, nodeMapper, attemptMapper, deltaMapper,
                knowledgePointService, abilityPointService, abilityKnowledgePointMapper, knowledgeMasteryMapper,
                profileService, floorProgressService, eventPublisher, agenticClient);

        when(runMapper.selectById(anyString())).thenAnswer(inv -> runs.get(inv.getArgument(0)));
        when(runMapper.insert(any(StudentTowerRun.class))).thenAnswer(inv -> {
            StudentTowerRun run = inv.getArgument(0);
            runs.put(run.getRunId(), run);
            return 1;
        });
        when(runMapper.updateById(any(StudentTowerRun.class))).thenAnswer(inv -> {
            StudentTowerRun run = inv.getArgument(0);
            runs.put(run.getRunId(), run);
            return 1;
        });
        when(runMapper.selectOne(any())).thenAnswer(inv -> runs.values().stream()
                .filter(run -> "active".equals(run.getStatus()))
                .findFirst().orElse(null));
        when(runMapper.selectCount(any())).thenReturn(0L);

        when(nodeMapper.selectById(anyString())).thenAnswer(inv -> nodes.get(inv.getArgument(0)));
        when(nodeMapper.insert(any(StudentTowerNode.class))).thenAnswer(inv -> {
            StudentTowerNode node = inv.getArgument(0);
            nodes.put(node.getNodeId(), node);
            return 1;
        });
        when(nodeMapper.updateById(any(StudentTowerNode.class))).thenAnswer(inv -> {
            StudentTowerNode node = inv.getArgument(0);
            nodes.put(node.getNodeId(), node);
            return 1;
        });
        when(nodeMapper.selectList(any())).thenAnswer(inv -> new ArrayList<>(nodes.values()));

        when(attemptMapper.insert(any(StudentTowerAttempt.class))).thenAnswer(inv -> {
            attempts.add(inv.getArgument(0));
            return 1;
        });
        when(deltaMapper.insert(any(StudentAbilityDeltaLog.class))).thenAnswer(inv -> {
            deltas.add(inv.getArgument(0));
            return 1;
        });
        when(deltaMapper.selectList(any())).thenAnswer(inv -> new ArrayList<>(deltas));

        when(knowledgePointService.listByCourseCode("101", null)).thenReturn(List.of(
                kp("kp-1", "循环", 5),
                kp("kp-2", "函数", 3),
                kp("kp-3", "文件", 1)));
        when(abilityPointService.listByCourseCode("101")).thenReturn(List.of(ability("ab-1", "逻辑能力")));
        when(abilityKnowledgePointMapper.selectList(any())).thenReturn(List.of(
                mapping("ab-1", "kp-1"),
                mapping("ab-1", "kp-2"),
                mapping("ab-1", "kp-3")));
        when(knowledgeMasteryMapper.selectList(any())).thenReturn(List.of(mastery("kp-1", 30), mastery("kp-2", 75)));
        when(profileService.getCompetencyScores(anyInt(), anyInt())).thenReturn(List.of(score("ab-1", 60)));
        when(agenticClient.isMockMode()).thenReturn(false);
        when(agenticClient.isConfiguredForRealAi()).thenReturn(false);
        when(agenticClient.configurationMessage()).thenReturn("AI未配置");
    }

    @Test
    void generateRunBuildsFallbackRouteAndGetOrCreateReusesActiveRun() {
        Map<String, Object> result = service.generateRun("1001", "101", false);

        assertEquals("active", result.get("status"));
        assertNotNull(result.get("currentNodeId"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> nodeDtos = (List<Map<String, Object>>) result.get("nodes");
        assertTrue(nodeDtos.size() >= 3);
        assertTrue(nodeDtos.stream().anyMatch(node -> "diagnosis".equals(node.get("roomType"))));
        assertTrue(nodeDtos.stream().anyMatch(node -> "boss".equals(node.get("roomType"))));

        Map<String, Object> reused = service.getOrCreateActiveRun("1001", "101");
        assertEquals(result.get("runId"), reused.get("runId"));
        verify(runMapper).insert(any(StudentTowerRun.class));
    }

    @Test
    void enterNodeRejectsLockedNodeAndAllowsAvailableNode() {
        StudentTowerRun run = storedRun();
        StudentTowerNode available = storedNode(run.getRunId(), "battle", "available", "kp-1", "ab-1", 1);
        StudentTowerNode locked = storedNode(run.getRunId(), "battle", "locked", "kp-2", "ab-1", 2);

        Map<String, Object> entered = service.enterNode("1001", run.getRunId(), available.getNodeId());

        assertEquals(available.getNodeId(), ((Map<?, ?>) entered.get("node")).get("nodeId"));
        assertEquals(available.getNodeId(), run.getCurrentNodeId());
        assertThrows(IllegalStateException.class, () -> service.enterNode("1001", run.getRunId(), locked.getNodeId()));
    }

    @Test
    void completeNodeClearsBattleUnlocksChildRecordsAttemptDeltaAndEvent() {
        StudentTowerRun run = storedRun();
        StudentTowerNode battle = storedNode(run.getRunId(), "battle", "available", "kp-1", "ab-1", 2);
        StudentTowerNode child = storedNode(run.getRunId(), "rest", "locked", "kp-1", "ab-1", 1);
        child.setUnlockAfterNodeId(battle.getNodeId());
        Map<String, Object> answer = Map.of(
                "source", "battle_room",
                "type", "single",
                "answered", true,
                "autoGradable", true,
                "correct", true,
                "studentAnswer", "A",
                "questionId", "q1");

        Map<String, Object> result = service.completeNode("1001", run.getRunId(), battle.getNodeId(),
                Map.of("result", "cleared", "hpLeft", 7, "answerSummary", List.of(answer)));

        assertEquals(1.0D, (Double) result.get("correctRate"), 0.0001);
        assertEquals("cleared", battle.getStatus());
        assertEquals("available", child.getStatus());
        assertEquals(1, attempts.size());
        assertEquals(1, deltas.size());
        assertTrue(deltas.get(0).getDeltaScore() > 0);
        verify(floorProgressService).updateFloorStatus("1001", "101", "kp-1", "cleared");
        verify(eventPublisher).publish(any(GameEvent.class));
    }

    @Test
    void completeNodeMarksBattleFailedWhenNotCleared() {
        StudentTowerRun run = storedRun();
        StudentTowerNode battle = storedNode(run.getRunId(), "elite", "available", "kp-1", "ab-1", 2);
        Map<String, Object> answer = Map.of(
                "source", "elite_room",
                "type", "single",
                "answered", true,
                "autoGradable", true,
                "correct", false,
                "studentAnswer", "B");

        Map<String, Object> result = service.completeNode("1001", run.getRunId(), battle.getNodeId(),
                Map.of("result", "failed", "hp_left", 0, "answers", List.of(answer)));

        assertEquals("failed", battle.getStatus());
        assertEquals(0.0D, (Double) result.get("correctRate"), 0.0001);
        assertTrue(deltas.get(0).getDeltaScore() < 0);
        verify(floorProgressService).updateFloorStatus("1001", "101", "kp-1", "weak");
    }

    @Test
    void diagnoseNodePerfectClearsBoundEliteAndReturnsBypassPayload() {
        StudentTowerRun run = storedRun();
        StudentTowerNode diagnosis = storedNode(run.getRunId(), "diagnosis", "available", "kp-1", "ab-1", 1);
        StudentTowerNode elite = storedNode(run.getRunId(), "elite", "locked", "kp-1", "ab-1", 2);
        diagnosis.setParentNodeId(elite.getNodeId());
        Map<String, Object> answer = Map.of(
                "source", "diagnosis_room",
                "type", "fill",
                "answered", true,
                "autoGradable", true,
                "correct", true,
                "studentAnswer", "range(5)");

        Map<String, Object> result = service.diagnoseNode("1001", run.getRunId(), diagnosis.getNodeId(),
                Map.of("answers", List.of(answer)));

        assertEquals("perfect", result.get("status"));
        assertEquals(true, result.get("eliteBypassed"));
        assertEquals("cleared", diagnosis.getStatus());
        assertEquals("cleared", elite.getStatus());
        assertEquals(2, attempts.size());
    }

    @Test
    void diagnoseNodePartialUnlocksEliteForPractice() {
        StudentTowerRun run = storedRun();
        StudentTowerNode diagnosis = storedNode(run.getRunId(), "diagnosis", "available", "kp-1", "ab-1", 1);
        StudentTowerNode elite = storedNode(run.getRunId(), "elite", "locked", "kp-1", "ab-1", 2);
        diagnosis.setParentNodeId(elite.getNodeId());
        Map<String, Object> answer = Map.of(
                "source", "diagnosis_room",
                "type", "single",
                "answered", true,
                "autoGradable", true,
                "correct", false,
                "studentAnswer", "B");

        Map<String, Object> result = service.diagnoseNode("1001", run.getRunId(), diagnosis.getNodeId(),
                Map.of("answers", List.of(answer)));

        assertEquals("weak", result.get("status"));
        assertEquals(elite.getNodeId(), result.get("nextNodeId"));
        assertEquals("available", elite.getStatus());
    }

    @Test
    void getAbilityDeltasMapsRecentDeltaLogs() {
        StudentAbilityDeltaLog log = new StudentAbilityDeltaLog();
        log.setId("delta-1");
        log.setRunId("run-1");
        log.setNodeId("node-1");
        log.setKnowledgePointId("kp-1");
        log.setAbilityPointId("ab-1");
        log.setDeltaScore(7);
        log.setBeforeScore(60);
        log.setAfterScore(67);
        log.setReason("通关");
        log.setAiSummary("能力提升");
        log.setCreatedAt(LocalDateTime.now());
        deltas.add(log);

        List<Map<String, Object>> result = service.getAbilityDeltas("1001", "101", "run-1");

        assertEquals(1, result.size());
        assertEquals("delta-1", result.get(0).get("id"));
        assertEquals(7, result.get(0).get("deltaScore"));
    }

    @Test
    void completeNodeUsesAiSummaryWhenRealAiReturnsText() {
        when(agenticClient.invoke(eq("ability-growth-explain"), any()))
                .thenReturn(new AgenticResponse(true, Map.of("summary", "AI总结"), "ok"));
        StudentTowerRun run = storedRun();
        StudentTowerNode boss = storedNode(run.getRunId(), "boss", "available", "kp-1", "ab-1", 3);
        Map<String, Object> answer = Map.of(
                "source", "boss_room",
                "type", "single",
                "answered", true,
                "autoGradable", true,
                "correct", true,
                "studentAnswer", "A");

        service.completeNode("1001", run.getRunId(), boss.getNodeId(),
                Map.of("result", "cleared", "answers", List.of(answer)));

        assertEquals("AI总结", deltas.get(0).getAiSummary());
        ArgumentCaptor<GameEvent> eventCaptor = ArgumentCaptor.forClass(GameEvent.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        assertEquals("boss_defeated", eventCaptor.getValue().getEventType());
    }

    private StudentTowerRun storedRun() {
        StudentTowerRun run = new StudentTowerRun();
        run.setRunId("run-" + (runs.size() + 1));
        run.setStudentNo("1001");
        run.setCourseCode("101");
        run.setStatus("active");
        run.setRouteSource("template");
        run.setCurrentNodeId("node-1");
        runs.put(run.getRunId(), run);
        return run;
    }

    private StudentTowerNode storedNode(String runId, String roomType, String status, String kpId, String abilityId, int difficulty) {
        StudentTowerNode node = new StudentTowerNode();
        node.setNodeId("node-" + (nodes.size() + 1));
        node.setRunId(runId);
        node.setNodeOrder(nodes.size() + 1);
        node.setRowNo(nodes.size() + 1);
        node.setColNo(1);
        node.setRoomType(roomType);
        node.setStatus(status);
        node.setKnowledgePointId(kpId);
        node.setAbilityPointId(abilityId);
        node.setDifficulty(difficulty);
        nodes.put(node.getNodeId(), node);
        return node;
    }

    private KnowledgePoint kp(String id, String name, int importance) {
        KnowledgePoint kp = new KnowledgePoint();
        kp.setKnowledgePointId(id);
        kp.setCourseCode("101");
        kp.setName(name);
        kp.setDescription(name + "描述");
        kp.setImportance(importance);
        return kp;
    }

    private AbilityPoint ability(String id, String name) {
        AbilityPoint ability = new AbilityPoint();
        ability.setAbilityPointId(id);
        ability.setCourseCode("101");
        ability.setName(name);
        return ability;
    }

    private AbilityKnowledgePoint mapping(String abilityId, String kpId) {
        AbilityKnowledgePoint mapping = new AbilityKnowledgePoint();
        mapping.setAbilityPointId(abilityId);
        mapping.setKnowledgePointId(kpId);
        return mapping;
    }

    private KnowledgeMastery mastery(String kpId, int score) {
        KnowledgeMastery mastery = new KnowledgeMastery();
        mastery.setStudentNo("1001");
        mastery.setCourseCode("101");
        mastery.setKnowledgePointId(kpId);
        mastery.setMasteryScore(score);
        return mastery;
    }

    private CompetencyScore score(String abilityId, int score) {
        CompetencyScore competencyScore = new CompetencyScore();
        competencyScore.setAbilityPointId(abilityId);
        competencyScore.setScore(score);
        return competencyScore;
    }
}
