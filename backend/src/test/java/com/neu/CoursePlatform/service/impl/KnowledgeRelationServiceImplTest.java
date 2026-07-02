package com.neu.CoursePlatform.service.impl;

import static com.neu.CoursePlatform.service.TeacherServiceTest.setBaseMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.neu.CoursePlatform.entity.KnowledgePoint;
import com.neu.CoursePlatform.entity.KnowledgeRelation;
import com.neu.CoursePlatform.mapper.KnowledgeRelationMapper;
import com.neu.CoursePlatform.service.KnowledgePointService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.*;

class KnowledgeRelationServiceImplTest {

    private KnowledgeRelationServiceImpl service;
    private KnowledgePointService knowledgePointService;
    private Map<String, KnowledgeRelation> relationStore;

    @BeforeEach
    void setUp() throws Exception {
        knowledgePointService = mock(KnowledgePointService.class);
        relationStore = new LinkedHashMap<>();

        KnowledgeRelationMapper mapper = (KnowledgeRelationMapper) Proxy.newProxyInstance(
                KnowledgeRelationMapper.class.getClassLoader(),
                new Class<?>[]{KnowledgeRelationMapper.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("insert".equals(name) && args != null && args.length == 1 && args[0] instanceof KnowledgeRelation r) {
                        relationStore.put(r.getRelationId(), r);
                        return 1;
                    }
                    if ("toString".equals(name)) return "RelationMapperProxy";
                    if ("hashCode".equals(name)) return System.identityHashCode(proxy);
                    if ("equals".equals(name)) return proxy == args[0];
                    return null;
                });

        KnowledgeRelationServiceImpl real = new KnowledgeRelationServiceImpl(knowledgePointService);
        setBaseMapper(real, mapper);
        service = spy(real);

        // Stub listByCourseCode to return from in-memory store, avoiding LambdaQueryWrapper
        doAnswer(inv -> relationStore.values().stream()
                .filter(r -> inv.getArgument(0, String.class).equals(r.getCourseCode()))
                .toList())
                .when(service).listByCourseCode(anyString());

        // Stub relationExists to check in-memory store
        doAnswer(inv -> {
            String courseCode = inv.getArgument(0);
            String fromId = inv.getArgument(1);
            String toId = inv.getArgument(2);
            String type = inv.getArgument(3);
            return relationStore.values().stream()
                    .anyMatch(r -> courseCode.equals(r.getCourseCode())
                            && fromId.equals(r.getFromKnowledgePointId())
                            && toId.equals(r.getToKnowledgePointId())
                            && type.equals(r.getRelationType()));
        }).when(service).relationExists(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void createRelationWithValidDataSucceeds() {
        KnowledgePoint from = kp("kp-1", "CS101", "Java基础");
        KnowledgePoint to = kp("kp-2", "CS101", "OOP");
        when(knowledgePointService.getById("kp-1")).thenReturn(from);
        when(knowledgePointService.getById("kp-2")).thenReturn(to);

        KnowledgeRelation rel = new KnowledgeRelation();
        rel.setRelationId("rel-1");
        rel.setFromKnowledgePointId("kp-1");
        rel.setToKnowledgePointId("kp-2");

        assertDoesNotThrow(() -> service.createRelation("CS101", rel));
        assertEquals("prerequisite", rel.getRelationType());
        assertEquals("CS101", rel.getCourseCode());
        assertTrue(relationStore.containsKey("rel-1"));
    }

    @Test
    void createRelationThrowsWhenFromPointNotFound() {
        when(knowledgePointService.getById("kp-1")).thenReturn(null);
        when(knowledgePointService.getById("kp-2")).thenReturn(kp("kp-2", "CS101", "OOP"));

        KnowledgeRelation rel = relation("kp-1", "kp-2");
        assertThrows(IllegalArgumentException.class, () -> service.createRelation("CS101", rel));
    }

    @Test
    void createRelationThrowsWhenToPointNotFound() {
        when(knowledgePointService.getById("kp-1")).thenReturn(kp("kp-1", "CS101", "Java"));
        when(knowledgePointService.getById("kp-2")).thenReturn(null);

        KnowledgeRelation rel = relation("kp-1", "kp-2");
        assertThrows(IllegalArgumentException.class, () -> service.createRelation("CS101", rel));
    }

    @Test
    void createRelationThrowsWhenCourseMismatch() {
        when(knowledgePointService.getById("kp-1")).thenReturn(kp("kp-1", "CS101", "Java"));
        when(knowledgePointService.getById("kp-2")).thenReturn(kp("kp-2", "CS202", "Python"));

        KnowledgeRelation rel = relation("kp-1", "kp-2");
        assertThrows(IllegalArgumentException.class, () -> service.createRelation("CS101", rel));
    }

    @Test
    void createRelationThrowsWhenSelfReferencing() {
        KnowledgePoint same = kp("kp-1", "CS101", "Java");
        when(knowledgePointService.getById("kp-1")).thenReturn(same);

        KnowledgeRelation rel = relation("kp-1", "kp-1");
        assertThrows(IllegalArgumentException.class, () -> service.createRelation("CS101", rel));
    }

    @Test
    void createRelationThrowsWhenRelationIsNull() {
        assertThrows(IllegalArgumentException.class, () -> service.createRelation("CS101", null));
    }

    @Test
    void createRelationDefaultsToPrerequisiteWhenTypeNull() {
        KnowledgePoint from = kp("kp-1", "CS101", "Java");
        KnowledgePoint to = kp("kp-2", "CS101", "OOP");
        when(knowledgePointService.getById("kp-1")).thenReturn(from);
        when(knowledgePointService.getById("kp-2")).thenReturn(to);

        KnowledgeRelation rel = new KnowledgeRelation();
        rel.setRelationId("rel-2");
        rel.setFromKnowledgePointId("kp-1");
        rel.setToKnowledgePointId("kp-2");

        service.createRelation("CS101", rel);
        assertEquals("prerequisite", rel.getRelationType());
    }

    @Test
    void createRelationThrowsWhenDuplicateRelationExists() {
        KnowledgePoint from = kp("kp-1", "CS101", "Java");
        KnowledgePoint to = kp("kp-2", "CS101", "OOP");
        when(knowledgePointService.getById("kp-1")).thenReturn(from);
        when(knowledgePointService.getById("kp-2")).thenReturn(to);

        // Pre-populate store so relationExists returns true
        KnowledgeRelation existing = new KnowledgeRelation();
        existing.setRelationId("existing-rel");
        existing.setCourseCode("CS101");
        existing.setFromKnowledgePointId("kp-1");
        existing.setToKnowledgePointId("kp-2");
        existing.setRelationType("prerequisite");
        relationStore.put("existing-rel", existing);

        KnowledgeRelation rel = new KnowledgeRelation();
        rel.setRelationId("rel-3");
        rel.setFromKnowledgePointId("kp-1");
        rel.setToKnowledgePointId("kp-2");

        assertThrows(IllegalArgumentException.class, () -> service.createRelation("CS101", rel));
    }

    @Test
    void wouldCreateCycleReturnsTrueForSimpleCycle() {
        // Pre-populate: kp-2 → kp-1 exists, so adding kp-1 → kp-2 would create cycle
        KnowledgeRelation existing = new KnowledgeRelation();
        existing.setRelationId("r1");
        existing.setCourseCode("CS101");
        existing.setFromKnowledgePointId("kp-2");
        existing.setToKnowledgePointId("kp-1");
        existing.setRelationType("prerequisite");
        relationStore.put("r1", existing);

        assertTrue(service.wouldCreateCycle("CS101", "kp-1", "kp-2", "prerequisite"));
    }

    @Test
    void wouldCreateCycleReturnsFalseWhenNoCycle() {
        KnowledgeRelation existing = new KnowledgeRelation();
        existing.setRelationId("r1");
        existing.setCourseCode("CS101");
        existing.setFromKnowledgePointId("kp-1");
        existing.setToKnowledgePointId("kp-2");
        existing.setRelationType("prerequisite");
        relationStore.put("r1", existing);

        assertFalse(service.wouldCreateCycle("CS101", "kp-2", "kp-3", "prerequisite"));
    }

    @Test
    void wouldCreateCycleReturnsFalseForNonHierarchyType() {
        assertFalse(service.wouldCreateCycle("CS101", "kp-1", "kp-2", "related"));
    }

    @Test
    void createRelationThrowsWhenWouldCreateCycle() {
        KnowledgePoint from = kp("kp-1", "CS101", "Java");
        KnowledgePoint to = kp("kp-2", "CS101", "OOP");
        when(knowledgePointService.getById("kp-1")).thenReturn(from);
        when(knowledgePointService.getById("kp-2")).thenReturn(to);

        // Create existing reverse relation so adding forward creates a cycle
        KnowledgeRelation reverse = new KnowledgeRelation();
        reverse.setRelationId("r-reverse");
        reverse.setCourseCode("CS101");
        reverse.setFromKnowledgePointId("kp-2");
        reverse.setToKnowledgePointId("kp-1");
        reverse.setRelationType("prerequisite");
        relationStore.put("r-reverse", reverse);

        KnowledgeRelation rel = new KnowledgeRelation();
        rel.setRelationId("rel-cycle");
        rel.setFromKnowledgePointId("kp-1");
        rel.setToKnowledgePointId("kp-2");

        assertThrows(IllegalArgumentException.class, () -> service.createRelation("CS101", rel));
    }

    // ============ helpers ============

    private static KnowledgePoint kp(String id, String courseCode, String name) {
        KnowledgePoint kp = new KnowledgePoint();
        kp.setKnowledgePointId(id);
        kp.setCourseCode(courseCode);
        kp.setName(name);
        return kp;
    }

    private static KnowledgeRelation relation(String fromId, String toId) {
        KnowledgeRelation r = new KnowledgeRelation();
        r.setFromKnowledgePointId(fromId);
        r.setToKnowledgePointId(toId);
        return r;
    }
}
