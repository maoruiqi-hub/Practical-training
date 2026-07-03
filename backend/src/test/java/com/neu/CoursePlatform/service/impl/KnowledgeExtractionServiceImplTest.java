package com.neu.CoursePlatform.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.neu.CoursePlatform.agentic.AgenticClient;
import com.neu.CoursePlatform.agentic.AgenticRequest;
import com.neu.CoursePlatform.agentic.AgenticResponse;
import com.neu.CoursePlatform.entity.CourseResource;
import com.neu.CoursePlatform.entity.KnowledgeExtractionCandidate;
import com.neu.CoursePlatform.entity.KnowledgePoint;
import com.neu.CoursePlatform.mapper.KnowledgeExtractionCandidateMapper;
import com.neu.CoursePlatform.service.CourseResourceService;
import com.neu.CoursePlatform.service.KnowledgePointService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.*;

class KnowledgeExtractionServiceImplTest {

    private KnowledgeExtractionServiceImpl service;
    private AgenticClient agenticClient;
    private CourseResourceService courseResourceService;
    private KnowledgePointService knowledgePointService;
    private Map<String, KnowledgeExtractionCandidate> candidateStore;

    @BeforeEach
    void setUp() {
        agenticClient = mock(AgenticClient.class);
        courseResourceService = mock(CourseResourceService.class);
        knowledgePointService = mock(KnowledgePointService.class);
        candidateStore = new LinkedHashMap<>();

        KnowledgeExtractionCandidateMapper mapper = (KnowledgeExtractionCandidateMapper) Proxy.newProxyInstance(
                KnowledgeExtractionCandidateMapper.class.getClassLoader(),
                new Class<?>[]{KnowledgeExtractionCandidateMapper.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("insert".equals(name) && args != null && args.length == 1 && args[0] instanceof KnowledgeExtractionCandidate c) {
                        if (c.getCandidateId() == null) c.setCandidateId("c-" + (candidateStore.size() + 1));
                        candidateStore.put(c.getCandidateId(), c);
                        return 1;
                    }
                    if ("updateById".equals(name) && args != null && args.length >= 1 && args[0] instanceof KnowledgeExtractionCandidate c) {
                        if (candidateStore.containsKey(c.getCandidateId())) {
                            candidateStore.put(c.getCandidateId(), c);
                            return 1;
                        }
                        return 0;
                    }
                    if ("selectById".equals(name)) {
                        return candidateStore.get(String.valueOf(args[0]));
                    }
                    if ("selectList".equals(name)) {
                        return new ArrayList<>(candidateStore.values());
                    }
                    if ("toString".equals(name)) return "ExtractionMapperProxy";
                    if ("hashCode".equals(name)) return System.identityHashCode(proxy);
                    if ("equals".equals(name)) return proxy == args[0];
                    return null;
                });

        service = new KnowledgeExtractionServiceImpl(agenticClient, courseResourceService, mapper, knowledgePointService);
    }

    // ============ extract ============

