package com.neu.CoursePlatform.service.impl;

import com.neu.CoursePlatform.dto.CourseDTO;
import com.neu.CoursePlatform.entity.Course;
import com.neu.CoursePlatform.entity.Lesson;
import com.neu.CoursePlatform.mapper.CourseMapper;
import com.neu.CoursePlatform.service.LessonService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.*;

import static com.neu.CoursePlatform.service.TeacherServiceTest.setBaseMapper;
import static org.junit.jupiter.api.Assertions.*;

/**
 * CourseServiceImpl 单元测试 — 课程搜索 / DTO 映射 / 课时查询。
 * 使用与 TeacherServiceTest 相同的动态代理模式模拟 CourseMapper 与 LessonService。
 */
class CourseServiceImplTest {

    private CourseServiceImpl service;
    private Map<String, Course> courseStore;
    private List<Lesson> lessonStore;

    @BeforeEach
    void setUp() throws Exception {
        courseStore = new LinkedHashMap<>();
        lessonStore = new ArrayList<>();

        // ---- 构造课程数据（含所有字段） ----
        Course javaCourse = course("CS101", "Java程序设计", "张老师", "T001", 4, 64,
                "https://example.com/java.jpg",
                "学习Java语言核心语法与面向对象编程思想",
                "计算机科学与技术",
                "掌握Java基础语法；理解面向对象三大特性；能够独立开发Java应用");
        Course pythonCourse = course("CS102", "Python数据分析", "李老师", "T002", 3, 48,
                "https://example.com/python.jpg",
                "使用Python进行数据处理、分析与可视化",
                "数据科学",
                "掌握NumPy与Pandas核心API；能够完成数据清洗与可视化任务");
        Course netCourse = course("CS103", "计算机网络", "王老师", "T003", 3, 48,
                "https://example.com/net.jpg",
                "计算机网络体系结构与协议详解",
                "计算机科学与技术",
                "理解OSI七层模型；掌握TCP/IP协议栈；熟悉常见网络拓扑");

        courseStore.put("CS101", javaCourse);
        courseStore.put("CS102", pythonCourse);
        courseStore.put("CS103", netCourse);

        // ---- 构造课时数据 ----
        lessonStore.add(lesson("L001", "CS101", "Java语言概述"));
        lessonStore.add(lesson("L002", "CS101", "面向对象编程基础"));
        lessonStore.add(lesson("L003", "CS101", "异常处理机制"));
        lessonStore.add(lesson("L004", "CS102", "NumPy数组操作"));
        lessonStore.add(lesson("L005", "CS102", "Pandas数据框入门"));
        // CS103 没有课时

        // ---- 创建 CourseMapper 动态代理 ----
        CourseMapper courseMapperProxy = (CourseMapper) Proxy.newProxyInstance(
                CourseMapper.class.getClassLoader(),
                new Class<?>[]{CourseMapper.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("selectByKeyword".equals(name)) {
                        String keyword = (String) args[0];
                        if (keyword == null || keyword.isEmpty()) {
                            return new ArrayList<>();
                        }
                        return courseStore.values().stream()
                                .filter(c -> c.getCourseName() != null
                                        && c.getCourseName().contains(keyword))
                                .toList();
                    }
                    if ("selectList".equals(name)) {
                        return new ArrayList<>(courseStore.values());
                    }
                    if ("insert".equals(name) && args != null
                            && args.length == 1 && args[0] instanceof Course) {
                        Course course = (Course) args[0];
                        courseStore.put(course.getCourseCode(), course);
                        return 1;
                    }
                    // Object 方法
                    if ("toString".equals(name)) return "CourseMapperProxy";
                    if ("hashCode".equals(name)) return System.identityHashCode(proxy);
                    if ("equals".equals(name)) return proxy == args[0];
                    return null;
                });

        // ---- 创建 LessonService 动态代理 ----
        LessonService lessonServiceProxy = (LessonService) Proxy.newProxyInstance(
                LessonService.class.getClassLoader(),
                new Class<?>[]{LessonService.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("listByCourseCode".equals(name)) {
                        String courseCode = (String) args[0];
                        return new ArrayList<>(lessonStore.stream()
                                .filter(l -> courseCode != null
                                        && courseCode.equals(l.getCourseCode()))
                                .toList());
                    }
                    // Object 方法
                    if ("toString".equals(name)) return "LessonServiceProxy";
                    if ("hashCode".equals(name)) return System.identityHashCode(proxy);
                    if ("equals".equals(name)) return proxy == args[0];
                    return null;
                });

        // ---- 组装 service ----
        service = new CourseServiceImpl(lessonServiceProxy);
        setBaseMapper(service, courseMapperProxy);
    }

    // ============ searchByKeyword ============

    @Test
    void searchByKeywordFindsCourses() {
        List<Course> results = service.searchByKeyword("Java");
        assertEquals(1, results.size());
        assertEquals("CS101", results.get(0).getCourseCode());
        assertEquals("Java程序设计", results.get(0).getCourseName());
    }

