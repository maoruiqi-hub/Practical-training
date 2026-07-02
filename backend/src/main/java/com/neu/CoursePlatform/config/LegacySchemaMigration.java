package com.neu.CoursePlatform.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

@Component
public class LegacySchemaMigration implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(LegacySchemaMigration.class);

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public LegacySchemaMigration(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        createTowerRunTables();
        migrateKnowledgeRelations();
        migrateExams();
    }

    private void createTowerRunTables() {
        try {
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS student_tower_run (
                        run_id VARCHAR(64) PRIMARY KEY,
                        student_no VARCHAR(64) NOT NULL,
                        course_code VARCHAR(64) NOT NULL,
                        version INTEGER DEFAULT 1,
                        status VARCHAR(32) DEFAULT 'active',
                        route_source VARCHAR(32) DEFAULT 'rule',
                        current_node_id VARCHAR(64),
                        ai_snapshot_json TEXT,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
            jdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS idx_tower_run_student_course
                    ON student_tower_run(student_no, course_code, status)
                    """);
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS student_tower_node (
                        node_id VARCHAR(64) PRIMARY KEY,
                        run_id VARCHAR(64) NOT NULL,
                        node_order INTEGER NOT NULL,
                        row_no INTEGER NOT NULL,
                        col_no INTEGER NOT NULL,
                        room_type VARCHAR(32) NOT NULL,
                        status VARCHAR(32) DEFAULT 'locked',
                        knowledge_point_id VARCHAR(64),
                        ability_point_id VARCHAR(64),
                        parent_node_id VARCHAR(64),
                        unlock_after_node_id VARCHAR(64),
                        difficulty INTEGER DEFAULT 1,
                        ai_reason TEXT,
                        payload_json TEXT,
                        cleared_at TIMESTAMP,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
            jdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS idx_tower_node_run_order
                    ON student_tower_node(run_id, node_order)
                    """);
            jdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS idx_tower_node_unlock
                    ON student_tower_node(run_id, unlock_after_node_id)
                    """);
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS student_tower_attempt (
                        attempt_id VARCHAR(64) PRIMARY KEY,
                        run_id VARCHAR(64) NOT NULL,
                        node_id VARCHAR(64) NOT NULL,
                        student_no VARCHAR(64) NOT NULL,
                        course_code VARCHAR(64) NOT NULL,
                        room_type VARCHAR(32) NOT NULL,
                        result VARCHAR(32) NOT NULL,
                        correct_rate NUMERIC(5,2),
                        hp_left INTEGER,
                        answer_summary_json TEXT,
                        ai_report_json TEXT,
                        started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        finished_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
            jdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS idx_tower_attempt_node
                    ON student_tower_attempt(node_id, finished_at)
                    """);
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS student_ability_delta_log (
                        id VARCHAR(64) PRIMARY KEY,
                        student_no VARCHAR(64) NOT NULL,
                        course_code VARCHAR(64) NOT NULL,
                        run_id VARCHAR(64),
                        node_id VARCHAR(64),
                        knowledge_point_id VARCHAR(64),
                        ability_point_id VARCHAR(64),
                        delta_score INTEGER DEFAULT 0,
                        before_score INTEGER,
                        after_score INTEGER,
                        reason TEXT,
                        ai_summary TEXT,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
            jdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS idx_ability_delta_student_course
                    ON student_ability_delta_log(student_no, course_code, created_at)
                    """);
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS student_tower_question_pack (
                        pack_id VARCHAR(64) PRIMARY KEY,
                        run_id VARCHAR(64) NOT NULL,
                        node_id VARCHAR(64) NOT NULL,
                        student_no VARCHAR(64) NOT NULL,
                        course_code VARCHAR(64) NOT NULL,
                        mode VARCHAR(32) NOT NULL,
                        question_ids_json TEXT NOT NULL,
                        source VARCHAR(32) DEFAULT 'rule',
                        strategy_json TEXT,
                        ai_reason TEXT,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
            jdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS idx_tower_question_pack_node
                    ON student_tower_question_pack(run_id, node_id, mode)
                    """);
        } catch (Exception e) {
            log.warn("Tower run table migration skipped", e);
        }
    }

    private void migrateKnowledgeRelations() {
        if (!tableExists("knowledge_edge") || !tableExists("knowledge_relation")) return;
        try {
            jdbcTemplate.update("""
                    INSERT INTO knowledge_relation (relation_id, course_code, from_knowledge_point_id, to_knowledge_point_id, relation_type)
                    SELECT edge_id, course_code, source_id, target_id, relation_type
                    FROM knowledge_edge old_edge
                    WHERE NOT EXISTS (
                        SELECT 1 FROM knowledge_relation kr
                        WHERE kr.relation_id = old_edge.edge_id
                    )
                    """);
        } catch (Exception e) {
            log.warn("Legacy knowledge_edge migration skipped", e);
        }
    }

    private void migrateExams() {
        if (tableExists("paper") && tableExists("exam")) {
            try {
                jdbcTemplate.update("""
                        INSERT INTO exam (exam_id, course_code, task_no, title, generate_type, target_count, total_score, status, create_time)
                        SELECT paper_id, course_code, task_no, title, strategy, target_count, total_score, status, create_time
                        FROM paper old_paper
                        WHERE NOT EXISTS (
                            SELECT 1 FROM exam ex
                            WHERE ex.exam_id = old_paper.paper_id
                        )
                        """);
            } catch (Exception e) {
                log.warn("Legacy paper migration skipped", e);
            }
        }
        if (tableExists("paper_question") && tableExists("exam_question")) {
            try {
                jdbcTemplate.update("""
                        INSERT INTO exam_question (id, exam_id, question_id, sort_order, score_snapshot, question_type, knowledge_point_id, difficulty)
                        SELECT id, paper_id, question_id, sort_order, score_snapshot, question_type, knowledge_point_id, difficulty
                        FROM paper_question old_question
                        WHERE NOT EXISTS (
                            SELECT 1 FROM exam_question exam_question
                            WHERE exam_question.id = old_question.id
                        )
                        """);
            } catch (Exception e) {
                log.warn("Legacy paper_question migration skipped", e);
            }
        }
    }

    private boolean tableExists(String tableName) {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            return tableExists(metaData, tableName)
                    || tableExists(metaData, tableName.toUpperCase())
                    || tableExists(metaData, tableName.toLowerCase());
        } catch (Exception e) {
            log.warn("Could not inspect table {}", tableName, e);
            return false;
        }
    }

    private boolean tableExists(DatabaseMetaData metaData, String tableName) throws Exception {
        try (ResultSet rs = metaData.getTables(null, null, tableName, new String[]{"TABLE"})) {
            return rs.next();
        }
    }
}
