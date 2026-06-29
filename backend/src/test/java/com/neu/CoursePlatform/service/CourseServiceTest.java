package com.neu.CoursePlatform.service;

import com.neu.CoursePlatform.dto.CourseDTO;
import com.neu.CoursePlatform.entity.Course;
import com.neu.CoursePlatform.entity.Lesson;
import com.neu.CoursePlatform.mapper.CourseMapper;
import com.neu.CoursePlatform.service.impl.CourseServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.*;

import static com.neu.CoursePlatform.service.TeacherServiceTest.setBaseMapper;
import static org.junit.jupiter.api.Assertions.*;

class CourseServiceTest {

    private CourseServiceImpl service;
    private Map<String, Course> store;
    private List<Lesson> lessonStore;

    @BeforeEach
    void setUp() throws Exception {
        store = new LinkedHashMap<>();
        Course c1 = course("1", "Python程序设计", "李明", 4, 64, "计算机类");
        Course c2 = course("2", "数据结构", "测试教师", 3, 48, null);
        store.put("1", c1);
        store.put("2", c2);

        lessonStore = new ArrayList<>();
        lessonStore.add(lesson("1", "1", "Python简介", "video"));
        lessonStore.add(lesson("2", "1", "变量与类型", "doc"));

        CourseMapper proxy = (CourseMapper) Proxy.newProxyInstance(
                CourseMapper.class.getClassLoader(), new Class<?>[]{CourseMapper.class},
                (p, method, args) -> courseInvoke(store, method.getName(), args));

        LessonService lessonProxy = (LessonService) Proxy.newProxyInstance(
                LessonService.class.getClassLoader(), new Class<?>[]{LessonService.class},
                (p, method, args) -> {
                    if ("listByCourseCode".equals(method.getName())) {
                        String cc = (String) args[0];
                        return lessonStore.stream().filter(l -> cc.equals(l.getCourseCode())).toList();
                    }
                    return null;
                });

        service = new CourseServiceImpl(lessonProxy);
        setBaseMapper(service, proxy);
    }

    @Test
    void searchByKeywordFindsCourseName() {
        List<Course> results = service.searchByKeyword("Python");
        assertEquals(1, results.size());
        assertEquals("Python程序设计", results.get(0).getCourseName());
    }

    @Test
    void searchDtoByKeywordIncludesLessonCount() {
        List<CourseDTO> results = service.searchDtoByKeyword("Python");
        assertEquals(1, results.size());
        assertEquals(2, results.get(0).getLessonCount());
    }

    @Test
    void listDtoReturnsAllCourses() {
        List<CourseDTO> results = service.listDto();
        assertEquals(2, results.size());
    }

    @Test
    void listLessonsReturnsCorrectLessons() {
        List<Lesson> lessons = service.listLessons("1");
        assertEquals(2, lessons.size());
    }

    @Test
    void listLessonsReturnsEmptyForNoLessons() {
        List<Lesson> lessons = service.listLessons("2");
        assertTrue(lessons.isEmpty());
    }

    @Test
    void dtoHasCorrectFields() {
        List<CourseDTO> dtos = service.listDto();
        CourseDTO dto = dtos.get(0);
        assertEquals("Python程序设计", dto.getCourseName());
        assertEquals("李明", dto.getTeacher());
        assertEquals(4, dto.getCredits());
        assertEquals(64, dto.getHours());
    }

    // ============ helpers ============

    private Course course(String code, String name, String teacher, int credits, int hours, String major) {
        Course c = new Course();
        c.setCourseCode(code);
        c.setCourseName(name);
        c.setTeacher(teacher);
        c.setCredits(credits);
        c.setHours(hours);
        c.setApplicableMajor(major);
        return c;
    }

    private Lesson lesson(String no, String cc, String title, String type) {
        Lesson l = new Lesson();
        l.setLessonNo(no);
        l.setCourseCode(cc);
        l.setLessonTitle(title);
        l.setResourceType(type);
        return l;
    }

    static Object courseInvoke(Map<String, Course> store, String name, Object[] args) {
        switch (name) {
            case "selectByKeyword": {
                String kw = (String) args[0];
                if (kw == null || kw.isEmpty()) return new ArrayList<>(store.values());
                return store.values().stream()
                        .filter(c -> c.getCourseName() != null && c.getCourseName().contains(kw)).toList();
            }
            case "selectById": return store.get(String.valueOf(args[0]));
            case "selectList": return new ArrayList<>(store.values());
            case "insert": { Course c = (Course) args[0]; store.put(c.getCourseCode(), c); return 1; }
            case "updateById": { Course c = (Course) args[0]; store.put(c.getCourseCode(), c); return 1; }
            case "deleteById": return store.remove(String.valueOf(args[0])) != null ? 1 : 0;
            case "selectCount": return (long) store.size();
            default: return null;
        }
    }
}