    @Test
    void searchByKeywordReturnsEmptyForNoMatch() {
        List<Course> results = service.searchByKeyword("C++");
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void searchByKeywordWithNullKeyword() {
        List<Course> results = service.searchByKeyword(null);
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void searchByKeywordWithEmptyKeyword() {
        List<Course> results = service.searchByKeyword("");
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    // ============ searchDtoByKeyword ============

    @Test
    void searchDtoByKeywordReturnsDtosWithLessonCounts() {
        List<CourseDTO> results = service.searchDtoByKeyword("Java");

        assertEquals(1, results.size());
        CourseDTO dto = results.get(0);
        assertEquals("CS101", dto.getCourseCode());
        assertEquals("Java程序设计", dto.getCourseName());
        assertEquals(3, dto.getLessonCount(), "CS101 应有 3 个课时");
    }

    @Test
    void searchDtoByKeywordNoMatchReturnsEmpty() {
        List<CourseDTO> results = service.searchDtoByKeyword("Rust");
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    // ============ listDto ============

    @Test
    void listDtoReturnsAllCoursesAsDtos() {
        List<CourseDTO> results = service.listDto();

        assertEquals(3, results.size());

        // 按课程编号排序便于断言
        results.sort(Comparator.comparing(CourseDTO::getCourseCode));

        assertEquals("CS101", results.get(0).getCourseCode());
        assertEquals(3, results.get(0).getLessonCount());

        assertEquals("CS102", results.get(1).getCourseCode());
        assertEquals(2, results.get(1).getLessonCount());

        assertEquals("CS103", results.get(2).getCourseCode());
        assertEquals(0, results.get(2).getLessonCount(), "CS103 没有课时");
    }

    // ============ listLessons ============

    @Test
    void listLessonsReturnsLessonsForCourseCode() {
        List<Lesson> results = service.listLessons("CS101");

        assertEquals(3, results.size());
        results.sort(Comparator.comparing(Lesson::getLessonNo));

        assertEquals("L001", results.get(0).getLessonNo());
        assertEquals("CS101", results.get(0).getCourseCode());
        assertEquals("Java语言概述", results.get(0).getLessonTitle());

        assertEquals("L002", results.get(1).getLessonNo());
        assertEquals("面向对象编程基础", results.get(1).getLessonTitle());
    }

    @Test
    void listLessonsReturnsEmptyForCourseWithoutLessons() {
        List<Lesson> results = service.listLessons("CS103");
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void listLessonsReturnsEmptyForUnknownCourseCode() {
        List<Lesson> results = service.listLessons("NONEXIST");
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    // ============ toDto 字段映射 ============

    @Test
    void toDtoCorrectlyMapsAllCourseFields() {
        // 通过 listDto 拿到映射后的 DTO，逐个字段校验
        List<CourseDTO> dtos = service.listDto();
        CourseDTO dto = dtos.stream()
                .filter(d -> "CS101".equals(d.getCourseCode()))
                .findFirst()
                .orElseThrow();

        assertEquals("CS101", dto.getCourseCode());
        assertEquals("Java程序设计", dto.getCourseName());
        assertEquals("张老师", dto.getTeacher());
        assertEquals(Integer.valueOf(4), dto.getCredits());
        assertEquals(Integer.valueOf(64), dto.getHours());
        assertEquals("https://example.com/java.jpg", dto.getCoverUrl());
        assertEquals("学习Java语言核心语法与面向对象编程思想", dto.getDescription());
        assertEquals("计算机科学与技术", dto.getApplicableMajor());
        assertEquals("掌握Java基础语法；理解面向对象三大特性；能够独立开发Java应用",
                dto.getCourseObjectives());
        assertEquals(3, dto.getLessonCount());
    }

    @Test
    void toDtoMapsCourseWithNoLessonsCorrectly() {
        List<CourseDTO> dtos = service.listDto();
        CourseDTO dto = dtos.stream()
                .filter(d -> "CS103".equals(d.getCourseCode()))
                .findFirst()
                .orElseThrow();

        assertEquals("CS103", dto.getCourseCode());
        assertEquals("计算机网络", dto.getCourseName());
        assertEquals(0, dto.getLessonCount());
    }

    // ============ 组合场景 ============

    @Test
    void searchByKeywordThenListLessons() {
        List<Course> courses = service.searchByKeyword("数据分析");
        assertEquals(1, courses.size());
        assertEquals("CS102", courses.get(0).getCourseCode());

        List<Lesson> lessons = service.listLessons(courses.get(0).getCourseCode());
        assertEquals(2, lessons.size());
    }

    // ============ helper: 构造测试数据 ============

    private static Course course(String courseCode, String courseName, String teacher,
                                 String teacherNo, int credits, int hours,
                                 String coverUrl, String description,
                                 String applicableMajor, String courseObjectives) {
        Course c = new Course();
        c.setCourseCode(courseCode);
        c.setCourseName(courseName);
        c.setTeacher(teacher);
        c.setTeacherNo(teacherNo);
        c.setCredits(credits);
        c.setHours(hours);
        c.setCoverUrl(coverUrl);
        c.setDescription(description);
        c.setApplicableMajor(applicableMajor);
        c.setCourseObjectives(courseObjectives);
        return c;
    }

    private static Lesson lesson(String lessonNo, String courseCode, String lessonTitle) {
        Lesson l = new Lesson();
        l.setLessonNo(lessonNo);
        l.setCourseCode(courseCode);
        l.setLessonTitle(lessonTitle);
        return l;
    }
}
