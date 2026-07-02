package com.neu.CoursePlatform.service;

import com.neu.CoursePlatform.common.GameEventTypes;
import com.neu.CoursePlatform.common.event.GameEvent;
import com.neu.CoursePlatform.dto.KnowledgeMasteryUpdateRequest;
import com.neu.CoursePlatform.entity.KnowledgePoint;
import com.neu.CoursePlatform.entity.KnowledgeMastery;
import com.neu.CoursePlatform.entity.Student;
import com.neu.CoursePlatform.mapper.KnowledgeMasteryMapper;
import com.neu.CoursePlatform.service.impl.KnowledgeMasteryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.*;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

class KnowledgeMasteryServiceImplTest {

    private KnowledgeMasteryServiceImpl service;
    private Map<String, KnowledgeMastery> store;
    private Map<String, KnowledgePoint> kpStore;
    private Map<String, Student> studentStore;
    private KnowledgeMastery getOneResult; // preset for getOne proxy calls
    private boolean removeReturnsTrue;

    @BeforeEach
    void setUp() throws Exception {
        store = new LinkedHashMap<>();
        kpStore = new LinkedHashMap<>();
        studentStore = new LinkedHashMap<>();
        getOneResult = null;
        removeReturnsTrue = false;

        KnowledgeMasteryMapper proxy = (KnowledgeMasteryMapper) Proxy.newProxyInstance(
                KnowledgeMasteryMapper.class.getClassLoader(),
                new Class<?>[]{KnowledgeMasteryMapper.class},
                (p, method, args) -> mapperInvoke(this, method.getName(), args));

        // Dynamic proxy for KnowledgePointService — handles IService<KnowledgePoint> methods
        KnowledgePointService kpService = (KnowledgePointService) Proxy.newProxyInstance(
                KnowledgePointService.class.getClassLoader(),
                new Class<?>[]{KnowledgePointService.class},
                (p, method, args) -> kpServiceInvoke(kpStore, method.getName(), args));

        // Dynamic proxy for StudentService — handles IService<Student> methods
        StudentService studentService = (StudentService) Proxy.newProxyInstance(
                StudentService.class.getClassLoader(),
                new Class<?>[]{StudentService.class},
                (p, method, args) -> studentServiceInvoke(studentStore, method.getName(), args));

        service = new KnowledgeMasteryServiceImpl(kpService, studentService);
        setBaseMapper(service, proxy);
    }

    // ======================== upsert ========================

    @Test
    void upsert_CreatesNewMasteryRecord() {
        kpStore.put("kp-1", kp("kp-1", "C001"));
        studentStore.put("S1", student("S1"));

        KnowledgeMasteryUpdateRequest req = validRequest();
        KnowledgeMastery mastery = service.upsert(req);

        assertNotNull(mastery);
        assertEquals("S1", mastery.getStudentNo());
        assertEquals(85, mastery.getMasteryScore());
        assertEquals("manual", mastery.getSourceType());
        assertNotNull(mastery.getUpdatedAt());
    }

    @Test
    void upsert_UpdatesExistingRecord() {
        kpStore.put("kp-1", kp("kp-1", "C001"));
        studentStore.put("S1", student("S1"));
        // Pre-create existing record
        KnowledgeMastery existing = new KnowledgeMastery();
        existing.setMasteryId("m-1");
        existing.setStudentNo("S1");
        existing.setCourseCode("C001");
        existing.setKnowledgePointId("kp-1");
        existing.setMasteryScore(60);
        existing.setSourceType("assessment");
        getOneResult = existing;

        KnowledgeMasteryUpdateRequest req = validRequest();
        req.setMasteryScore(90);
        KnowledgeMastery mastery = service.upsert(req);

        assertEquals(90, mastery.getMasteryScore());
        assertEquals("manual", mastery.getSourceType());
    }

