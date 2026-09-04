package com.neu.CoursePlatform.profile;

import com.neu.CoursePlatform.common.GameEventTypes;
import com.neu.CoursePlatform.common.event.GameEvent;
import com.neu.CoursePlatform.service.LearningEvidenceService;
import com.neu.CoursePlatform.service.StudentAbilityProjectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:profile_projection_chain;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:profile-projection-chain-schema.sql",
        "app.performance-indexes.enabled=false"
})
class ProfileProjectionChainIntegrationTest {

    @Autowired
    private LearningEvidenceService evidenceService;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private StudentAbilityProjectionService abilityProjectionService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @BeforeEach
    void seedTrustedFacts() {
        jdbc.update("INSERT INTO student(student_no, name) VALUES ('1', '测试学生')");
        jdbc.update("""
                INSERT INTO course_game_config(id, course_id, game_mode_enabled, created_at, updated_at)
                VALUES ('cfg-1', '101', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
        jdbc.update("INSERT INTO knowledge_point(knowledge_point_id, course_code, name, importance) VALUES ('kp-1', '101', '对象基础', 2)");
        jdbc.update("INSERT INTO knowledge_point(knowledge_point_id, course_code, name, importance) VALUES ('kp-2', '101', '设计实践', 1)");
        jdbc.update("INSERT INTO ability_point(ability_point_id, course_code, name) VALUES ('ap-1', '101', '课程要点一')");
        jdbc.update("INSERT INTO ability_point(ability_point_id, course_code, name) VALUES ('ap-2', '101', '课程要点二')");
        jdbc.update("INSERT INTO ability_knowledge_point(id, ability_point_id, knowledge_point_id) VALUES ('ak-1', 'ap-1', 'kp-1')");
        jdbc.update("INSERT INTO ability_knowledge_point(id, ability_point_id, knowledge_point_id) VALUES ('ak-2', 'ap-2', 'kp-2')");
        jdbc.update("INSERT INTO competency_point(competency_id, course_code, name, status, sort_order) VALUES ('cp-1', '101', '问题解决', 'active', 1)");
        jdbc.update("INSERT INTO competency_point(competency_id, course_code, name, status, sort_order) VALUES ('cp-2', '101', '工程实践', 'active', 2)");
        jdbc.update("""
                INSERT INTO ability_point_competency_relation(
                    id, course_code, ability_point_id, competency_id, relation_status,
                    strength, confidence, strength_source, evidence_count, matrix_version, updated_at)
                VALUES ('rel-1', '101', 'ap-1', 'cp-1', 'related', 0.7, 0.8, 'teacher_prior', 10, 'v1', CURRENT_TIMESTAMP)
                """);
        jdbc.update("""
                INSERT INTO ability_point_competency_relation(
                    id, course_code, ability_point_id, competency_id, relation_status,
                    strength, confidence, strength_source, evidence_count, matrix_version, updated_at)
                VALUES ('rel-2', '101', 'ap-2', 'cp-1', 'related', 0.3, 0.6, 'teacher_prior', 10, 'v1', CURRENT_TIMESTAMP)
                """);
        jdbc.update("""
                INSERT INTO ability_point_competency_relation(
                    id, course_code, ability_point_id, competency_id, relation_status,
                    strength, confidence, strength_source, evidence_count, matrix_version, updated_at)
                VALUES ('rel-3', '101', 'ap-2', 'cp-2', 'related', 1.0, 0.7, 'teacher_prior', 10, 'v1', CURRENT_TIMESTAMP)
                """);
        for (int i = 1; i <= 10; i++) {
            jdbc.update("""
                    INSERT INTO question(question_id, course_code, type, stem, answer, difficulty, knowledge_point_id, score)
                    VALUES (?, '101', 'fill', ?, '正确', 2, 'kp-1', 10)
                    """, "q-" + i, "题目" + i);
        }
    }

    @Test
    void verifiedAnswersProjectProfileAndAwardBadgeExactlyOnce() {
        List<Map<String, Object>> answers = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            answers.add(Map.of("questionId", "q-" + i, "studentAnswer", "正确"));
        }
        Set<String> questionIds = answers.stream()
                .map(answer -> String.valueOf(answer.get("questionId")))
                .collect(java.util.stream.Collectors.toSet());

        LearningEvidenceService.BatchResult first = evidenceService.recordVerifiedAnswers(
                "1", "101", "evaluation-1", "quiz", answers, questionIds);

        assertThat(first.gradedCount()).isEqualTo(10);
        assertThat(first.correctCount()).isEqualTo(10);
        assertThat(first.answers()).allMatch(LearningEvidenceService.AnswerResult::applied);
        assertThat(count("learning_answer_evidence")).isEqualTo(10);
        assertThat(count("profile_projection_ledger")).isEqualTo(10);
        assertThat(count("knowledge_mastery_history")).isEqualTo(10);
        assertThat(count("growth_history")).isEqualTo(20);

        Map<String, Object> profile = jdbc.queryForMap("""
                SELECT hp, atk, exp, level, coins, consecutive_correct, recent_answers
                FROM student_profile WHERE student_no = '1' AND course_code = '101'
                """);
        assertThat(((Number) profile.get("hp")).intValue()).isEqualTo(100);
        assertThat(((Number) profile.get("atk")).intValue()).isEqualTo(100);
        assertThat(((Number) profile.get("exp")).intValue()).isEqualTo(100);
        assertThat(((Number) profile.get("level")).intValue()).isEqualTo(1);
        assertThat(((Number) profile.get("coins")).intValue()).isEqualTo(200);
        assertThat(((Number) profile.get("consecutive_correct")).intValue()).isEqualTo(10);
        assertThat(String.valueOf(profile.get("recent_answers")).split(",")).hasSize(10);

        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM achievement
                WHERE student_no = '1' AND course_code = '101' AND badge_code = 'combo_10'
                """, Integer.class)).isEqualTo(1);

