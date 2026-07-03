package com.neu.CoursePlatform.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.neu.CoursePlatform.entity.AbilityKnowledgePoint;
import com.neu.CoursePlatform.entity.Question;
import com.neu.CoursePlatform.entity.StudentTowerAttempt;
import com.neu.CoursePlatform.entity.StudentTowerNode;
import com.neu.CoursePlatform.entity.StudentTowerQuestionPack;
import com.neu.CoursePlatform.entity.StudentTowerRun;
import com.neu.CoursePlatform.mapper.AbilityKnowledgePointMapper;
import com.neu.CoursePlatform.mapper.StudentTowerAttemptMapper;
import com.neu.CoursePlatform.mapper.StudentTowerNodeMapper;
import com.neu.CoursePlatform.mapper.StudentTowerQuestionPackMapper;
import com.neu.CoursePlatform.mapper.StudentTowerRunMapper;
import com.neu.CoursePlatform.service.QuestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

class TowerQuestionPackServiceImplTest {

    private StudentTowerQuestionPackMapper packMapper;
    private StudentTowerRunMapper runMapper;
    private StudentTowerNodeMapper nodeMapper;
    private StudentTowerAttemptMapper attemptMapper;
    private AbilityKnowledgePointMapper abilityKnowledgePointMapper;
    private QuestionService questionService;
    private TowerQuestionPackServiceImpl service;

    @BeforeEach
    void setUp() {
        packMapper = mock(StudentTowerQuestionPackMapper.class);
        runMapper = mock(StudentTowerRunMapper.class);
        nodeMapper = mock(StudentTowerNodeMapper.class);
        attemptMapper = mock(StudentTowerAttemptMapper.class);
        abilityKnowledgePointMapper = mock(AbilityKnowledgePointMapper.class);
        questionService = mock(QuestionService.class);
        service = new TowerQuestionPackServiceImpl(packMapper, runMapper, nodeMapper, attemptMapper,
                abilityKnowledgePointMapper, questionService);
    }