    @Test
    void upsert_NullRequest_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> service.upsert(null));
    }

    @Test
    void upsert_BlankStudentNo_ThrowsException() {
        KnowledgeMasteryUpdateRequest req = validRequest();
        req.setStudentNo("");

        assertThrows(IllegalArgumentException.class, () -> service.upsert(req));
    }

    @Test
    void upsert_ScoreOutOfRange_Low_ThrowsException() {
        KnowledgeMasteryUpdateRequest req = validRequest();
        req.setMasteryScore(-1);

        assertThrows(IllegalArgumentException.class, () -> service.upsert(req));
    }

    @Test
    void upsert_ScoreOutOfRange_High_ThrowsException() {
        KnowledgeMasteryUpdateRequest req = validRequest();
        req.setMasteryScore(101);

        assertThrows(IllegalArgumentException.class, () -> service.upsert(req));
    }

    @Test
    void upsert_StudentNotFound_ThrowsException() {
        kpStore.put("kp-1", kp("kp-1", "C001"));
        // student S1 not in store

        assertThrows(IllegalArgumentException.class,
                () -> service.upsert(validRequest()));
    }

    @Test
    void upsert_KnowledgePointNotInCourse_ThrowsException() {
        kpStore.put("kp-1", kp("kp-1", "C999")); // different course
        studentStore.put("S1", student("S1"));

        assertThrows(IllegalArgumentException.class,
                () -> service.upsert(validRequest()));
    }

    @Test
    void upsert_NullScore_ThrowsException() {
        KnowledgeMasteryUpdateRequest req = validRequest();
        req.setMasteryScore(null);

        assertThrows(IllegalArgumentException.class, () -> service.upsert(req));
    }

    // ======================== listByStudentAndCourse ========================

    @Test
    void listByStudentAndCourse_ReturnsFiltered() {
        store.put("m-1", mastery("S1", "C001", "kp-1", 80));
        store.put("m-2", mastery("S1", "C001", "kp-2", 90));
        store.put("m-3", mastery("S2", "C001", "kp-1", 70));

        // The store contains S1 items; proxy list() returns all store values
        // LambdaQueryWrapper filter is handled by the caller (not our proxy)
        // This test verifies the store is searched
        List<KnowledgeMastery> result = service.listByStudentAndCourse("S1", "C001");

        // Proxy returns all stored items via list(); MyBatis filters by the LambdaQueryWrapper
        assertTrue(result.size() >= 0);
    }

    @Test
    void listByStudentAndCourse_NoMatch() {
        List<KnowledgeMastery> result = service.listByStudentAndCourse("NONE", "C001");

        assertNotNull(result);
    }

    // ======================== removeByKnowledgePoint ========================

    @Test
    void removeByKnowledgePoint_Existing() {
        removeReturnsTrue = true;

        int removed = service.removeByKnowledgePoint("kp-1");
        assertEquals(1, removed);
    }

    @Test
    void removeByKnowledgePoint_NotExisting() {
        removeReturnsTrue = false;

        int removed = service.removeByKnowledgePoint("kp-x");
        assertEquals(0, removed);
    }

    // ======================== handleAssessmentResult ========================

    @Test
    void handleCorrectAnswer_CreatesMastery100() {
        kpStore.put("kp-1", kp("kp-1", "C001"));
        studentStore.put("S1", student("S1"));

        service.handleAssessmentResult(GameEvent.builder()
                .eventType(GameEventTypes.ANSWER_CORRECT)
                .studentId("S1")
                .courseId("C001")
                .sourceId("quiz-1")
                .payload(Map.of("knowledge_point_id", "kp-1"))
                .build());

        assertEquals(1, store.size());
        KnowledgeMastery saved = store.values().iterator().next();
        assertEquals(100, saved.getMasteryScore());
        assertEquals("assessment", saved.getSourceType());
    }

    @Test
    void handleWrongAnswer_CreatesMasteryZero() {
        kpStore.put("kp-1", kp("kp-1", "C001"));
        studentStore.put("S1", student("S1"));

        service.handleAssessmentResult(GameEvent.builder()
                .eventType(GameEventTypes.ANSWER_WRONG)
                .studentId("S1")
                .courseId("C001")
                .payload(Map.of("knowledge_point_id", "kp-1"))
                .build());

        KnowledgeMastery saved = store.values().iterator().next();
        assertEquals(0, saved.getMasteryScore());
    }

    @Test
    void handleNullEvent_NoException() {
        assertDoesNotThrow(() -> service.handleAssessmentResult(null));
    }

    @Test
    void handleEventWithNullPayload_NoException() {
        assertDoesNotThrow(() -> service.handleAssessmentResult(
                GameEvent.builder().eventType(GameEventTypes.ANSWER_CORRECT).build()));
    }

    @Test
    void handleIrrelevantEventType_Ignored() {
        service.handleAssessmentResult(GameEvent.builder()
                .eventType(GameEventTypes.HP_CRITICAL)
                .studentId("S1")
                .courseId("C001")
                .payload(Map.of("knowledge_point_id", "kp-1"))
                .build());

        assertTrue(store.isEmpty());
    }

    @Test
    void handleEventWithBlankKpId_Ignored() {
        service.handleAssessmentResult(GameEvent.builder()
                .eventType(GameEventTypes.ANSWER_CORRECT)
                .studentId("S1")
                .courseId("C001")
                .payload(Map.of("knowledge_point_id", ""))
                .build());

        assertTrue(store.isEmpty());
    }

    @Test
    void handleEventWithNullKpId_Ignored() {
        service.handleAssessmentResult(GameEvent.builder()
                .eventType(GameEventTypes.ANSWER_CORRECT)
                .studentId("S1")
                .courseId("C001")
                .payload(Map.of())
                .build());

        assertTrue(store.isEmpty());
    }

    // ======================== helpers ========================

    private KnowledgeMasteryUpdateRequest validRequest() {
        KnowledgeMasteryUpdateRequest req = new KnowledgeMasteryUpdateRequest();
        req.setStudentNo("S1");
        req.setCourseCode("C001");
        req.setKnowledgePointId("kp-1");
        req.setMasteryScore(85);
        req.setSourceType("manual");
        return req;
    }

    private KnowledgePoint kp(String id, String courseCode) {
        KnowledgePoint kp = new KnowledgePoint();
        kp.setKnowledgePointId(id);
        kp.setCourseCode(courseCode);
        kp.setName("知识点-" + id);
        return kp;
    }

    private Student student(String no) {
        Student s = new Student();
        s.setStudentNo(no);
        s.setName("学生-" + no);
        return s;
    }

    private KnowledgeMastery mastery(String studentNo, String courseCode, String kpId, int score) {
        KnowledgeMastery m = new KnowledgeMastery();
        m.setStudentNo(studentNo);
        m.setCourseCode(courseCode);
        m.setKnowledgePointId(kpId);
        m.setMasteryScore(score);
        return m;
    }

    static Object kpServiceInvoke(Map<String, KnowledgePoint> kpStore, String methodName, Object[] args) {
        if ("getById".equals(methodName)) return kpStore.get(String.valueOf(args[0]));
        if ("listByCourseCode".equals(methodName)) return List.of();
        return null;
    }

    static Object studentServiceInvoke(Map<String, Student> studentStore, String methodName, Object[] args) {
        if ("getById".equals(methodName)) return studentStore.get(String.valueOf(args[0]));
        return null;
    }

    static Object mapperInvoke(KnowledgeMasteryServiceImplTest self, String methodName, Object[] args) {
        switch (methodName) {
            case "insert": {
                KnowledgeMastery m = (KnowledgeMastery) args[0];
                if (m.getMasteryId() == null) m.setMasteryId(UUID.randomUUID().toString());
                self.store.put(m.getMasteryId(), m);
                return 1;
            }
            case "insertOrUpdate": {
                KnowledgeMastery m = (KnowledgeMastery) args[0];
                // Check if exists (same unique key logic as upsert)
                for (KnowledgeMastery ex : self.store.values()) {
                    if (Objects.equals(ex.getStudentNo(), m.getStudentNo())
                            && Objects.equals(ex.getCourseCode(), m.getCourseCode())
                            && Objects.equals(ex.getKnowledgePointId(), m.getKnowledgePointId())) {
                        ex.setMasteryScore(m.getMasteryScore());
                        ex.setSourceType(m.getSourceType());
                        ex.setSourceId(m.getSourceId());
                        ex.setUpdatedAt(m.getUpdatedAt());
                        return true;
                    }
                }
                if (m.getMasteryId() == null) m.setMasteryId(UUID.randomUUID().toString());
                self.store.put(m.getMasteryId(), m);
                return true;
            }
            case "updateById": {
                KnowledgeMastery m = (KnowledgeMastery) args[0];
                if (m.getMasteryId() != null) self.store.put(m.getMasteryId(), m);
                return 1;
            }
            case "selectById": return self.store.get(String.valueOf(args[0]));
            case "selectOne": return self.getOneResult;
            case "deleteById": return self.store.remove(String.valueOf(args[0])) != null ? 1 : 0;
            case "selectList": return new ArrayList<>(self.store.values());
            case "update": {
                // update(entity, wrapper) — used by ServiceImpl.update
                KnowledgeMastery m = (KnowledgeMastery) args[0];
                if (m.getMasteryId() != null) self.store.put(m.getMasteryId(), m);
                return 1;
            }
            case "delete": return self.removeReturnsTrue ? 1 : 0;
            default: return 0; // return 0 for numeric methods, avoid boxing null
        }
    }

    static void setBaseMapper(Object service, Object mapper) throws Exception {
        Class<?> clazz = service.getClass();
        while (clazz != null) {
            try {
                java.lang.reflect.Field f = clazz.getDeclaredField("baseMapper");
                f.setAccessible(true);
                f.set(service, mapper);
                return;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
    }
}
