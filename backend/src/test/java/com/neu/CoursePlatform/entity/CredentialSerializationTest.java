package com.neu.CoursePlatform.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class CredentialSerializationTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void studentAndTeacherPasswordsAreNeverSerialized() throws Exception {
        Student student = new Student();
        student.setStudentNo("s1");
        student.setPassword("secret");
        Teacher teacher = new Teacher();
        teacher.setTeacherNo("t1");
        teacher.setPassword("secret");

        assertFalse(objectMapper.writeValueAsString(student).contains("password"));
        assertFalse(objectMapper.writeValueAsString(teacher).contains("password"));
    }
}