    @Test
    void extractThrowsWhenResourceNotFound() {
        when(courseResourceService.getById("res-1")).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () -> service.extract("CS101", "res-1"));
    }

    @Test
    void extractThrowsWhenCourseCodeMismatch() {
        CourseResource res = resource("res-1", "CS202", "课件1");
        when(courseResourceService.getById("res-1")).thenReturn(res);
        assertThrows(IllegalArgumentException.class, () -> service.extract("CS101", "res-1"));
    }

    @Test
    void extractThrowsWhenAiResponseNotSuccess() {
        CourseResource res = resource("res-1", "CS101", "课件1");
        when(courseResourceService.getById("res-1")).thenReturn(res);
        AgenticResponse response = new AgenticResponse(false, Map.of(), "AI服务不可用");
        when(agenticClient.invoke(eq("extract"), any(AgenticRequest.class))).thenReturn(response);
        assertThrows(IllegalArgumentException.class, () -> service.extract("CS101", "res-1"));
    }

    @Test
    void extractSavesCandidatesFromAiResponse() {
        CourseResource res = resource("res-1", "CS101", "课件1");
        when(courseResourceService.getById("res-1")).thenReturn(res);
        AgenticResponse response = new AgenticResponse(true, Map.of("knowledgePoints", List.of(
                Map.of("name", "Java基础", "description", "Java基本语法", "importance", 5),
                Map.of("name", "OOP概念", "description", "面向对象编程", "importance", 4)
        )), "ok");
        when(agenticClient.invoke(eq("extract"), any(AgenticRequest.class))).thenReturn(response);

        List<KnowledgeExtractionCandidate> result = service.extract("CS101", "res-1");

        assertEquals(2, result.size());
        assertEquals("Java基础", result.get(0).getName());
        assertEquals("OOP概念", result.get(1).getName());
        assertEquals(2, candidateStore.size());
    }

    @Test
    void extractUsesCandidatesKeyAsFallback() {
        CourseResource res = resource("res-1", "CS101", "课件");
        when(courseResourceService.getById("res-1")).thenReturn(res);
        AgenticResponse response = new AgenticResponse(true, Map.of("candidates", List.of(
                Map.of("name", "知识点A")
        )), "ok");
        when(agenticClient.invoke(eq("extract"), any(AgenticRequest.class))).thenReturn(response);

        List<KnowledgeExtractionCandidate> result = service.extract("CS101", "res-1");
        assertEquals(1, result.size());
        assertEquals("知识点A", result.get(0).getName());
    }

    @Test
    void extractSkipsDraftsWithBlankName() {
        CourseResource res = resource("res-1", "CS101", "课件");
        when(courseResourceService.getById("res-1")).thenReturn(res);
        AgenticResponse response = new AgenticResponse(true, Map.of("knowledgePoints", List.of(
                Map.of("name", "  "),
                Map.of("name", "有效知识点", "importance", 3)
        )), "ok");
        when(agenticClient.invoke(eq("extract"), any(AgenticRequest.class))).thenReturn(response);

        List<KnowledgeExtractionCandidate> result = service.extract("CS101", "res-1");
        assertEquals(1, result.size());
        assertEquals("有效知识点", result.get(0).getName());
    }

    @Test
    void extractDefaultsChapterFromResource() {
        CourseResource res = resource("res-1", "CS101", "课件");
        res.setChapter("第三章");
        when(courseResourceService.getById("res-1")).thenReturn(res);
        AgenticResponse response = new AgenticResponse(true, Map.of("knowledgePoints", List.of(
                Map.of("name", "知识点X")
        )), "ok");
        when(agenticClient.invoke(eq("extract"), any(AgenticRequest.class))).thenReturn(response);

        List<KnowledgeExtractionCandidate> result = service.extract("CS101", "res-1");
        assertEquals("第三章", result.get(0).getChapter());
    }

    @Test
    void extractUsesDraftChapterOverResource() {
        CourseResource res = resource("res-1", "CS101", "课件");
        res.setChapter("第三章");
        when(courseResourceService.getById("res-1")).thenReturn(res);
        AgenticResponse response = new AgenticResponse(true, Map.of("knowledgePoints", List.of(
                Map.of("name", "知识点X", "chapter", "第四章")
        )), "ok");
        when(agenticClient.invoke(eq("extract"), any(AgenticRequest.class))).thenReturn(response);

        List<KnowledgeExtractionCandidate> result = service.extract("CS101", "res-1");
        assertEquals("第四章", result.get(0).getChapter());
    }

    @Test
    void extractClampsImportanceOutOfRange() {
        CourseResource res = resource("res-1", "CS101", "课件");
        when(courseResourceService.getById("res-1")).thenReturn(res);
        AgenticResponse response = new AgenticResponse(true, Map.of("knowledgePoints", List.of(
                Map.of("name", "K1", "importance", 10),
                Map.of("name", "K2", "importance", 0)
        )), "ok");
        when(agenticClient.invoke(eq("extract"), any(AgenticRequest.class))).thenReturn(response);

        List<KnowledgeExtractionCandidate> result = service.extract("CS101", "res-1");
        assertNull(result.get(0).getImportance());
        assertNull(result.get(1).getImportance());
    }

    @Test
    void extractReadsStringImportanceAndIgnoresInvalidText() {
        CourseResource res = resource("res-1", "CS101", "课件");
        when(courseResourceService.getById("res-1")).thenReturn(res);
        AgenticResponse response = new AgenticResponse(true, Map.of("knowledgePoints", List.of(
                Map.of("name", "K1", "importance", "4"),
                Map.of("name", "K2", "importance", "bad")
        )), "ok");
        when(agenticClient.invoke(eq("extract"), any(AgenticRequest.class))).thenReturn(response);

        List<KnowledgeExtractionCandidate> result = service.extract("CS101", "res-1");

        assertEquals(4, result.get(0).getImportance());
        assertNull(result.get(1).getImportance());
    }

    @Test
    void extractHandlesEmptyAiData() {
        CourseResource res = resource("res-1", "CS101", "课件");
        when(courseResourceService.getById("res-1")).thenReturn(res);
        AgenticResponse response = new AgenticResponse(true, Map.of(), "ok");
        when(agenticClient.invoke(eq("extract"), any(AgenticRequest.class))).thenReturn(response);

        List<KnowledgeExtractionCandidate> result = service.extract("CS101", "res-1");
        assertTrue(result.isEmpty());
    }

    @Test
    void extractHandlesNullAiData() {
        CourseResource res = resource("res-1", "CS101", "课件");
        when(courseResourceService.getById("res-1")).thenReturn(res);
        AgenticResponse response = new AgenticResponse(true, null, "ok");
        when(agenticClient.invoke(eq("extract"), any(AgenticRequest.class))).thenReturn(response);

        List<KnowledgeExtractionCandidate> result = service.extract("CS101", "res-1");
        assertTrue(result.isEmpty());
    }

    @Test
    void extractCandidatesHavePendingStatus() {
        CourseResource res = resource("res-1", "CS101", "课件");
        when(courseResourceService.getById("res-1")).thenReturn(res);
        AgenticResponse response = new AgenticResponse(true, Map.of("knowledgePoints", List.of(
                Map.of("name", "知识点")
        )), "ok");
        when(agenticClient.invoke(eq("extract"), any(AgenticRequest.class))).thenReturn(response);

        List<KnowledgeExtractionCandidate> result = service.extract("CS101", "res-1");
        assertEquals("pending", result.get(0).getStatus());
        assertNotNull(result.get(0).getCreatedAt());
    }

    @Test
    void listPendingDelegatesToMapper() {
        KnowledgeExtractionCandidate candidate = new KnowledgeExtractionCandidate();
        candidate.setCandidateId("c-list");
        candidate.setCourseCode("CS101");
        candidate.setStatus("pending");
        candidateStore.put("c-list", candidate);

        List<KnowledgeExtractionCandidate> result = service.listPending("CS101");

        assertEquals(1, result.size());
        assertEquals("c-list", result.get(0).getCandidateId());
    }

    // ============ accept ============

    @Test
    void acceptCreatesKnowledgePointAndMarksAccepted() {
        KnowledgeExtractionCandidate candidate = new KnowledgeExtractionCandidate();
        candidate.setCandidateId("c1");
        candidate.setCourseCode("CS101");
        candidate.setName("Java基础");
        candidate.setDescription("Java基本语法");
        candidate.setChapter("第一章");
        candidate.setImportance(5);
        candidate.setStatus("pending");
        candidateStore.put("c1", candidate);

        String error = service.accept("CS101", "c1",
                editedCandidate("Java基础", "描述", "第一章", 5));

        assertNull(error);
        verify(knowledgePointService).save(any(KnowledgePoint.class));
        assertEquals("accepted", candidateStore.get("c1").getStatus());
    }

    @Test
    void acceptReturnsErrorWhenNameBlank() {
        KnowledgeExtractionCandidate candidate = new KnowledgeExtractionCandidate();
        candidate.setCandidateId("c1");
        candidate.setCourseCode("CS101");
        candidate.setName("OldName");
        candidate.setStatus("pending");
        candidateStore.put("c1", candidate);

        String error = service.accept("CS101", "c1", editedCandidate("  ", "", "", null));
        assertEquals("知识点名称不能为空", error);
    }

    @Test
    void acceptReturnsErrorWhenEditedCandidateNull() {
        KnowledgeExtractionCandidate candidate = new KnowledgeExtractionCandidate();
        candidate.setCandidateId("c1");
        candidate.setCourseCode("CS101");
        candidate.setName("X");
        candidate.setStatus("pending");
        candidateStore.put("c1", candidate);

        String error = service.accept("CS101", "c1", null);
        assertEquals("知识点名称不能为空", error);
    }

    @Test
    void acceptReturnsErrorWhenImportanceOutOfRange() {
        KnowledgeExtractionCandidate candidate = new KnowledgeExtractionCandidate();
        candidate.setCandidateId("c1");
        candidate.setCourseCode("CS101");
        candidate.setName("X");
        candidate.setStatus("pending");
        candidateStore.put("c1", candidate);

        String error = service.accept("CS101", "c1", editedCandidate("Java", "", "", 10));
        assertEquals("知识点重要程度必须在 1 到 5 之间", error);
    }

    @Test
    void acceptThrowsWhenCandidateNotPending() {
        KnowledgeExtractionCandidate candidate = new KnowledgeExtractionCandidate();
        candidate.setCandidateId("c1");
        candidate.setCourseCode("CS101");
        candidate.setName("X");
        candidate.setStatus("accepted");
        candidateStore.put("c1", candidate);

        assertThrows(IllegalArgumentException.class, () ->
                service.accept("CS101", "c1", editedCandidate("Java", "", "", 3)));
    }

    @Test
    void acceptThrowsWhenCandidateNotFound() {
        assertThrows(IllegalArgumentException.class, () ->
                service.accept("CS101", "nonexistent", editedCandidate("Java", "", "", 3)));
    }

    @Test
    void acceptThrowsWhenCourseCodeMismatch() {
        KnowledgeExtractionCandidate candidate = new KnowledgeExtractionCandidate();
        candidate.setCandidateId("c1");
        candidate.setCourseCode("CS202");
        candidate.setName("X");
        candidate.setStatus("pending");
        candidateStore.put("c1", candidate);

        assertThrows(IllegalArgumentException.class, () ->
                service.accept("CS101", "c1", editedCandidate("Java", "", "", 3)));
    }

    // ============ reject ============

    @Test
    void rejectMarksCandidateAsRejected() {
        KnowledgeExtractionCandidate candidate = new KnowledgeExtractionCandidate();
        candidate.setCandidateId("c1");
        candidate.setCourseCode("CS101");
        candidate.setName("X");
        candidate.setStatus("pending");
        candidateStore.put("c1", candidate);

        String error = service.reject("CS101", "c1");
        assertNull(error);
        assertEquals("rejected", candidateStore.get("c1").getStatus());
    }

    @Test
    void rejectThrowsWhenCandidateNotFound() {
        assertThrows(IllegalArgumentException.class, () -> service.reject("CS101", "nonexistent"));
    }

    @Test
    void rejectThrowsWhenAlreadyProcessed() {
        KnowledgeExtractionCandidate candidate = new KnowledgeExtractionCandidate();
        candidate.setCandidateId("c1");
        candidate.setCourseCode("CS101");
        candidate.setName("X");
        candidate.setStatus("rejected");
        candidateStore.put("c1", candidate);

        assertThrows(IllegalArgumentException.class, () -> service.reject("CS101", "c1"));
    }

    // ============ helpers ============

    private static CourseResource resource(String id, String courseCode, String title) {
        CourseResource r = new CourseResource();
        r.setResourceId(id);
        r.setCourseCode(courseCode);
        r.setTitle(title);
        r.setResourceType("pdf");
        r.setOriginalFilename("");
        r.setChapter("");
        return r;
    }

    private static KnowledgeExtractionCandidate editedCandidate(String name, String description, String chapter, Integer importance) {
        KnowledgeExtractionCandidate c = new KnowledgeExtractionCandidate();
        c.setName(name);
        c.setDescription(description);
        c.setChapter(chapter);
        c.setImportance(importance);
        return c;
    }
}