        int mastery = jdbc.queryForObject("""
                SELECT mastery_score FROM knowledge_mastery
                WHERE student_no = '1' AND course_code = '101' AND knowledge_point_id = 'kp-1'
                """, Integer.class);
        List<Map<String, Object>> coursePoints = abilityProjectionService.coursePoints("1", "101");
        assertThat(coursePoints).hasSize(2);
        Map<String, Object> evidencedCoursePoint = coursePoints.stream()
                .filter(item -> "ap-1".equals(item.get("abilityPointId"))).findFirst().orElseThrow();
        Map<String, Object> emptyCoursePoint = coursePoints.stream()
                .filter(item -> "ap-2".equals(item.get("abilityPointId"))).findFirst().orElseThrow();
        assertThat(((Number) evidencedCoursePoint.get("score")).intValue()).isEqualTo(mastery);
        assertThat(evidencedCoursePoint.get("formulaVersion")).isEqualTo("knowledge_mastery_weighted_v1");
        assertThat(emptyCoursePoint.get("score")).isNull();
        assertThat(emptyCoursePoint.get("hasEvidence")).isEqualTo(false);

        List<Map<String, Object>> trueCompetencies = abilityProjectionService.trueCompetencies("1", "101");
        Map<String, Object> projectedCompetency = trueCompetencies.stream()
                .filter(item -> "cp-1".equals(item.get("competencyId"))).findFirst().orElseThrow();
        Map<String, Object> insufficientCompetency = trueCompetencies.stream()
                .filter(item -> "cp-2".equals(item.get("competencyId"))).findFirst().orElseThrow();
        assertThat(((Number) projectedCompetency.get("score")).intValue()).isEqualTo(mastery);
        assertThat(projectedCompetency.get("coverage")).isEqualTo("1/2");
        assertThat(((Number) projectedCompetency.get("coverageRate")).doubleValue()).isEqualTo(0.7D);
        assertThat(projectedCompetency.get("matrixVersion")).isEqualTo("v1");
        assertThat(insufficientCompetency.get("score")).isNull();
        assertThat(insufficientCompetency.get("status")).isEqualTo("evidence_insufficient");

        LearningEvidenceService.BatchResult replay = evidenceService.recordVerifiedAnswers(
                "1", "101", "evaluation-1", "quiz", answers, questionIds);

        assertThat(replay.answers()).noneMatch(LearningEvidenceService.AnswerResult::applied);
        assertThat(count("learning_answer_evidence")).isEqualTo(10);
        assertThat(count("profile_projection_ledger")).isEqualTo(10);
        assertThat(count("growth_history")).isEqualTo(20);
        assertThat(jdbc.queryForObject("SELECT exp FROM student_profile WHERE student_no = '1'", Integer.class))
                .isEqualTo(100);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM achievement WHERE badge_code = 'combo_10'", Integer.class))
                .isEqualTo(1);

        String badgeMetadata = jdbc.queryForObject(
                "SELECT metadata FROM achievement WHERE badge_code = 'combo_10'", String.class);
        assertThat(badgeMetadata).contains("trusted_facts_v1", "\"consecutiveCorrect\":10");

        jdbc.update("""
                INSERT INTO student_tower_attempt(
                    attempt_id, run_id, node_id, student_no, course_code, room_type, result,
                    correct_rate, hp_left, answer_summary_json, started_at, finished_at)
                VALUES ('tower-attempt-1', 'run-1', 'node-1', '1', '101', 'elite', 'cleared',
                    1.0, 80, '[]', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
        GameEvent towerEvent = GameEvent.builder()
                .eventId("event-1").eventType(GameEventTypes.ELITE_DEFEATED)
                .studentId("1").courseId("101").sourceId("tower-attempt-1")
                .occurredAt(LocalDateTime.now()).payload(Map.of()).build();
        eventPublisher.publishEvent(towerEvent);
        eventPublisher.publishEvent(towerEvent);

        assertThat(jdbc.queryForObject("SELECT exp FROM student_profile WHERE student_no = '1'", Integer.class))
                .isEqualTo(220);
        assertThat(jdbc.queryForObject("SELECT coins FROM student_profile WHERE student_no = '1'", Integer.class))
                .isEqualTo(240);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM profile_projection_ledger
                WHERE source_type = 'tower_attempt' AND source_id = 'tower-attempt-1'
                """, Integer.class)).isEqualTo(1);

        GameEvent unrelatedEvent = GameEvent.builder()
                .eventId("event-2").eventType(GameEventTypes.BOSS_DEFEATED)
                .studentId("1").courseId("101").sourceId("task-submission-1")
                .occurredAt(LocalDateTime.now()).payload(Map.of("exp", 999999)).build();
        eventPublisher.publishEvent(unrelatedEvent);
        assertThat(jdbc.queryForObject("SELECT exp FROM student_profile WHERE student_no = '1'", Integer.class))
                .isEqualTo(220);
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }
}
