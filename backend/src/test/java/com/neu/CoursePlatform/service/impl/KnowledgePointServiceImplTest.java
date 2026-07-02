package com.neu.CoursePlatform.service.impl;

import static com.neu.CoursePlatform.service.TeacherServiceTest.setBaseMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.neu.CoursePlatform.entity.AbilityKnowledgePoint;
import com.neu.CoursePlatform.entity.CourseResource;
import com.neu.CoursePlatform.entity.KnowledgePoint;
import com.neu.CoursePlatform.entity.KnowledgeRelation;
import com.neu.CoursePlatform.mapper.AbilityKnowledgePointMapper;
import com.neu.CoursePlatform.mapper.CourseResourceMapper;
import com.neu.CoursePlatform.mapper.KnowledgePointMapper;
import com.neu.CoursePlatform.mapper.KnowledgeRelationMapper;
import com.neu.CoursePlatform.service.KnowledgeMasteryService;
import com.neu.CoursePlatform.service.QuestionService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class KnowledgePointServiceImplTest {

    private KnowledgePointServiceImpl service;
    private Map<String, KnowledgePoint> store;
    private Map<String, KnowledgeRelation> relationStore;
    private Map<String, CourseResource> resourceStore;
    private Map<String, AbilityKnowledgePoint> abilityKpStore;
    private Set<String> pointsWithQuestions;
    private boolean removeByKpCalled;

    private static final Pattern EQ_PATTERN =
            Pattern.compile("(\\w+)\\s*=\\s*#\\{ew\\.paramNameValuePairs\\.(\\w+)\\}", Pattern.CASE_INSENSITIVE);
    private static final Pattern LIKE_PATTERN =
            Pattern.compile("(\\w+)\\s+LIKE\\s+#\\{ew\\.paramNameValuePairs\\.(\\w+)\\}", Pattern.CASE_INSENSITIVE);

    @BeforeEach
    void setUp() throws Exception {
        store = new LinkedHashMap<>();
        relationStore = new LinkedHashMap<>();
        resourceStore = new LinkedHashMap<>();
        abilityKpStore = new LinkedHashMap<>();
        pointsWithQuestions = new HashSet<>();
        removeByKpCalled = false;

        // ========== 1) KnowledgePointMapper proxy ==========
        KnowledgePointMapper kpMapper = (KnowledgePointMapper) Proxy.newProxyInstance(
                KnowledgePointMapper.class.getClassLoader(),
                new Class<?>[]{KnowledgePointMapper.class},
                (proxy, method, args) -> {
                    String methodName = method.getName();
                    if ("selectList".equals(methodName)) {
                        Wrapper<?> wrapper = args != null && args.length >= 1 && args[0] instanceof Wrapper
                                ? (Wrapper<?>) args[0] : null;
                        return applyWrapperFilter(new ArrayList<>(store.values()), wrapper);
                    }
                    if ("deleteById".equals(methodName)) {
                        return store.remove(String.valueOf(args[0])) != null ? 1 : 0;
                    }
                    if ("insert".equals(methodName) && args != null && args.length == 1
                            && args[0] instanceof KnowledgePoint) {
                        KnowledgePoint kp = (KnowledgePoint) args[0];
                        if (kp.getKnowledgePointId() == null) {
                            kp.setKnowledgePointId(String.valueOf(store.size() + 1));
                        }
                        store.put(kp.getKnowledgePointId(), kp);
                        return 1;
                    }
                    if ("selectById".equals(methodName)) {
                        return store.get(String.valueOf(args[0]));
                    }
                    if ("selectCount".equals(methodName)) {
                        return (long) store.size();
                    }
                    if ("toString".equals(methodName)) return "KnowledgePointMapperProxy";
                    if ("hashCode".equals(methodName)) return System.identityHashCode(proxy);
                    if ("equals".equals(methodName)) return proxy == args[0];
                    return null;
                });

        // ========== 2) KnowledgeRelationMapper proxy ==========
        KnowledgeRelationMapper relationMapper = (KnowledgeRelationMapper) Proxy.newProxyInstance(
                KnowledgeRelationMapper.class.getClassLoader(),
                new Class<?>[]{KnowledgeRelationMapper.class},
                (proxy, method, args) -> {
                    String methodName = method.getName();
                    if ("selectCount".equals(methodName)) {
                        Wrapper<?> wrapper = args != null && args.length >= 1 && args[0] instanceof Wrapper
                                ? (Wrapper<?>) args[0] : null;
                        return (long) filterEntities(new ArrayList<>(relationStore.values()), wrapper).size();
                    }
                    if ("delete".equals(methodName)) {
                        Wrapper<?> wrapper = args != null && args.length >= 1 && args[0] instanceof Wrapper
                                ? (Wrapper<?>) args[0] : null;
                        List<KnowledgeRelation> toRemove = filterEntities(
                                new ArrayList<>(relationStore.values()), wrapper);
                        toRemove.forEach(r -> relationStore.remove(r.getRelationId()));
                        return toRemove.size();
                    }
                    if ("insert".equals(methodName) && args != null && args.length == 1
                            && args[0] instanceof KnowledgeRelation) {
                        KnowledgeRelation r = (KnowledgeRelation) args[0];
                        relationStore.put(r.getRelationId(), r);
                        return 1;
                    }
                    if ("toString".equals(methodName)) return "RelationMapperProxy";
                    if ("hashCode".equals(methodName)) return System.identityHashCode(proxy);
                    if ("equals".equals(methodName)) return proxy == args[0];
                    return null;
                });

        // ========== 3) CourseResourceMapper proxy ==========
        CourseResourceMapper resourceMapper = (CourseResourceMapper) Proxy.newProxyInstance(
                CourseResourceMapper.class.getClassLoader(),
                new Class<?>[]{CourseResourceMapper.class},
                (proxy, method, args) -> {
                    String methodName = method.getName();
                    if ("update".equals(methodName) && args != null && args.length >= 2) {
                        Wrapper<?> wrapper = args[1] instanceof Wrapper ? (Wrapper<?>) args[1] : null;
                        if (wrapper != null) {
                            int count = 0;
                            for (CourseResource r : resourceStore.values()) {
                                if (evaluateExpression(r, wrapper.getSqlSegment(),
                                        getParams(wrapper))) {
                                    r.setKnowledgePointId(null);
                                    count++;
                                }
                            }
                            return count;
                        }
                        return 0;
                    }
                    if ("insert".equals(methodName) && args != null && args.length == 1
                            && args[0] instanceof CourseResource) {
                        CourseResource r = (CourseResource) args[0];
                        resourceStore.put(r.getResourceId(), r);
                        return 1;
                    }
                    if ("toString".equals(methodName)) return "ResourceMapperProxy";
                    if ("hashCode".equals(methodName)) return System.identityHashCode(proxy);
                    if ("equals".equals(methodName)) return proxy == args[0];
                    return null;
                });

        // ========== 4) AbilityKnowledgePointMapper proxy ==========
        AbilityKnowledgePointMapper abilityMapper = (AbilityKnowledgePointMapper) Proxy.newProxyInstance(
                AbilityKnowledgePointMapper.class.getClassLoader(),
                new Class<?>[]{AbilityKnowledgePointMapper.class},
                (proxy, method, args) -> {
                    String methodName = method.getName();
                    if ("delete".equals(methodName)) {
                        Wrapper<?> wrapper = args != null && args.length >= 1 && args[0] instanceof Wrapper
                                ? (Wrapper<?>) args[0] : null;
                        List<AbilityKnowledgePoint> toRemove = new ArrayList<>();
                        if (wrapper != null) {
                            for (AbilityKnowledgePoint akp : abilityKpStore.values()) {
                                if (evaluateExpression(akp, wrapper.getSqlSegment(),
                                        getParams(wrapper))) {
                                    toRemove.add(akp);
                                }
                            }
                        }
                        toRemove.forEach(a -> abilityKpStore.remove(a.getId()));
                        return toRemove.size();
                    }
                    if ("insert".equals(methodName) && args != null && args.length == 1
                            && args[0] instanceof AbilityKnowledgePoint) {
                        AbilityKnowledgePoint akp = (AbilityKnowledgePoint) args[0];
                        abilityKpStore.put(akp.getId(), akp);
                        return 1;
                    }
                    if ("toString".equals(methodName)) return "AbilityMapperProxy";
                    if ("hashCode".equals(methodName)) return System.identityHashCode(proxy);
                    if ("equals".equals(methodName)) return proxy == args[0];
                    return null;
                });

        // ========== 5) KnowledgeMasteryService mock ==========
        KnowledgeMasteryService masteryService = mock(KnowledgeMasteryService.class);
        doAnswer(inv -> {
            removeByKpCalled = true;
            return 1;
        }).when(masteryService).removeByKnowledgePoint(anyString());

        // ========== 6) QuestionService mock ==========
        QuestionService questionService = mock(QuestionService.class);
        doAnswer(inv -> {
            Wrapper<?> wrapper = inv.getArgument(0);
            if (wrapper != null) {
                Map<String, Object> params = getParams(wrapper);
                for (Object val : params.values()) {
                    if (val != null && pointsWithQuestions.contains(val.toString())) {
                        return 1L;
                    }
                }
            }
            return 0L;
        }).when(questionService).count(any());

        // ========== Instantiate and inject baseMapper ==========
        service = new KnowledgePointServiceImpl(
                relationMapper, resourceMapper, abilityMapper, masteryService, questionService);
        setBaseMapper(service, kpMapper);
    }

    // ================================================================
    // listByCourse
    // ================================================================

    @Test
    void listByCourse_ReturnsKnowledgePointsForCourseCode() {
        store.put("1", kp("1", "CS101", "L01", "Ch1", "Java基础", "Java入门知识"));
        store.put("2", kp("2", "CS101", "L02", "Ch1", "面向对象", "封装继承多态"));
        store.put("3", kp("3", "CS102", "L01", "Ch1", "数据库基础", "SQL语法"));

        List<KnowledgePoint> result = service.listByCourse("CS101", null, null);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(k -> "CS101".equals(k.getCourseCode())));
    }

    @Test
    void listByCourse_FiltersByLessonNo() {
        store.put("1", kp("1", "CS101", "L01", "Ch1", "Java基础", "Java入门"));
        store.put("2", kp("2", "CS101", "L01", "Ch2", "变量", "变量声明"));
        store.put("3", kp("3", "CS101", "L02", "Ch1", "Spring", "Spring框架"));

        List<KnowledgePoint> result = service.listByCourse("CS101", "L01", null);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(k -> "L01".equals(k.getLessonNo())));
    }

    @Test
    void listByCourse_FiltersByKeyword() {
        store.put("1", kp("1", "CS101", "L01", "Ch1", "Java入门", "基础语法"));
        store.put("2", kp("2", "CS101", "L01", "Ch2", "变量类型", "包含变量相关知识"));
        store.put("3", kp("3", "CS101", "L02", "Ch1", "Spring", "Spring框架入门"));

        List<KnowledgePoint> result = service.listByCourse("CS101", null, "变量");

        assertEquals(1, result.size());
        assertEquals("2", result.get(0).getKnowledgePointId());
    }

    @Test
    void listByCourse_ReturnsEmptyListForNonExistentCourse() {
        store.put("1", kp("1", "CS101", "L01", "Ch1", "Java", "Java知识"));

        List<KnowledgePoint> result = service.listByCourse("NONEXISTENT", null, null);

        assertTrue(result.isEmpty());
    }

    // ================================================================
    // listByCourseCode
    // ================================================================

    @Test
    void listByCourseCode_ReturnsPointsFilteredByCourseCode() {
        store.put("1", kp("1", "CS101", "L01", "Ch1", "Java基础", "入门"));
        store.put("2", kp("2", "CS101", "L02", "Ch2", "OOP", "面向对象"));
        store.put("3", kp("3", "CS102", "L01", "Ch1", "DB", "数据库"));

        List<KnowledgePoint> result = service.listByCourseCode("CS101", null);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(k -> "CS101".equals(k.getCourseCode())));
    }

    @Test
    void listByCourseCode_FiltersByChapter() {
        store.put("1", kp("1", "CS101", "L01", "Ch1", "Java基础", "入门"));
        store.put("2", kp("2", "CS101", "L02", "Ch1", "OOP", "面向对象"));
        store.put("3", kp("3", "CS101", "L01", "Ch2", "集合", "集合框架"));

        List<KnowledgePoint> result = service.listByCourseCode("CS101", "Ch1");

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(k -> "Ch1".equals(k.getChapter())));
    }

    // ================================================================
    // removePoint
    // ================================================================

    @Test
    void removePoint_ThrowsWhenQuestionsReferenceThePoint() {
        store.put("1", kp("1", "CS101", "L01", "Ch1", "Java", "desc"));
        pointsWithQuestions.add("1");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.removePoint("1"));
        assertTrue(ex.getMessage().contains("题目引用"));
        assertTrue(store.containsKey("1"));
    }

    @Test
    void removePoint_ThrowsWhenRelationsReferenceThePoint() {
        store.put("1", kp("1", "CS101", "L01", "Ch1", "Java", "desc"));
        relationStore.put("r1", relation("r1", "CS101", "1", "2", "precedes"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.removePoint("1"));
        assertTrue(ex.getMessage().contains("知识关系"));
        assertTrue(store.containsKey("1"));
    }

    @Test
    void removePoint_SucceedsWhenNoReferencesExist() {
        store.put("1", kp("1", "CS101", "L01", "Ch1", "Java", "desc"));

        assertDoesNotThrow(() -> service.removePoint("1"));
        assertFalse(store.containsKey("1"));
    }

    // ================================================================
    // deleteWithDependencies
    // ================================================================

    @Test
    void deleteWithDependencies_RemovesRelationsResourceRefsAbilityMappingsAndMasteryData() {
        store.put("1", kp("1", "CS101", "L01", "Ch1", "Java", "desc"));
        relationStore.put("r1", relation("r1", "CS101", "1", "2", "precedes"));
        relationStore.put("r2", relation("r2", "CS101", "3", "1", "related"));
        resourceStore.put("res1", resource("res1", "1"));
        resourceStore.put("res2", resource("res2", "99"));
        abilityKpStore.put("ak1", abilityKp("ak1", "abil1", "1"));
        abilityKpStore.put("ak2", abilityKp("ak2", "abil2", "1"));

        boolean result = service.deleteWithDependencies("1");

        assertTrue(result);
        assertFalse(store.containsKey("1"));
        assertTrue(relationStore.isEmpty());
        assertNull(resourceStore.get("res1").getKnowledgePointId());
        assertEquals("99", resourceStore.get("res2").getKnowledgePointId());
        assertTrue(abilityKpStore.isEmpty());
        assertTrue(removeByKpCalled);
    }

    @Test
    void deleteWithDependencies_ThrowsWhenQuestionsReferenceThePoint() {
        store.put("1", kp("1", "CS101", "L01", "Ch1", "Java", "desc"));
        pointsWithQuestions.add("1");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.deleteWithDependencies("1"));
        assertTrue(ex.getMessage().contains("题目引用"));
        assertTrue(store.containsKey("1"));
    }

    // ================================================================
    // Helpers -- entity builders
    // ================================================================

    private KnowledgePoint kp(String id, String courseCode, String lessonNo,
                               String chapter, String name, String description) {
        KnowledgePoint kp = new KnowledgePoint();
        kp.setKnowledgePointId(id);
        kp.setCourseCode(courseCode);
        kp.setLessonNo(lessonNo);
        kp.setChapter(chapter);
        kp.setName(name);
        kp.setDescription(description);
        return kp;
    }

    private KnowledgeRelation relation(String id, String courseCode,
                                        String fromId, String toId, String type) {
        KnowledgeRelation r = new KnowledgeRelation();
        r.setRelationId(id);
        r.setCourseCode(courseCode);
        r.setFromKnowledgePointId(fromId);
        r.setToKnowledgePointId(toId);
        r.setRelationType(type);
        return r;
    }

    private CourseResource resource(String id, String knowledgePointId) {
        CourseResource r = new CourseResource();
        r.setResourceId(id);
        r.setKnowledgePointId(knowledgePointId);
        return r;
    }

    private AbilityKnowledgePoint abilityKp(String id, String abilityPointId,
                                             String knowledgePointId) {
        AbilityKnowledgePoint a = new AbilityKnowledgePoint();
        a.setId(id);
        a.setAbilityPointId(abilityPointId);
        a.setKnowledgePointId(knowledgePointId);
        return a;
    }

    // ================================================================
    // Helpers -- wrapper-based filtering
    // ================================================================

    @SuppressWarnings("unchecked")
    private static Map<String, Object> getParams(Wrapper<?> wrapper) {
        if (wrapper == null) return Collections.emptyMap();
        try {
            return (Map<String, Object>) wrapper.getClass()
                    .getMethod("getParamNameValuePairs").invoke(wrapper);
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private List<KnowledgePoint> applyWrapperFilter(List<KnowledgePoint> all, Wrapper<?> wrapper) {
        if (wrapper == null || all.isEmpty()) return all;
        String sql = wrapper.getSqlSegment();
        Map<String, Object> params = getParams(wrapper);
        if (sql == null || sql.isBlank()) return all;
        return all.stream()
                .filter(kp -> evaluateExpression(kp, sql, params))
                .collect(Collectors.toList());
    }

    private <T> List<T> filterEntities(List<T> all, Wrapper<?> wrapper) {
        if (wrapper == null || all.isEmpty()) return all;
        String sql = wrapper.getSqlSegment();
        Map<String, Object> params = getParams(wrapper);
        if (sql == null || sql.isBlank()) return all;
        return all.stream()
                .filter(entity -> evaluateExpression(entity, sql, params))
                .collect(Collectors.toList());
    }

    private boolean evaluateExpression(Object entity, String sql, Map<String, Object> params) {
        if (sql == null || sql.isBlank()) return true;

        List<String> andParts = splitTopLevel(sql.trim(), "AND");
        if (andParts.isEmpty()) return true;

        for (String andPart : andParts) {
            andPart = andPart.trim();
            if (andPart.isEmpty()) continue;

            if (andPart.startsWith("(") && andPart.endsWith(")")) {
                String inner = andPart.substring(1, andPart.length() - 1).trim();
                List<String> orParts = splitTopLevel(inner, "OR");
                boolean anyMatch = orParts.stream()
                        .anyMatch(op -> matchesSimpleCondition(entity, op.trim(), params));
                if (!anyMatch) return false;
            } else {
                List<String> orParts = splitTopLevel(andPart, "OR");
                if (orParts.size() > 1) {
                    boolean anyMatch = orParts.stream()
                            .anyMatch(op -> matchesSimpleCondition(entity, op.trim(), params));
                    if (!anyMatch) return false;
                } else {
                    if (!matchesSimpleCondition(entity, andPart, params)) return false;
                }
            }
        }
        return true;
    }

    private static final Pattern TRAILING_CLAUSE =
            Pattern.compile("\\s+(ORDER\\s+BY|GROUP\\s+BY|HAVING|LIMIT|OFFSET).*$",
                    Pattern.CASE_INSENSITIVE);

    private String stripTrailingClauses(String condition) {
        return TRAILING_CLAUSE.matcher(condition.trim()).replaceFirst("");
    }

    private boolean matchesSimpleCondition(Object entity, String condition,
                                           Map<String, Object> params) {
        condition = stripTrailingClauses(condition.trim());

        Matcher likeMatcher = LIKE_PATTERN.matcher(condition);
        if (likeMatcher.matches()) {
            String column = likeMatcher.group(1);
            String paramKey = likeMatcher.group(2);
            Object paramValue = params.get(paramKey);
            if (paramValue == null) return false;
            String searchValue = paramValue.toString().replace("%", "");
            String fieldValue = getFieldValue(entity, column);
            return fieldValue != null && fieldValue.toLowerCase().contains(searchValue.toLowerCase());
        }

        Matcher eqMatcher = EQ_PATTERN.matcher(condition);
        if (eqMatcher.matches()) {
            String column = eqMatcher.group(1);
            String paramKey = eqMatcher.group(2);
            Object paramValue = params.get(paramKey);
            String fieldValue = getFieldValue(entity, column);
            if (paramValue == null) {
                return fieldValue == null;
            }
            return paramValue.toString().equals(fieldValue);
        }

        return true;
    }

    private String getFieldValue(Object entity, String columnName) {
        if (entity instanceof KnowledgePoint) {
            KnowledgePoint kp = (KnowledgePoint) entity;
            switch (columnName.toLowerCase()) {
                case "course_code": return kp.getCourseCode();
                case "lesson_no": return kp.getLessonNo();
                case "name": return kp.getName();
                case "description": return kp.getDescription();
                case "chapter": return kp.getChapter();
                case "knowledge_point_id": return kp.getKnowledgePointId();
                default: return null;
            }
        } else if (entity instanceof KnowledgeRelation) {
            KnowledgeRelation r = (KnowledgeRelation) entity;
            switch (columnName.toLowerCase()) {
                case "from_knowledge_point_id": return r.getFromKnowledgePointId();
                case "to_knowledge_point_id": return r.getToKnowledgePointId();
                case "course_code": return r.getCourseCode();
                default: return null;
            }
        } else if (entity instanceof CourseResource) {
            CourseResource r = (CourseResource) entity;
            if ("knowledge_point_id".equalsIgnoreCase(columnName)) return r.getKnowledgePointId();
            if ("course_code".equalsIgnoreCase(columnName)) return r.getCourseCode();
            return null;
        } else if (entity instanceof AbilityKnowledgePoint) {
            AbilityKnowledgePoint a = (AbilityKnowledgePoint) entity;
            if ("knowledge_point_id".equalsIgnoreCase(columnName)) return a.getKnowledgePointId();
            if ("ability_point_id".equalsIgnoreCase(columnName)) return a.getAbilityPointId();
            return null;
        }
        return null;
    }

    static List<String> splitTopLevel(String sql, String keyword) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        int lastSplit = 0;
        String upper = sql.toUpperCase();
        String kwUpper = keyword.toUpperCase();
        int i = 0;
        while (i < upper.length()) {
            char c = sql.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            } else if (depth == 0 && i + kwUpper.length() <= upper.length()
                    && upper.substring(i, i + kwUpper.length()).equals(kwUpper)) {
                boolean leftOk = i == 0 || !Character.isLetterOrDigit(sql.charAt(i - 1));
                boolean rightOk = i + kwUpper.length() >= sql.length()
                        || !Character.isLetterOrDigit(sql.charAt(i + kwUpper.length()));
                if (leftOk && rightOk) {
                    parts.add(sql.substring(lastSplit, i).trim());
                    lastSplit = i + keyword.length();
                    i += keyword.length();
                    continue;
                }
            }
            i++;
        }
        parts.add(sql.substring(lastSplit).trim());
        return parts;
    }
}
