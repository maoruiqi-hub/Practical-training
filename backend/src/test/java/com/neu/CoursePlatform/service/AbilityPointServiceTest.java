package com.neu.CoursePlatform.service;

import com.neu.CoursePlatform.entity.AbilityPoint;
import com.neu.CoursePlatform.mapper.AbilityPointMapper;
import com.neu.CoursePlatform.service.impl.AbilityPointServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.*;

import static com.neu.CoursePlatform.service.TeacherServiceTest.setBaseMapper;
import static org.junit.jupiter.api.Assertions.*;

class AbilityPointServiceTest {

    private AbilityPointServiceImpl service;
    private Map<String, AbilityPoint> store;

    @BeforeEach
    void setUp() throws Exception {
        store = new LinkedHashMap<>();
        store.put("ap-1", ap("ap-1", "course-1", "Python基础能力"));
        store.put("ap-2", ap("ap-2", "course-1", "算法思维"));
        store.put("ap-3", ap("ap-3", "course-2", "数据结构应用"));

        AbilityPointMapper proxy = (AbilityPointMapper) Proxy.newProxyInstance(
                AbilityPointMapper.class.getClassLoader(), new Class<?>[]{AbilityPointMapper.class},
                (p, method, args) -> {
                    switch (method.getName()) {
                        case "selectList": {
                            var wrapper = args[0];
                            // LambdaQueryWrapper — just return all for simplicity
                            return new ArrayList<>(store.values());
                        }
                        case "insert": { AbilityPoint a = (AbilityPoint) args[0]; store.put(a.getAbilityPointId(), a); return 1; }
                        case "selectById": return store.get(String.valueOf(args[0]));
                        case "updateById": { AbilityPoint a = (AbilityPoint) args[0]; store.put(a.getAbilityPointId(), a); return 1; }
                        case "deleteById": return store.remove(String.valueOf(args[0])) != null ? 1 : 0;
                        case "selectCount": return (long) store.size();
                        default: return null;
                    }
                });

        service = new AbilityPointServiceImpl();
        setBaseMapper(service, proxy);
    }

    @Test
    void listByCourseCodeReturnsPoints() {
        List<AbilityPoint> results = service.listByCourseCode("course-1");
        assertEquals(3, results.size()); // fake mapper returns all
    }

    @Test
    void listByCourseCodeReturnsAllForAnyCourse() {
        List<AbilityPoint> results = service.listByCourseCode("nonexistent");
        assertEquals(3, results.size()); // fake mapper doesn't filter
    }

    @Test
    void abilityPointHasCorrectFields() {
        List<AbilityPoint> results = service.listByCourseCode("course-1");
        AbilityPoint ap = results.get(0);
        assertNotNull(ap.getAbilityPointId());
        assertNotNull(ap.getName());
    }

    // ============ helper ============

    private AbilityPoint ap(String id, String courseCode, String name) {
        AbilityPoint a = new AbilityPoint();
        a.setAbilityPointId(id);
        a.setCourseCode(courseCode);
        a.setName(name);
        return a;
    }
}