    @Test
    void getOrCreateQuestionPackReusesCurrentStrategyPack() {
        when(runMapper.selectById("run-1")).thenReturn(run("run-1", "1001", "101"));
        when(nodeMapper.selectById("node-1")).thenReturn(node("node-1", "run-1", "battle", "kp-1", 2));
        StudentTowerQuestionPack existing = pack("pack-1", "run-1", "node-1", "battle",
                "[\"q1\",\"q2\",\"q3\",\"q4\",\"q5\"]",
                "{\"strategyVersion\":2,\"targetCount\":5}");
        when(packMapper.selectOne(any())).thenReturn(existing);
        when(questionService.getById(anyString())).thenAnswer(inv -> question(inv.getArgument(0), "kp-1", 2, "single"));

        Map<String, Object> result = service.getOrCreateQuestionPack("1001", "run-1", "node-1", "battle");

        assertEquals("pack-1", result.get("packId"));
        assertEquals("battle", result.get("mode"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> questions = (List<Map<String, Object>>) result.get("questions");
        assertEquals(5, questions.size());
        verify(packMapper, never()).insert(any(StudentTowerQuestionPack.class));
        verify(packMapper, never()).delete(any());
    }

    @Test
    void regenerateQuestionPackSelectsLayeredCandidatesAndPersistsStrategy() {
        when(runMapper.selectById("run-1")).thenReturn(run("run-1", "1001", "101"));
        when(nodeMapper.selectById("node-1")).thenReturn(node("node-1", "run-1", "elite", "kp-1", 3));
        when(packMapper.selectList(any())).thenReturn(List.of(pack("old", "run-1", "other", "battle",
                "[\"q-old\"]", "{\"strategyVersion\":2}")));
        StudentTowerAttempt attempt = new StudentTowerAttempt();
        attempt.setAnswerSummaryJson("[{\"questionId\":\"q-recent\"}]");
        attempt.setFinishedAt(LocalDateTime.now());
        when(attemptMapper.selectList(any())).thenReturn(List.of(attempt));
        when(abilityKnowledgePointMapper.selectList(any()))
                .thenReturn(List.of(mapping("a-1", "kp-1")))
                .thenReturn(List.of(mapping("a-1", "kp-1"), mapping("a-1", "kp-2")));
        when(questionService.filterQuestions(eq("101"), isNull(), eq("kp-1"), isNull(), isNull(), isNull()))
                .thenReturn(List.of(
                        question("q1", "kp-1", 3, "single"),
                        question("q2", "kp-1", 2, "multi"),
                        question("q3", "kp-1", 4, "fill")));
        when(questionService.filterQuestions(eq("101"), isNull(), eq("kp-2"), isNull(), isNull(), isNull()))
                .thenReturn(List.of(
                        question("q4", "kp-2", 3, "essay"),
                        question("q5", "kp-2", 4, "program"),
                        question("q6", "kp-2", 2, "single")));
        when(questionService.listByCourseCode("101")).thenReturn(List.of(
                question("q7", "kp-3", 3, "single"),
                question("q8", "kp-3", 5, "multi")));

        Map<String, Object> result = service.regenerateQuestionPack("1001", "run-1", "node-1", "elite");

        assertEquals("elite", result.get("mode"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> questions = (List<Map<String, Object>>) result.get("questions");
        assertEquals(6, questions.size());
        ArgumentCaptor<StudentTowerQuestionPack> captor = ArgumentCaptor.forClass(StudentTowerQuestionPack.class);
        verify(packMapper).delete(any());
        verify(packMapper).insert(captor.capture());
        assertEquals("rule", captor.getValue().getSource());
        assertTrue(captor.getValue().getStrategyJson().contains("\"targetCount\":6"));
        assertTrue(captor.getValue().getQuestionIdsJson().contains("q1"));
    }

    @Test
    void regenerateQuestionPackThrowsWhenNoCandidates() {
        when(runMapper.selectById("run-1")).thenReturn(run("run-1", "1001", "101"));
        when(nodeMapper.selectById("node-1")).thenReturn(node("node-1", "run-1", "battle", "kp-1", 1));
        when(packMapper.selectList(any())).thenReturn(List.of());
        when(attemptMapper.selectList(any())).thenReturn(List.of());
        when(abilityKnowledgePointMapper.selectList(any())).thenReturn(List.of());
        when(questionService.filterQuestions(any(), any(), any(), any(), any(), any())).thenReturn(List.of());
        when(questionService.listByCourseCode("101")).thenReturn(List.of());

        assertThrows(IllegalStateException.class,
                () -> service.regenerateQuestionPack("1001", "run-1", "node-1", "battle"));
    }

    private StudentTowerRun run(String id, String studentNo, String courseCode) {
        StudentTowerRun run = new StudentTowerRun();
        run.setRunId(id);
        run.setStudentNo(studentNo);
        run.setCourseCode(courseCode);
        run.setStatus("active");
        return run;
    }

    private StudentTowerNode node(String id, String runId, String roomType, String kpId, int difficulty) {
        StudentTowerNode node = new StudentTowerNode();
        node.setNodeId(id);
        node.setRunId(runId);
        node.setRoomType(roomType);
        node.setKnowledgePointId(kpId);
        node.setDifficulty(difficulty);
        node.setStatus("available");
        return node;
    }

    private StudentTowerQuestionPack pack(String id, String runId, String nodeId, String mode,
                                          String idsJson, String strategyJson) {
        StudentTowerQuestionPack pack = new StudentTowerQuestionPack();
        pack.setPackId(id);
        pack.setRunId(runId);
        pack.setNodeId(nodeId);
        pack.setStudentNo("1001");
        pack.setCourseCode("101");
        pack.setMode(mode);
        pack.setQuestionIdsJson(idsJson);
        pack.setStrategyJson(strategyJson);
        pack.setSource("rule");
        return pack;
    }

    private Question question(String id, String kpId, int difficulty, String type) {
        Question q = new Question();
        q.setQuestionId(id);
        q.setCourseCode("101");
        q.setKnowledgePointId(kpId);
        q.setDifficulty(difficulty);
        q.setType(type);
        q.setStem("stem-" + id);
        q.setAnswer("A");
        q.setScore(5);
        return q;
    }

    private AbilityKnowledgePoint mapping(String abilityId, String kpId) {
        AbilityKnowledgePoint mapping = new AbilityKnowledgePoint();
        mapping.setAbilityPointId(abilityId);
        mapping.setKnowledgePointId(kpId);
        return mapping;
    }
}
