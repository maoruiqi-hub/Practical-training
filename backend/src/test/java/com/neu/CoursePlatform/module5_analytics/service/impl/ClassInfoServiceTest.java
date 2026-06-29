package com.neu.CoursePlatform.module5_analytics.service.impl;

import com.neu.CoursePlatform.module5_analytics.entity.ClassInfo;
import com.neu.CoursePlatform.module5_analytics.mapper.ClassInfoMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ClassInfoServiceTest {

    private ClassInfoServiceImpl service;
    private Map<String, ClassInfo> store;
    private Map<String, Set<String>> classStudents; // classId → studentIds
    private int nameDuplicateCount;

    @BeforeEach
    void setUp() throws Exception {
        store = new LinkedHashMap<>();
        classStudents = new LinkedHashMap<>();
        nameDuplicateCount = 0;

        ClassInfoMapper proxy = (ClassInfoMapper) Proxy.newProxyInstance(
                ClassInfoMapper.class.getClassLoader(),
                new Class<?>[]{ClassInfoMapper.class},
                (p, method, args) -> mapperInvoke(this, method.getName(), args));

        service = new ClassInfoServiceImpl();
        Class<?> clazz = service.getClass();
        while (clazz != null) {
            try {
                java.lang.reflect.Field f = clazz.getDeclaredField("baseMapper");
                f.setAccessible(true);
                f.set(service, proxy);
                break;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
    }

    @Test
    void createClassSuccess() {
        ClassInfo c = new ClassInfo();
        c.setName("计科202班");
        c.setCourseId("course-1");
        c.setTeacherId("teacher-1");

        ClassInfo created = service.createClass(c);
        assertNotNull(created);
        assertNotNull(created.getId());
        assertNotNull(created.getCreatedAt());
    }

    @Test
    void createClassFailsDuplicateName() {
        nameDuplicateCount = 1;
        ClassInfo c = new ClassInfo();
        c.setName("计科202班");
        c.setCourseId("course-1");

        assertNull(service.createClass(c));
    }

    @Test
    void deleteClassIfEmptySucceeds() {
        ClassInfo c = saveClass("class-1", "计科201", "course-1");
        assertEquals(0, service.countStudents("class-1"));
        assertTrue(service.deleteClassIfEmpty("class-1"));
        assertNull(service.getById("class-1"));
    }

    @Test
    void deleteClassIfEmptyFailsWhenHasStudents() {
        saveClass("class-2", "计科202", "course-1");
        classStudents.computeIfAbsent("class-2", k -> new HashSet<>()).add("student-1");
        assertFalse(service.deleteClassIfEmpty("class-2"));
        assertNotNull(service.getById("class-2"));
    }

    @Test
    void enrollStudentSuccess() {
        saveClass("class-3", "计科203", "course-1");
        assertTrue(service.enrollStudent("class-3", "student-1"));
        assertTrue(service.getStudentIds("class-3").contains("student-1"));
    }

    @Test
    void removeStudentSuccess() {
        saveClass("class-4", "计科204", "course-1");
        classStudents.computeIfAbsent("class-4", k -> new HashSet<>()).add("student-1");
        assertTrue(service.removeStudent("class-4", "student-1"));
    }

    @Test
    void updateClassNameDuplicateIsRejected() {
        ClassInfo c = saveClass("class-5", "计科205", "course-1");
        nameDuplicateCount = 1;
        ClassInfo updated = new ClassInfo();
        updated.setName("计科205改");
        assertNull(service.updateClass("class-5", updated));
    }

    // ============ helpers ============

    private ClassInfo saveClass(String id, String name, String courseId) {
        ClassInfo c = new ClassInfo();
        c.setId(id);
        c.setName(name);
        c.setCourseId(courseId);
        c.setTeacherId("teacher-1");
        store.put(id, c);
        return c;
    }

    static Object mapperInvoke(ClassInfoServiceTest self, String name, Object[] args) {
        switch (name) {
            case "insert": {
                ClassInfo c = (ClassInfo) args[0];
                if (c.getId() == null) c.setId(UUID.randomUUID().toString());
                self.store.put(c.getId(), c);
                return 1;
            }
            case "selectById": return self.store.get(String.valueOf(args[0]));
            case "updateById": { ClassInfo c = (ClassInfo) args[0]; self.store.put(c.getId(), c); return 1; }
            case "deleteById": return self.store.remove(String.valueOf(args[0])) != null ? 1 : 0;
            case "selectList": return new ArrayList<>(self.store.values());
            case "countByNameAndCourse": return self.nameDuplicateCount;
            case "countStudentsByClassId": {
                Set<String> students = self.classStudents.get(String.valueOf(args[0]));
                return students == null ? 0 : students.size();
            }
            case "insertClassStudent": {
                self.classStudents.computeIfAbsent((String) args[0], k -> new HashSet<>()).add((String) args[1]);
                return 1;
            }
            case "deleteClassStudent": {
                Set<String> students = self.classStudents.get((String) args[0]);
                if (students == null) return 0;
                return students.remove((String) args[1]) ? 1 : 0;
            }
            case "selectStudentIdsByClassId": {
                Set<String> students = self.classStudents.get(String.valueOf(args[0]));
                return students == null ? List.of() : new ArrayList<>(students);
            }
            case "selectByTeacherId": return new ArrayList<>(self.store.values());
            default: return null;
        }
    }
}
