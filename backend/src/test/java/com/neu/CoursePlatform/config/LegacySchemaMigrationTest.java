package com.neu.CoursePlatform.config;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacySchemaMigrationTest {

    @Test
    void migratesPlaintextPasswordsAndLeavesBcryptHashesUntouched() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:credential-migration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE student (student_no VARCHAR(64) PRIMARY KEY, password VARCHAR(128))");
        jdbc.execute("CREATE TABLE teacher (teacher_no VARCHAR(64) PRIMARY KEY, password VARCHAR(128))");
        String existingHash = new BCryptPasswordEncoder().encode("already-safe");
        jdbc.update("INSERT INTO student(student_no, password) VALUES (?, ?)", "s1", "plain-secret");
        jdbc.update("INSERT INTO teacher(teacher_no, password) VALUES (?, ?)", "t1", existingHash);

        new LegacySchemaMigration(dataSource, jdbc).migrateLegacyPasswords();

        String migrated = jdbc.queryForObject(
                "SELECT password FROM student WHERE student_no = 's1'", String.class);
        String unchanged = jdbc.queryForObject(
                "SELECT password FROM teacher WHERE teacher_no = 't1'", String.class);
        assertTrue(new BCryptPasswordEncoder().matches("plain-secret", migrated));
        assertEquals(existingHash, unchanged);
    }
}
