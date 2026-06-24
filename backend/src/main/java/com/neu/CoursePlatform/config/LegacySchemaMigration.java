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
        migrateKnowledgeRelations();
        migrateExams();
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
