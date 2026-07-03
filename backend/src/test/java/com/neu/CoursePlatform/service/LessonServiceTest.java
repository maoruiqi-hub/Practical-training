package com.neu.CoursePlatform.service;

import com.neu.CoursePlatform.entity.Lesson;
import com.neu.CoursePlatform.entity.Course;
import com.neu.CoursePlatform.mapper.LessonMapper;
import com.neu.CoursePlatform.mapper.CourseMapper;
import com.neu.CoursePlatform.service.impl.LessonServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class LessonServiceTest {

    private LessonServiceImpl service;
    private Map<String, Lesson> store;
    private Map<String, Course> courses;

    @BeforeEach
    void setUp() throws Exception {
        store = new LinkedHashMap<>();
        store.put("1", lesson("1", "1", "Python简介", "video", "/v/py.mp4", "入门"));
        store.put("2", lesson("2", "1", "变量与类型", "doc", "/d/var.md", "基础"));
        store.put("3", lesson("3", "2", "链表", "video", "/v/ll.mp4", "DS"));
        courses = new LinkedHashMap<>();
        Course course = new Course();
        course.setCourseCode("1");
        course.setCourseName("Python程序设计");
        course.setTeacher("王老师");
        courses.put("1", course);

        LessonMapper proxy = (LessonMapper) Proxy.newProxyInstance(
                LessonMapper.class.getClassLoader(),
                new Class<?>[]{LessonMapper.class},
                (p, method, args) -> invoke(store, method.getName(), args)
        );

        CourseMapper courseMapper = (CourseMapper) Proxy.newProxyInstance(
                CourseMapper.class.getClassLoader(),
                new Class<?>[]{CourseMapper.class},
                (p, method, args) -> {
                    if ("selectById".equals(method.getName())) return courses.get(String.valueOf(args[0]));
                    if ("toString".equals(method.getName())) return "CourseMapperProxy";
                    if ("hashCode".equals(method.getName())) return System.identityHashCode(p);
                    if ("equals".equals(method.getName())) return p == args[0];
                    return null;
                }
        );

        service = new LessonServiceImpl(courseMapper);
        setBaseMapper(service, proxy);
    }

    // ============ 按课程查询 ============

    @Test
    void listByCourseCode() {
        assertEquals(2, service.listByCourseCode("1").size());
    }

    @Test
    void listByCourseCodeEmpty() {
        assertTrue(service.listByCourseCode("999").isEmpty());
    }

    // ============ 搜索 ============

    @Test
    void searchByTitle() {
        assertEquals(1, service.searchByKeyword("Python").size());
    }

    @Test
    void searchByDescription() {
        assertEquals(1, service.searchByKeyword("DS").size());
    }

    @Test
    void searchEmpty() {
        assertEquals(3, service.searchByKeyword("").size());
    }

    // ============ 详情 DTO ============

    @Test
    void getDetailDtoIncludesCourseInfo() {
        var detail = service.getDetailDto("1");

        assertNotNull(detail);
        assertEquals("1", detail.getLessonNo());
        assertEquals("Python简介", detail.getLessonTitle());
        assertEquals("Python程序设计", detail.getCourseName());
        assertEquals("王老师", detail.getTeacherName());
    }

    @Test
    void getDetailDtoReturnsLessonInfoWhenCourseMissing() {
        Lesson lesson = lesson("4", "404", "孤立课时", "doc", "/x.md", "无课程");
        store.put("4", lesson);

        var detail = service.getDetailDto("4");

        assertNotNull(detail);
        assertEquals("孤立课时", detail.getLessonTitle());
        assertNull(detail.getCourseName());
        assertNull(detail.getTeacherName());
    }

    @Test
    void getDetailDtoReturnsNullWhenLessonMissing() {
        assertNull(service.getDetailDto("missing"));
    }

    // ============ CRUD ============

    @Test
    void saveAndGet() {
        Lesson l = lesson("4", "1", "测试", "ppt", "/t.pptx", "测");
        assertTrue(service.save(l));
        assertNotNull(service.getById("4"));
    }

    @Test
    void update() {
        Lesson l = service.getById("1");
        l.setLessonTitle("更新标题");
        service.updateById(l);
        assertEquals("更新标题", service.getById("1").getLessonTitle());
    }

    @Test
    void delete() {
        assertTrue(service.removeById("1"));
        assertNull(service.getById("1"));
    }

    @Test
    void deleteNonexistent() {
        assertFalse(service.removeById("999"));
    }

    // ============ helpers ============

    private Lesson lesson(String no, String cc, String title, String type, String url, String desc) {
        Lesson l = new Lesson();
        l.setLessonNo(no);
        l.setCourseCode(cc);
        l.setLessonTitle(title);
        l.setResourceType(type);
        l.setResourceUrl(url);
        l.setDescription(desc);
        return l;
    }

    static Object invoke(Map<String, Lesson> store, String methodName, Object[] args) {
        switch (methodName) {
            case "selectByCourseCode": {
                String cc = (String) args[0];
                return store.values().stream()
                        .filter(l -> cc != null && cc.equals(l.getCourseCode())).toList();
            }
            case "selectByKeyword": {
                String kw = (String) args[0];
                if (kw == null || kw.isEmpty()) return new ArrayList<>(store.values());
                return store.values().stream()
                        .filter(l -> (l.getLessonTitle() != null && l.getLessonTitle().contains(kw))
                                || (l.getDescription() != null && l.getDescription().contains(kw))).toList();
            }
            case "insert": {
                if (args != null && args.length == 1 && args[0] instanceof Lesson l) {
                    store.put(l.getLessonNo(), l);
                    return 1;
                }
                return 0;
            }
            case "selectById": return store.get(String.valueOf(args[0]));
            case "selectList": return new ArrayList<>(store.values());
            case "updateById": { Lesson l = (Lesson) args[0]; store.put(l.getLessonNo(), l); return 1; }
            case "deleteById": return store.remove(String.valueOf(args[0])) != null ? 1 : 0;
            case "selectCount": return (long) store.size();
            default: return null;
        }
    }

    private static void setBaseMapper(Object service, Object mapper) throws Exception {
        for (Class<?> clazz = service.getClass(); clazz != null; clazz = clazz.getSuperclass()) {
            try {
                java.lang.reflect.Field field = clazz.getDeclaredField("baseMapper");
                field.setAccessible(true);
                field.set(service, mapper);
                return;
            } catch (NoSuchFieldException ignored) {
            }
        }
        throw new NoSuchFieldException("baseMapper");
    }
}
