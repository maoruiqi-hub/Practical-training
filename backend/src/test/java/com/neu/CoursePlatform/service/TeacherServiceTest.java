package com.neu.CoursePlatform.service;

import com.neu.CoursePlatform.entity.Teacher;
import com.neu.CoursePlatform.mapper.TeacherMapper;
import com.neu.CoursePlatform.service.impl.TeacherServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 教师服务单元测试 — 登录/注册/搜索。
 * 使用与 Module2ServiceTest 相同的动态代理模式。
 */
public class TeacherServiceTest {

    private TeacherServiceImpl service;
    private Map<String, Teacher> store;

    @BeforeEach
    void setUp() throws Exception {
        store = new LinkedHashMap<>();
        Teacher admin = new Teacher();
        admin.setTeacherNo("1");
        admin.setUsername("admin");
        admin.setPassword("admin123");
        admin.setRole("admin");
        admin.setName("管理员");
        store.put("1", admin);

        TeacherMapper mapperProxy = (TeacherMapper) Proxy.newProxyInstance(
                TeacherMapper.class.getClassLoader(),
                new Class<?>[]{TeacherMapper.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("selectByUsername".equals(name)) {
                        String username = (String) args[0];
                        return store.values().stream()
                                .filter(t -> username != null && username.equals(t.getUsername()))
                                .findFirst().orElse(null);
                    }
                    if ("selectByKeyword".equals(name)) {
                        String keyword = (String) args[0];
                        if (keyword == null || keyword.isEmpty()) return new ArrayList<>(store.values());
                        return store.values().stream()
                                .filter(t -> (t.getName() != null && t.getName().contains(keyword))
                                        || (t.getCollege() != null && t.getCollege().contains(keyword)))
                                .toList();
                    }
                    if ("insert".equals(name) && args != null && args.length == 1 && args[0] instanceof Teacher) {
                        Teacher t = (Teacher) args[0];
                        if (t.getTeacherNo() == null) t.setTeacherNo(String.valueOf(store.size() + 1));
                        store.put(t.getTeacherNo(), t);
                        return 1;
                    }
                    if ("selectById".equals(name)) {
                        return store.get(String.valueOf(args[0]));
                    }
                    if ("selectList".equals(name)) {
                        return new ArrayList<>(store.values());
                    }
                    if ("updateById".equals(name) && args != null && args.length == 1 && args[0] instanceof Teacher) {
                        Teacher t = (Teacher) args[0];
                        store.put(t.getTeacherNo(), t);
                        return 1;
                    }
                    if ("deleteById".equals(name)) {
                        return store.remove(String.valueOf(args[0])) != null ? 1 : 0;
                    }
                    if ("selectCount".equals(name)) {
                        return (long) store.size();
                    }
                    // Object methods
                    if ("toString".equals(name)) return "TeacherMapperProxy";
                    if ("hashCode".equals(name)) return System.identityHashCode(proxy);
                    if ("equals".equals(name)) return proxy == args[0];
                    return null;
                }
        );

        service = new TeacherServiceImpl();
        setBaseMapper(service, mapperProxy);
    }

    // ============ 登录 ============

    @Test
    void loginSuccess() {
        Teacher result = service.login("admin", "admin123");
        assertNotNull(result);
        assertEquals("管理员", result.getName());
    }

    @Test
    void loginFailsWrongPassword() {
        assertNull(service.login("admin", "wrong"));
    }

    @Test
    void loginFailsNonexistentUser() {
        assertNull(service.login("nobody", "any"));
    }

    @Test
    void loginFailsEmptyInput() {
        assertNull(service.login("", ""));
        assertNull(service.login("admin", ""));
        assertNull(service.login(null, "pass"));
    }

    // ============ 注册 ============

    @Test
    void registerSuccess() {
        Teacher t = teacher("3", "newteacher", "pass", "新老师");
        assertTrue(service.register(t));
        assertNotNull(service.login("newteacher", "pass"));
    }

    @Test
    void registerFailsDuplicateUsername() {
        Teacher dup = teacher("9", "admin", "other", "重名");
        assertFalse(service.register(dup));
    }

    @Test
    void registerHandlesNullFields() {
        Teacher t = new Teacher();
        t.setTeacherNo("5");
        t.setUsername("testnull");
        t.setPassword("123");
        assertTrue(service.register(t));
    }

    // ============ 搜索 ============

    @Test
    void searchFindsByName() {
        store.put("2", teacher("2", "liming", "123", "李明"));
        List<Teacher> results = service.searchByKeyword("李");
        assertEquals(1, results.size());
        assertEquals("李明", results.get(0).getName());
    }

    @Test
    void searchEmptyReturnsAll() {
        store.put("2", teacher("2", "u2", "pw", "T2"));
        assertEquals(2, service.searchByKeyword("").size());
    }

    // ============ helper ============

    private Teacher teacher(String no, String username, String password, String name) {
        Teacher t = new Teacher();
        t.setTeacherNo(no);
        t.setUsername(username);
        t.setPassword(password);
        t.setName(name);
        t.setRole("teacher");
        return t;
    }

    public static void setBaseMapper(Object service, Object mapper) throws Exception {
        Class<?> clazz = service.getClass();
        while (clazz != null) {
            try {
                Field f = clazz.getDeclaredField("baseMapper");
                f.setAccessible(true);
                f.set(service, mapper);
                return;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchFieldException("baseMapper");
    }
}
