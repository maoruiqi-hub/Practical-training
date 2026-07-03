package com.neu.CoursePlatform.module5_analytics.service.impl;

import com.neu.CoursePlatform.entity.Student;
import com.neu.CoursePlatform.mapper.StudentMapper;
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
    private Map<String, Student> students; // studentNo → Student
    private int nameDuplicateCount;

    @BeforeEach
    void setUp() throws Exception {
        store = new LinkedHashMap<>();
        classStudents = new LinkedHashMap<>();
        students = new LinkedHashMap<>();
        nameDuplicateCount = 0;

        ClassInfoMapper classInfoMapperProxy = (ClassInfoMapper) Proxy.newProxyInstance(
                ClassInfoMapper.class.getClassLoader(),
                new Class<?>[]{ClassInfoMapper.class},
                (p, method, args) -> mapperInvoke(this, method.getName(), args));

        StudentMapper studentMapperProxy = (StudentMapper) Proxy.newProxyInstance(
                StudentMapper.class.getClassLoader(),
                new Class<?>[]{StudentMapper.class},
                (p, method, args) -> studentMapperInvoke(this, method.getName(), args));

        service = new ClassInfoServiceImpl(studentMapperProxy);
        Class<?> clazz = service.getClass();
        while (clazz != null) {
            try {
                java.lang.reflect.Field f = clazz.getDeclaredField("baseMapper");
                f.setAccessible(true);
                f.set(service, classInfoMapperProxy);
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
        Student s = new Student();
        s.setStudentNo("student-1");
        s.setName("张三");
        students.put("student-1", s);
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

    @Test
    void updateClassReturnsNullWhenMissing() {
        ClassInfo updated = new ClassInfo();
        updated.setName("不存在");

        assertNull(service.updateClass("missing", updated));
    }

    @Test
    void updateClassSuccessKeepsIdAndRefreshesUpdatedAt() {
        saveClass("class-6", "计科206", "course-1");
        ClassInfo updated = new ClassInfo();
        updated.setName("计科206强化班");
        updated.setSemester("2026-2027-1");

        ClassInfo result = service.updateClass("class-6", updated);

        assertNotNull(result);
        assertEquals("class-6", result.getId());
        assertEquals("计科206强化班", result.getName());
        assertEquals("2026-2027-1", result.getSemester());
        assertNotNull(result.getUpdatedAt());
    }

    @Test
    void updateClassSameNameDoesNotCheckDuplicate() {
        saveClass("class-7", "计科207", "course-1");
        nameDuplicateCount = 99;
        ClassInfo updated = new ClassInfo();
        updated.setName("计科207");

        assertNotNull(service.updateClass("class-7", updated));
    }

    @Test
    void listByTeacherAndGetStudentsReturnMapperData() {
        saveClass("class-8", "计科208", "course-1");
        Student s = student("student-8", "赵六", "行政1班");
        students.put("student-8", s);
        classStudents.computeIfAbsent("class-8", k -> new LinkedHashSet<>()).add("student-8");

        assertEquals(1, service.listByTeacher("teacher-1").size());
        assertEquals(List.of("student-8"), service.getStudentIds("class-8"));
        assertEquals("赵六", service.getStudents("class-8").get(0).getName());
        assertTrue(service.getStudents("empty").isEmpty());
    }

    @Test
    void enrollStudentRejectsMissingClassBlankStudentMissingStudentAndDuplicate() {
        saveClass("class-9", "计科209", "course-1");
        students.put("student-9", student("student-9", "钱七", "行政1班"));

        assertFalse(service.enrollStudent("missing", "student-9"));
        assertFalse(service.enrollStudent("class-9", " "));
        assertFalse(service.enrollStudent("class-9", "missing-student"));
        assertTrue(service.enrollStudent("class-9", "student-9"));
        assertFalse(service.enrollStudent("class-9", "student-9"));
    }

    @Test
    void enrollStudentsClassifiesAddedMissingAndDuplicated() {
        saveClass("class-10", "计科210", "course-1");
        students.put("s1", student("s1", "学生1", "行政1班"));
        students.put("s2", student("s2", "学生2", "行政1班"));
        assertTrue(service.enrollStudent("class-10", "s2"));

        Map<String, Object> result = service.enrollStudents("class-10",
                Arrays.asList("s1", "s2", "missing", "", null, "s1"));

        assertEquals(List.of("s1"), result.get("added"));
        assertEquals(List.of("missing"), result.get("missing"));
        assertEquals(List.of("s2"), result.get("duplicatedOrFailed"));
    }

    @Test
    void enrollStudentsReturnsMissingWhenClassDoesNotExist() {
        Map<String, Object> result = service.enrollStudents("missing", List.of("s1", "s2"));

        assertEquals(List.of(), result.get("added"));
        assertEquals(List.of("s1", "s2"), result.get("missing"));
        assertEquals(List.of(), result.get("duplicatedOrFailed"));
    }

    @Test
    void enrollStudentsByClassNameAddsMatchedAdministrativeClassStudents() {
        saveClass("class-11", "计科211", "course-1");
        students.put("s1", student("s1", "学生1", "软件1班"));
        students.put("s2", student("s2", "学生2", "软件1班"));
        students.put("s3", student("s3", "学生3", "软件2班"));

        Map<String, Object> result = service.enrollStudentsByClassName("class-11", "软件1班");

        assertEquals("软件1班", result.get("sourceClassName"));
        assertEquals(2, result.get("matched"));
        assertEquals(List.of("s1", "s2"), result.get("added"));
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

    private static Student student(String studentNo, String name, String className) {
        Student s = new Student();
        s.setStudentNo(studentNo);
        s.setName(name);
        s.setClassName(className);
        return s;
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
                boolean added = self.classStudents.computeIfAbsent((String) args[0], k -> new LinkedHashSet<>()).add((String) args[1]);
                if (!added) throw new IllegalStateException("duplicate");
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
            case "selectStudentsByClassId": {
                Set<String> ids = self.classStudents.get(String.valueOf(args[0]));
                if (ids == null) return List.of();
                return ids.stream().map(self.students::get).filter(Objects::nonNull).toList();
            }
            default: return null;
        }
    }

    static Object studentMapperInvoke(ClassInfoServiceTest self, String name, Object[] args) {
        switch (name) {
            case "selectById": return self.students.get(String.valueOf(args[0]));
            case "selectByClassId": {
                String className = String.valueOf(args[0]);
                return self.students.values().stream()
                        .filter(s -> className.equals(s.getClassName()))
                        .toList();
            }
            default: return null;
        }
    }
}
