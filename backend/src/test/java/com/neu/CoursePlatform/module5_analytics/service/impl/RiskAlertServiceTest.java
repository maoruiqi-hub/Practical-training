package com.neu.CoursePlatform.module5_analytics.service.impl;

import com.neu.CoursePlatform.module5_analytics.entity.RiskAlert;
import com.neu.CoursePlatform.module5_analytics.mapper.RiskAlertMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class RiskAlertServiceTest {

    private RiskAlertServiceImpl service;
    private Map<String, RiskAlert> store;
    private List<String> activeByStudent;
    private int countActiveByType;

    @BeforeEach
    void setUp() throws Exception {
        store = new LinkedHashMap<>();
        activeByStudent = new ArrayList<>();
        countActiveByType = 0;

        RiskAlertMapper proxy = (RiskAlertMapper) Proxy.newProxyInstance(
                RiskAlertMapper.class.getClassLoader(),
                new Class<?>[]{RiskAlertMapper.class},
                (p, method, args) -> riskMapperInvoke(this, method.getName(), args));

        service = new RiskAlertServiceImpl();
        // 手动注入 baseMapper
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
    void receiveEventCreatesAlert() {
        RiskAlert alert = service.receiveEvent("student-1", "course-1",
                "low_score", "high", "{\"score\":40}");
        assertNotNull(alert);
        assertEquals("student-1", alert.getStudentId());
        assertEquals("low_score", alert.getRiskType());
        assertEquals("high", alert.getRiskLevel());
        assertEquals("active", alert.getStatus());
        assertNotNull(alert.getCreatedAt());
    }

    @Test
    void receiveEventDedupSkipsWhenActiveExists() {
        countActiveByType = 1; // 模拟已存在活跃预警
        RiskAlert alert = service.receiveEvent("student-1", "course-1",
                "low_score", "high", "{}");
        assertNull(alert);
    }

    @Test
    void resolveChangesStatus() {
        RiskAlert alert = new RiskAlert();
        alert.setId("alert-1");
        alert.setStatus("active");
        store.put("alert-1", alert);

        assertTrue(service.resolve("alert-1", "teacher-1"));
        assertEquals("resolved", alert.getStatus());
        assertEquals("teacher-1", alert.getResolvedBy());
        assertNotNull(alert.getResolvedAt());
    }

    @Test
    void resolveReturnsFalseForNonexistent() {
        assertFalse(service.resolve("nonexistent", "teacher-1"));
    }

    @Test
    void resolveReturnsFalseForAlreadyResolved() {
        RiskAlert alert = new RiskAlert();
        alert.setId("alert-2");
        alert.setStatus("resolved");
        store.put("alert-2", alert);

        assertFalse(service.resolve("alert-2", "teacher-1"));
    }

    @Test
    void hasActiveAlertDelegatesToMapper() {
        countActiveByType = 0;
        assertFalse(service.hasActiveAlert("student-1", "procrastination"));

        countActiveByType = 1;
        assertTrue(service.hasActiveAlert("student-1", "procrastination"));
    }

    @Test
    void getStudentRiskStatusReturnsHighestLevel() {
        activeByStudent.add("high");
        activeByStudent.add("low");
        var status = service.getStudentRiskStatus("student-1");
        assertEquals("high", status.highestLevel());
    }

    @Test
    void getStudentRiskStatusReturnsNoneForEmpty() {
        var status = service.getStudentRiskStatus("student-1");
        assertEquals("none", status.highestLevel());
    }

    // ============ proxy handler ============

    static Object riskMapperInvoke(RiskAlertServiceTest self, String name, Object[] args) {
        switch (name) {
            case "insert": {
                RiskAlert a = (RiskAlert) args[0];
                if (a.getId() == null) a.setId(UUID.randomUUID().toString());
                self.store.put(a.getId(), a);
                return 1;
            }
            case "selectById": return self.store.get(String.valueOf(args[0]));
            case "updateById": { RiskAlert a = (RiskAlert) args[0]; self.store.put(a.getId(), a); return 1; }
            case "selectList": return new ArrayList<>(self.store.values());
            case "selectCount": return (long) self.store.size();
            case "countActiveByType": return self.countActiveByType;
            case "selectActiveByStudent": {
                return self.activeByStudent.stream().map(level -> {
                    RiskAlert a = new RiskAlert();
                    a.setRiskLevel(level);
                    a.setId(UUID.randomUUID().toString());
                    return a;
                }).toList();
            }
            default: return null;
        }
    }
}
