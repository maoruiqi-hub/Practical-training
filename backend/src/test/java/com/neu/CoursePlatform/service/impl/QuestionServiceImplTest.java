package com.neu.CoursePlatform.service.impl;

import com.neu.CoursePlatform.dto.ExamGenerateRequest;
import com.neu.CoursePlatform.entity.Question;
import com.neu.CoursePlatform.mapper.QuestionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.*;
import java.util.stream.Collectors;

import static com.neu.CoursePlatform.service.TeacherServiceTest.setBaseMapper;
import static org.junit.jupiter.api.Assertions.*;

/**
 * QuestionServiceImpl 单元测试。
 * 使用与 TeacherServiceTest 相同的动态代理模式注入 QuestionMapper。
 */
class QuestionServiceImplTest {

    private QuestionServiceImpl service;
    private Map<String, Question> store;

    // ==================== setUp ====================

    @BeforeEach
    void setUp() throws Exception {
        store = new LinkedHashMap<>();

        QuestionMapper mapperProxy = (QuestionMapper) Proxy.newProxyInstance(
                QuestionMapper.class.getClassLoader(),
                new Class<?>[]{QuestionMapper.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("selectByCourseCode".equals(name)) {
                        String courseCode = (String) args[0];
                        return store.values().stream()
                                .filter(q -> courseCode != null && courseCode.equals(q.getCourseCode()))
                                .collect(Collectors.toList());
                    }
                    if ("selectByLessonNo".equals(name)) {
                        String lessonNo = (String) args[0];
                        return store.values().stream()
                                .filter(q -> lessonNo != null && lessonNo.equals(q.getLessonNo()))
                                .collect(Collectors.toList());
                    }
                    if ("selectByKeyword".equals(name)) {
                        String keyword = (String) args[0];
                        if (keyword == null || keyword.isEmpty()) return new ArrayList<>(store.values());
                        return store.values().stream()
                                .filter(q -> q.getStem() != null && q.getStem().contains(keyword))
                                .collect(Collectors.toList());
                    }
                    if ("selectByFilter".equals(name)) {
                        String courseCode = (String) args[0];
                        String lessonNo = (String) args[1];
                        String knowledgePointId = (String) args[2];
                        String type = (String) args[3];
                        Integer difficulty = (Integer) args[4];
                        String keyword = (String) args[5];
                        return store.values().stream()
                                .filter(q -> courseCode == null || courseCode.isEmpty() || courseCode.equals(q.getCourseCode()))
                                .filter(q -> lessonNo == null || lessonNo.isEmpty() || lessonNo.equals(q.getLessonNo()))
                                .filter(q -> knowledgePointId == null || knowledgePointId.isEmpty() || knowledgePointId.equals(q.getKnowledgePointId()))
                                .filter(q -> type == null || type.isEmpty() || type.equals(q.getType()))
                                .filter(q -> difficulty == null || difficulty.equals(q.getDifficulty()))
                                .filter(q -> keyword == null || keyword.isEmpty() || (q.getStem() != null && q.getStem().contains(keyword)))
                                .collect(Collectors.toList());
                    }
                    if ("toString".equals(name)) return "QuestionMapperProxy";
                    if ("hashCode".equals(name)) return System.identityHashCode(proxy);
                    if ("equals".equals(name)) return proxy == args[0];
                    return null;
                }
        );

        service = new QuestionServiceImpl();
        setBaseMapper(service, mapperProxy);
    }

    // ==================== helper ====================

    private Question question(String id, String courseCode, String lessonNo, String type,
                              String stem, String answer, Integer difficulty,
                              String knowledgePointId, Integer score) {
        Question q = new Question();
        q.setQuestionId(id);
        q.setCourseCode(courseCode);
        q.setLessonNo(lessonNo);
        q.setType(type);
        q.setStem(stem);
        q.setAnswer(answer);
        q.setDifficulty(difficulty);
        q.setKnowledgePointId(knowledgePointId);
        q.setScore(score);
        return q;
    }

    private void put(Question q) {
        store.put(q.getQuestionId(), q);
    }

    /** 批量创建指定数量的题目，自动编号和分配题型/难度/知识点。 */
    private void seedQuestions(String courseCode, int count) {
        String[] types = {"single", "multi", "fill", "essay"};
        for (int i = 1; i <= count; i++) {
            String id = "Q" + store.size();
            put(question(id, courseCode, "L" + ((i % 5) + 1),
                    types[i % types.length],
                    "题干内容 " + i + " Java基础",
                    "答案" + i,
                    (i % 5) + 1,
                    "KP" + ((i % 4) + 1),
                    5));
        }
    }

    // ==================== listByCourseCode ====================

    @Nested
    @DisplayName("listByCourseCode")
    class ListByCourseCodeTests {

        @Test
        @DisplayName("按课程代码筛选题目")
        void filtersByCourseCode() {
            put(question("1", "CS101", "L1", "single", "题目A", "A", 3, "KP1", 5));
            put(question("2", "CS101", "L2", "multi", "题目B", "B", 2, "KP2", 5));
            put(question("3", "CS102", "L1", "single", "题目C", "C", 4, "KP1", 5));

            List<Question> result = service.listByCourseCode("CS101");
            assertEquals(2, result.size());
            assertTrue(result.stream().allMatch(q -> "CS101".equals(q.getCourseCode())));
        }

        @Test
        @DisplayName("不存在的课程代码返回空列表")
        void unknownCourseReturnsEmpty() {
            put(question("1", "CS101", "L1", "single", "题目A", "A", 3, "KP1", 5));

            List<Question> result = service.listByCourseCode("CS999");
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    // ==================== listByLessonNo ====================

    @Nested
    @DisplayName("listByLessonNo")
    class ListByLessonNoTests {

        @Test
        @DisplayName("按课时编号筛选题目")
        void filtersByLessonNo() {
            put(question("1", "CS101", "L1", "single", "题目A", "A", 3, "KP1", 5));
            put(question("2", "CS101", "L1", "multi", "题目B", "B", 2, "KP2", 5));
            put(question("3", "CS101", "L2", "fill", "题目C", "C", 4, "KP1", 5));

            List<Question> result = service.listByLessonNo("L1");
            assertEquals(2, result.size());
            assertTrue(result.stream().allMatch(q -> "L1".equals(q.getLessonNo())));
        }

        @Test
        @DisplayName("不存在的课时编号返回空列表")
        void unknownLessonNoReturnsEmpty() {
            put(question("1", "CS101", "L1", "single", "题目A", "A", 3, "KP1", 5));

            List<Question> result = service.listByLessonNo("L99");
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    // ==================== searchByKeyword ====================

    @Nested
    @DisplayName("searchByKeyword")
    class SearchByKeywordTests {

        @Test
        @DisplayName("按关键词搜索题干")
        void searchesByKeyword() {
            put(question("1", "CS101", "L1", "single", "Java多态的理解", "A", 3, "KP1", 5));
            put(question("2", "CS101", "L1", "multi", "Spring框架原理", "B", 2, "KP2", 5));
            put(question("3", "CS101", "L2", "fill", "数据库事务特性", "ACID", 4, "KP3", 5));

            List<Question> result = service.searchByKeyword("Java");
            assertEquals(1, result.size());
            assertEquals("Java多态的理解", result.get(0).getStem());
        }

        @Test
        @DisplayName("关键词为空时返回全部题目")
        void emptyKeywordReturnsAll() {
            put(question("1", "CS101", "L1", "single", "题目A", "A", 3, "KP1", 5));
            put(question("2", "CS101", "L1", "multi", "题目B", "B", 2, "KP2", 5));

            assertEquals(2, service.searchByKeyword("").size());
            assertEquals(2, service.searchByKeyword(null).size());
        }

        @Test
        @DisplayName("无匹配关键词时返回空列表")
        void noMatchReturnsEmpty() {
            put(question("1", "CS101", "L1", "single", "题目A", "A", 3, "KP1", 5));

            List<Question> result = service.searchByKeyword("Python");
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    // ==================== filterQuestions ====================

    @Nested
    @DisplayName("filterQuestions")
    class FilterQuestionsTests {

        @Test
        @DisplayName("组合多个筛选条件")
        void filtersWithMultipleCriteria() {
            put(question("1", "CS101", "L1", "single", "题目A", "A", 3, "KP1", 5));
            put(question("2", "CS101", "L1", "multi", "题目B", "B", 3, "KP2", 5));
            put(question("3", "CS101", "L2", "single", "题目C", "C", 3, "KP1", 5));
            put(question("4", "CS102", "L1", "single", "题目D", "D", 3, "KP1", 5));

            // CS101 + L1 + type=single + difficulty=3
            List<Question> result = service.filterQuestions("CS101", "L1", null, "single", 3, null);
            assertEquals(1, result.size());
            assertEquals("1", result.get(0).getQuestionId());
        }

        @Test
        @DisplayName("单一条件筛选")
        void filtersWithSingleCriterion() {
            put(question("1", "CS101", "L1", "single", "题目A", "A", 3, "KP1", 5));
            put(question("2", "CS101", "L2", "multi", "题目B", "B", 2, "KP2", 5));

            List<Question> result = service.filterQuestions(null, null, null, "multi", null, null);
            assertEquals(1, result.size());
            assertEquals("multi", result.get(0).getType());
        }

        @Test
        @DisplayName("组合筛选含关键词")
        void filtersWithKeyword() {
            put(question("1", "CS101", "L1", "single", "Java基础", "A", 3, "KP1", 5));
            put(question("2", "CS101", "L1", "multi", "Python入门", "B", 2, "KP2", 5));

            List<Question> result = service.filterQuestions("CS101", null, null, null, null, "Java");
            assertEquals(1, result.size());
            assertEquals("Java基础", result.get(0).getStem());
        }

        @Test
        @DisplayName("无匹配结果时返回空列表")
        void noMatchReturnsEmpty() {
            put(question("1", "CS101", "L1", "single", "题目A", "A", 3, "KP1", 5));

            List<Question> result = service.filterQuestions("CS999", null, null, null, null, null);
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("所有参数为null时返回全部")
        void allNullReturnsAll() {
            put(question("1", "CS101", "L1", "single", "题目A", "A", 3, "KP1", 5));
            put(question("2", "CS102", "L2", "multi", "题目B", "B", 2, "KP2", 5));

            List<Question> result = service.filterQuestions(null, null, null, null, null, null);
            assertEquals(2, result.size());
        }
    }

    // ==================== generateExam — 基础流程 ====================

    @Nested
    @DisplayName("generateExam 基础流程")
    class GenerateExamBasicTests {

        @Test
        @DisplayName("指定 count 生成试卷")
        void generatesWithCount() {
            seedQuestions("CS101", 20);

            ExamGenerateRequest req = new ExamGenerateRequest();
            req.setCount(5);

            List<Question> exam = service.generateExam("CS101", req);
            assertEquals(5, exam.size());
        }

        @Test
        @DisplayName("count 为 null 默认 10 道题")
        void nullCountDefaultsToTen() {
            seedQuestions("CS101", 20);

            ExamGenerateRequest req = new ExamGenerateRequest();
            req.setCount(null);

            List<Question> exam = service.generateExam("CS101", req);
            assertEquals(10, exam.size());
        }

        @Test
        @DisplayName("指定 types 过滤题型")
        void filtersByTypes() {
            put(question("1", "CS101", "L1", "single", "单选题", "A", 3, "KP1", 5));
            put(question("2", "CS101", "L1", "single", "单选题2", "B", 2, "KP1", 5));
            put(question("3", "CS101", "L1", "multi", "多选题", "AB", 4, "KP1", 5));
            put(question("4", "CS101", "L1", "fill", "填空题", "答案", 1, "KP1", 5));
            put(question("5", "CS101", "L1", "essay", "简答题", "详细", 5, "KP1", 5));

            ExamGenerateRequest req = new ExamGenerateRequest();
            req.setCount(3);
            req.setTypes(List.of("single", "multi"));

            List<Question> exam = service.generateExam("CS101", req);
            assertEquals(3, exam.size());
            assertTrue(exam.stream().allMatch(q ->
                    "single".equals(q.getType()) || "multi".equals(q.getType())));
        }

        @Test
        @DisplayName("指定 knowledgePointIds 过滤知识点")
        void filtersByKnowledgePointIds() {
            put(question("1", "CS101", "L1", "single", "题目A", "A", 3, "KP1", 5));
            put(question("2", "CS101", "L1", "single", "题目B", "B", 3, "KP2", 5));
            put(question("3", "CS101", "L1", "single", "题目C", "C", 3, "KP1", 5));

            ExamGenerateRequest req = new ExamGenerateRequest();
            req.setCount(2);
            req.setKnowledgePointIds(List.of("KP1"));

            List<Question> exam = service.generateExam("CS101", req);
            assertEquals(2, exam.size());
            assertTrue(exam.stream().allMatch(q -> "KP1".equals(q.getKnowledgePointId())));
        }

        @Test
        @DisplayName("指定 difficultyMin / difficultyMax 过滤难度")
        void filtersByDifficultyRange() {
            put(question("1", "CS101", "L1", "single", "题目A", "A", 1, "KP1", 5));
            put(question("2", "CS101", "L1", "single", "题目B", "B", 2, "KP1", 5));
            put(question("3", "CS101", "L1", "single", "题目C", "C", 3, "KP1", 5));
            put(question("4", "CS101", "L1", "single", "题目D", "D", 4, "KP1", 5));
            put(question("5", "CS101", "L1", "single", "题目E", "E", 5, "KP1", 5));

            ExamGenerateRequest req = new ExamGenerateRequest();
            req.setCount(3);
            req.setDifficultyMin(2);
            req.setDifficultyMax(4);

            List<Question> exam = service.generateExam("CS101", req);
            assertEquals(3, exam.size());
            assertTrue(exam.stream().allMatch(q -> q.getDifficulty() >= 2 && q.getDifficulty() <= 4));
        }
    }

    // ==================== generateExam — 异常场景 ====================

    @Nested
    @DisplayName("generateExam 异常场景")
    class GenerateExamExceptionTests {

        @Test
        @DisplayName("request 为 null 时抛出 IllegalArgumentException")
        void nullRequestThrows() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.generateExam("CS101", null));
            assertEquals("组卷参数不能为空", ex.getMessage());
        }

        @Test
        @DisplayName("count 为 0 时抛出异常")
        void countZeroThrows() {
            seedQuestions("CS101", 10);

            ExamGenerateRequest req = new ExamGenerateRequest();
            req.setCount(0);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.generateExam("CS101", req));
            assertEquals("题目数量必须大于 0", ex.getMessage());
        }

        @Test
        @DisplayName("count 为负数时抛出异常")
        void countNegativeThrows() {
            seedQuestions("CS101", 10);

            ExamGenerateRequest req = new ExamGenerateRequest();
            req.setCount(-5);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.generateExam("CS101", req));
            assertEquals("题目数量必须大于 0", ex.getMessage());
        }

        @Test
        @DisplayName("题库题目不足时抛出异常")
        void insufficientQuestionsThrows() {
            seedQuestions("CS101", 3);

            ExamGenerateRequest req = new ExamGenerateRequest();
            req.setCount(10);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.generateExam("CS101", req));
            assertTrue(ex.getMessage().contains("符合条件的题目不足"));
            assertTrue(ex.getMessage().contains("10"));
        }

        @Test
        @DisplayName("课程下无题目时抛出异常（空候选集）")
        void emptyCandidatesThrows() {
            seedQuestions("CS101", 5);

            ExamGenerateRequest req = new ExamGenerateRequest();
            req.setCount(3);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.generateExam("CS999", req));
            assertEquals("没有符合条件的题目", ex.getMessage());
        }
    }

    // ==================== generateExam — typeCounts ====================

    @Nested
    @DisplayName("generateExam typeCounts 按题型数量组卷")
    class GenerateExamTypeCountsTests {

        @Test
        @DisplayName("按题型数量组卷成功")
        void generatesByTypeCounts() {
            for (int i = 0; i < 10; i++) {
                put(question("S" + i, "CS101", "L1", "single", "单选" + i, "A", 3, "KP1", 5));
            }
            for (int i = 0; i < 10; i++) {
                put(question("M" + i, "CS101", "L1", "multi", "多选" + i, "AB", 2, "KP1", 5));
            }
            for (int i = 0; i < 10; i++) {
                put(question("F" + i, "CS101", "L1", "fill", "填空" + i, "答案", 4, "KP1", 5));
            }

            ExamGenerateRequest req = new ExamGenerateRequest();
            req.setTypeCounts(Map.of("single", 3, "multi", 2));
            req.setCount(5);

            List<Question> exam = service.generateExam("CS101", req);
            assertEquals(5, exam.size());
            long singleCount = exam.stream().filter(q -> "single".equals(q.getType())).count();
            long multiCount = exam.stream().filter(q -> "multi".equals(q.getType())).count();
            assertEquals(3, singleCount);
            assertEquals(2, multiCount);
        }

        @Test
        @DisplayName("typeCounts 题型不足时抛出异常")
        void typeCountsInsufficientThrows() {
            put(question("1", "CS101", "L1", "single", "单选1", "A", 3, "KP1", 5));
            put(question("2", "CS101", "L1", "single", "单选2", "B", 3, "KP1", 5));

            ExamGenerateRequest req = new ExamGenerateRequest();
            req.setTypeCounts(Map.of("single", 3, "multi", 1));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.generateExam("CS101", req));
            assertTrue(ex.getMessage().contains("符合题型数量要求的题目不足"));
        }

        @Test
        @DisplayName("typeCounts 包含非法键值对时被忽略")
        void typeCountsSkipsInvalidEntries() {
            for (int i = 0; i < 10; i++) {
                put(question("S" + i, "CS101", "L1", "single", "单选" + i, "A", 3, "KP1", 5));
            }

            ExamGenerateRequest req = new ExamGenerateRequest();
            Map<String, Integer> typeCounts = new LinkedHashMap<>();
            typeCounts.put("single", 3);
            typeCounts.put("multi", 0);  // count=0  -> skipped
            typeCounts.put("fill", -1);  // negative -> skipped
            req.setTypeCounts(typeCounts);
            req.setCount(3);

            List<Question> exam = service.generateExam("CS101", req);
            assertEquals(3, exam.size());
            assertTrue(exam.stream().allMatch(q -> "single".equals(q.getType())));
        }
    }

    // ==================== generateExam — knowledgePointIdCounts ====================

    @Nested
    @DisplayName("generateExam knowledgePointIdCounts 按知识点题量组卷")
    class GenerateExamKnowledgePointCountsTests {

        @Test
        @DisplayName("按知识点题量组卷成功")
        void generatesByKnowledgePointCounts() {
            for (int i = 0; i < 10; i++) {
                put(question("A" + i, "CS101", "L1", "single", "题目" + i, "A", 3, "KP1", 5));
            }
            for (int i = 0; i < 10; i++) {
                put(question("B" + i, "CS101", "L1", "single", "题目" + i, "B", 3, "KP2", 5));
            }

            ExamGenerateRequest req = new ExamGenerateRequest();
            req.setKnowledgePointIdCounts(Map.of("KP1", 2, "KP2", 3));
            req.setCount(5);

            List<Question> exam = service.generateExam("CS101", req);
            assertEquals(5, exam.size());
            long kp1Count = exam.stream().filter(q -> "KP1".equals(q.getKnowledgePointId())).count();
            long kp2Count = exam.stream().filter(q -> "KP2".equals(q.getKnowledgePointId())).count();
            assertEquals(2, kp1Count);
            assertEquals(3, kp2Count);
        }

        @Test
        @DisplayName("knowledgePointIdCounts 题量不足时抛出异常")
        void knowledgePointCountsInsufficientThrows() {
            put(question("1", "CS101", "L1", "single", "题目1", "A", 3, "KP1", 5));

            ExamGenerateRequest req = new ExamGenerateRequest();
            req.setKnowledgePointIdCounts(Map.of("KP1", 2, "KP2", 1));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.generateExam("CS101", req));
            assertTrue(ex.getMessage().contains("符合知识点题量要求的题目不足"));
        }

        @Test
        @DisplayName("knowledgePointIdCounts 包含无效条目时被跳过")
        void knowledgePointCountsSkipsInvalid() {
            for (int i = 0; i < 10; i++) {
                put(question("A" + i, "CS101", "L1", "single", "题目" + i, "A", 3, "KP1", 5));
            }

            ExamGenerateRequest req = new ExamGenerateRequest();
            Map<String, Integer> kpCounts = new LinkedHashMap<>();
            kpCounts.put("KP1", 3);
            kpCounts.put("KP2", 0);
            req.setKnowledgePointIdCounts(kpCounts);
            req.setCount(3);

            List<Question> exam = service.generateExam("CS101", req);
            assertEquals(3, exam.size());
            assertTrue(exam.stream().allMatch(q -> "KP1".equals(q.getKnowledgePointId())));
        }
    }

    // ==================== generateExam — difficultyRatios ====================

    @Nested
    @DisplayName("generateExam difficultyRatios 按难度比例组卷")
    class GenerateExamDifficultyRatiosTests {

        @Test
        @DisplayName("按难度比例组卷成功")
        void generatesByDifficultyRatios() {
            // 难度1: 20道, 难度3: 20道, 难度5: 20道
            for (int i = 0; i < 20; i++) {
                put(question("D1_" + i, "CS101", "L1", "single", "简单" + i, "A", 1, "KP1", 5));
            }
            for (int i = 0; i < 20; i++) {
                put(question("D3_" + i, "CS101", "L1", "single", "中等" + i, "A", 3, "KP1", 5));
            }
            for (int i = 0; i < 20; i++) {
                put(question("D5_" + i, "CS101", "L1", "single", "困难" + i, "A", 5, "KP1", 5));
            }

            ExamGenerateRequest req = new ExamGenerateRequest();
            req.setCount(10);
            req.setDifficultyRatios(Map.of(1, 30, 3, 40, 5, 30));

            List<Question> exam = service.generateExam("CS101", req);
            assertEquals(10, exam.size());
            // 30%简单=3  40%中等=4  30%困难=3
            long easy = exam.stream().filter(q -> q.getDifficulty() == 1).count();
            long mid = exam.stream().filter(q -> q.getDifficulty() == 3).count();
            long hard = exam.stream().filter(q -> q.getDifficulty() == 5).count();
            assertEquals(3, easy);
            assertEquals(4, mid);
            assertEquals(3, hard);
        }

        @Test
        @DisplayName("某难度题目不足时抛出异常")
        void difficultyRatioInsufficientThrows() {
            put(question("1", "CS101", "L1", "single", "简单", "A", 1, "KP1", 5));
            put(question("2", "CS101", "L1", "single", "中等", "B", 3, "KP1", 5));

            ExamGenerateRequest req = new ExamGenerateRequest();
            req.setCount(5);
            req.setDifficultyRatios(Map.of(1, 50, 5, 50));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.generateExam("CS101", req));
            assertTrue(ex.getMessage().contains("符合难度比例要求的题目不足"));
        }

        @Test
        @DisplayName("difficultyRatios 的 ratio 全为 0 时抛出异常")
        void ratioSumZeroThrows() {
            seedQuestions("CS101", 20);

            ExamGenerateRequest req = new ExamGenerateRequest();
            req.setCount(10);
            req.setDifficultyRatios(Map.of(1, 0, 3, 0));

            assertThrows(IllegalArgumentException.class,
                    () -> service.generateExam("CS101", req));
        }
    }

    // ==================== generateExam — targetScore ====================

    @Nested
    @DisplayName("generateExam targetScore 目标总分校验")
    class GenerateExamTargetScoreTests {

        @Test
        @DisplayName("总分精确匹配时不抛异常")
        void targetScoreExactMatch() {
            for (int i = 0; i < 10; i++) {
                put(question("Q" + i, "CS101", "L1", "single", "题目" + i, "A", 3, "KP1", 5));
            }

            ExamGenerateRequest req = new ExamGenerateRequest();
            req.setCount(3);
            req.setTargetScore(15); // 3 * 5 = 15

            List<Question> exam = service.generateExam("CS101", req);
            int total = exam.stream().mapToInt(q -> q.getScore() == null ? 0 : q.getScore()).sum();
            assertEquals(15, total);
        }

        @Test
        @DisplayName("总分不匹配时抛出异常")
        void targetScoreMismatchThrows() {
            for (int i = 0; i < 10; i++) {
                put(question("Q" + i, "CS101", "L1", "single", "题目" + i, "A", 3, "KP1", 5));
            }

            ExamGenerateRequest req = new ExamGenerateRequest();
            req.setCount(3);
            req.setTargetScore(20); // 实际 3*5=15

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.generateExam("CS101", req));
            assertTrue(ex.getMessage().contains("不等于目标总分"));
        }

        @Test
        @DisplayName("targetScore 为 null 时跳过校验")
        void nullTargetScoreSkipsValidation() {
            for (int i = 0; i < 10; i++) {
                put(question("Q" + i, "CS101", "L1", "single", "题目" + i, "A", 3, "KP1", 5));
            }

            ExamGenerateRequest req = new ExamGenerateRequest();
            req.setCount(3);
            req.setTargetScore(null);

            List<Question> exam = service.generateExam("CS101", req);
            assertEquals(3, exam.size());
        }

        @Test
        @DisplayName("targetScore <= 0 时抛出异常")
        void negativeTargetScoreThrows() {
            for (int i = 0; i < 10; i++) {
                put(question("Q" + i, "CS101", "L1", "single", "题目" + i, "A", 3, "KP1", 5));
            }

            ExamGenerateRequest req = new ExamGenerateRequest();
            req.setCount(3);
            req.setTargetScore(0);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.generateExam("CS101", req));
            assertEquals("目标总分必须大于 0", ex.getMessage());
        }
    }

    // ==================== selectByStrategy ====================

    @Nested
    @DisplayName("selectByStrategy 策略选择")
    class SelectByStrategyTests {

        @Test
        @DisplayName("strategy=random 随机选题")
        void strategyRandom() {
            seedQuestions("CS101", 20);

            ExamGenerateRequest req = new ExamGenerateRequest();
            req.setCount(5);
            req.setStrategy("random");

            List<Question> exam = service.generateExam("CS101", req);
            assertEquals(5, exam.size());
        }

        @Test
        @DisplayName("strategy=knowledge 按知识点轮询选题")
        void strategyKnowledge() {
            put(question("KP1_1", "CS101", "L1", "single", "题1", "A", 3, "KP1", 5));
            put(question("KP1_2", "CS101", "L1", "single", "题2", "A", 3, "KP1", 5));
            put(question("KP2_1", "CS101", "L1", "single", "题3", "B", 3, "KP2", 5));
            put(question("KP2_2", "CS101", "L1", "single", "题4", "B", 3, "KP2", 5));
            put(question("KP3_1", "CS101", "L1", "single", "题5", "C", 3, "KP3", 5));
            put(question("KP3_2", "CS101", "L1", "single", "题6", "C", 3, "KP3", 5));

            ExamGenerateRequest req = new ExamGenerateRequest();
            req.setCount(3);
            req.setStrategy("knowledge");
            req.setKnowledgePointIds(List.of("KP1", "KP2", "KP3"));

            List<Question> exam = service.generateExam("CS101", req);
            assertEquals(3, exam.size());
            Set<String> kps = exam.stream().map(Question::getKnowledgePointId).collect(Collectors.toSet());
            assertTrue(kps.size() >= 2, "knowledge strategy should pick from different knowledge points");
        }

        @Test
        @DisplayName("strategy=difficulty 按难度均衡选题")
        void strategyDifficulty() {
            put(question("D1_1", "CS101", "L1", "single", "简单1", "A", 1, "KP1", 5));
            put(question("D1_2", "CS101", "L1", "single", "简单2", "A", 1, "KP1", 5));
            put(question("D1_3", "CS101", "L1", "single", "简单3", "A", 1, "KP1", 5));
            put(question("D3_1", "CS101", "L1", "single", "中等1", "B", 3, "KP1", 5));
            put(question("D3_2", "CS101", "L1", "single", "中等2", "B", 3, "KP1", 5));
            put(question("D3_3", "CS101", "L1", "single", "中等3", "B", 3, "KP1", 5));
            put(question("D5_1", "CS101", "L1", "single", "困难1", "C", 5, "KP1", 5));
            put(question("D5_2", "CS101", "L1", "single", "困难2", "C", 5, "KP1", 5));
            put(question("D5_3", "CS101", "L1", "single", "困难3", "C", 5, "KP1", 5));

            ExamGenerateRequest req = new ExamGenerateRequest();
            req.setCount(3);
            req.setStrategy("difficulty");

            List<Question> exam = service.generateExam("CS101", req);
            assertEquals(3, exam.size());
            Set<Integer> diffs = exam.stream().map(Question::getDifficulty).collect(Collectors.toSet());
            assertTrue(diffs.size() >= 2, "difficulty strategy should pick from different difficulty levels");
        }

        @Test
        @DisplayName("strategy 为 null 默认使用 random 策略")
        void nullStrategyDefaultsToRandom() {
            seedQuestions("CS101", 20);

            ExamGenerateRequest req = new ExamGenerateRequest();
            req.setCount(5);
            req.setStrategy(null);

            List<Question> exam = service.generateExam("CS101", req);
            assertEquals(5, exam.size());
        }

        @Test
        @DisplayName("strategy 为空白字符串默认使用 random 策略")
        void blankStrategyDefaultsToRandom() {
            seedQuestions("CS101", 20);

            ExamGenerateRequest req = new ExamGenerateRequest();
            req.setCount(5);
            req.setStrategy("   ");

            List<Question> exam = service.generateExam("CS101", req);
            assertEquals(5, exam.size());
        }

        @Test
        @DisplayName("候选集为空时 strategy 直接返回空列表（由上层抛异常）")
        void emptyCandidatesWithStrategy() {
            ExamGenerateRequest req = new ExamGenerateRequest();
            req.setCount(5);
            req.setStrategy("knowledge");
            req.setKnowledgePointIds(List.of("KP1"));

            assertThrows(IllegalArgumentException.class,
                    () -> service.generateExam("CS999", req));
        }
    }

    // ==================== normalize（间接测试） ====================

    @Nested
    @DisplayName("normalize 规范化（间接测试）")
    class NormalizeTests {

        @Test
        @DisplayName("knowledgePointId 含首尾空格时仍能匹配")
        void trimsWhitespaceInKnowledgePointId() {
            put(question("1", "CS101", "L1", "single", "题目", "A", 3, "  KP1  ", 5));
            put(question("2", "CS101", "L1", "single", "题目", "A", 3, "  KP1  ", 5));
            put(question("3", "CS101", "L1", "single", "题目", "A", 3, "KP2", 5));

            ExamGenerateRequest req = new ExamGenerateRequest();
            req.setKnowledgePointIdCounts(Map.of("KP1", 2));
            req.setCount(2);

            List<Question> exam = service.generateExam("CS101", req);
            assertEquals(2, exam.size());
            assertTrue(exam.stream().allMatch(q -> q.getKnowledgePointId().trim().equals("KP1")));
        }

        @Test
        @DisplayName("types 过滤时忽略首尾空格")
        void trimsWhitespaceInTypes() {
            put(question("1", "CS101", "L1", "single", "单选题", "A", 3, "KP1", 5));
            put(question("2", "CS101", "L1", "multi", "多选题", "AB", 3, "KP1", 5));
            put(question("3", "CS101", "L1", "fill", "填空题", "答案", 3, "KP1", 5));

            ExamGenerateRequest req = new ExamGenerateRequest();
            req.setCount(2);
            req.setTypes(List.of("  single  ", "  multi  "));

            List<Question> exam = service.generateExam("CS101", req);
            assertEquals(2, exam.size());
            assertTrue(exam.stream().allMatch(q ->
                    "single".equals(q.getType()) || "multi".equals(q.getType())));
        }

        @Test
        @DisplayName("null 值被 normalize 为空字符串后不影响匹配")
        void nullKnowledgePointIdDoesNotMatchNonEmptyFilter() {
            put(question("1", "CS101", "L1", "single", "题目", "A", 3, null, 5));
            put(question("2", "CS101", "L1", "single", "题目", "B", 3, "KP1", 5));

            ExamGenerateRequest req = new ExamGenerateRequest();
            req.setKnowledgePointIdCounts(Map.of("KP1", 1));
            req.setCount(1);

            List<Question> exam = service.generateExam("CS101", req);
            assertEquals(1, exam.size());
            assertEquals("KP1", exam.get(0).getKnowledgePointId());
        }

        @Test
        @DisplayName("仅包含空格的 knowledgePointId normalize 后为空字符串")
        void whitespaceOnlyNormalizesToEmpty() {
            put(question("1", "CS101", "L1", "single", "题目", "A", 3, "   ", 5));
            put(question("2", "CS101", "L1", "single", "题目", "B", 3, "KP1", 5));

            ExamGenerateRequest req = new ExamGenerateRequest();
            req.setKnowledgePointIdCounts(Map.of("KP1", 1));
            req.setCount(1);

            List<Question> exam = service.generateExam("CS101", req);
            assertEquals(1, exam.size());
            assertEquals("KP1", exam.get(0).getKnowledgePointId());
        }
    }

    // ==================== 多条件组合 ====================

    @Nested
    @DisplayName("多条件组合组卷")
    class CombinedConditionsTests {

        @Test
        @DisplayName("typeCounts + types 过滤组合")
        void typeCountsWithTypesFilter() {
            for (int i = 0; i < 10; i++) {
                put(question("S" + i, "CS101", "L1", "single", "单选" + i, "A", 3, "KP1", 5));
                put(question("M" + i, "CS101", "L1", "multi", "多选" + i, "AB", 3, "KP1", 5));
            }

            ExamGenerateRequest req = new ExamGenerateRequest();
            req.setTypeCounts(Map.of("single", 3, "multi", 2));
            req.setTypes(List.of("single")); // 仅允许 single → multi 候选为空

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.generateExam("CS101", req));
            assertTrue(ex.getMessage().contains("符合题型数量要求的题目不足"));
        }

        @Test
        @DisplayName("knowledgePointIdCounts + difficulty 范围过滤组合")
        void knowledgePointCountsWithDifficultyFilter() {
            put(question("1", "CS101", "L1", "single", "题目", "A", 1, "KP1", 5));
            put(question("2", "CS101", "L1", "single", "题目", "B", 1, "KP1", 5));
            put(question("3", "CS101", "L1", "single", "题目", "C", 5, "KP1", 5)); // 被 difficulty 过滤
            put(question("4", "CS101", "L1", "single", "题目", "D", 1, "KP2", 5));
            put(question("5", "CS101", "L1", "single", "题目", "E", 1, "KP2", 5));

            ExamGenerateRequest req = new ExamGenerateRequest();
            req.setKnowledgePointIdCounts(Map.of("KP1", 2));
            req.setDifficultyMax(2);
            req.setCount(2);

            List<Question> exam = service.generateExam("CS101", req);
            assertEquals(2, exam.size());
            assertTrue(exam.stream().allMatch(q ->
                    "KP1".equals(q.getKnowledgePointId()) && q.getDifficulty() <= 2));
        }

        @Test
        @DisplayName("difficultyRatios + knowledgePointIds 过滤组合")
        void difficultyRatiosWithKnowledgePointFilter() {
            for (int i = 0; i < 10; i++) {
                put(question("D1_" + i, "CS101", "L1", "single", "简单" + i, "A", 1, "KP1", 5));
            }
            for (int i = 0; i < 10; i++) {
                put(question("D5_" + i, "CS101", "L1", "single", "困难" + i, "A", 5, "KP1", 5));
            }
            for (int i = 0; i < 10; i++) {
                put(question("D1_KP2_" + i, "CS101", "L1", "single", "简单2" + i, "A", 1, "KP2", 5));
            }

            ExamGenerateRequest req = new ExamGenerateRequest();
            req.setCount(6);
            req.setDifficultyRatios(Map.of(1, 50, 5, 50));
            req.setKnowledgePointIds(List.of("KP1"));

            List<Question> exam = service.generateExam("CS101", req);
            assertEquals(6, exam.size());
            assertTrue(exam.stream().allMatch(q -> "KP1".equals(q.getKnowledgePointId())));
            long easy = exam.stream().filter(q -> q.getDifficulty() == 1).count();
            long hard = exam.stream().filter(q -> q.getDifficulty() == 5).count();
            assertEquals(3, easy);
            assertEquals(3, hard);
        }
    }

    // ==================== 边界与健壮性 ====================

    @Nested
    @DisplayName("边界与健壮性")
    class EdgeCasesTests {

        @Test
        @DisplayName("difficulty 为 null 的题目在 difficultyRatios 中被视作难度3")
        void nullDifficultyTreatedAsDefault() {
            for (int i = 0; i < 10; i++) {
                put(question("Q" + i, "CS101", "L1", "single", "题目" + i, "A", null, "KP1", 5));
            }

            ExamGenerateRequest req = new ExamGenerateRequest();
            req.setCount(5);
            req.setDifficultyRatios(Map.of(3, 100));

            List<Question> exam = service.generateExam("CS101", req);
            assertEquals(5, exam.size());
        }

        @Test
        @DisplayName("score 为 null 时 targetScore 校验默认为 0")
        void nullScoreTreatedAsZero() {
            put(question("1", "CS101", "L1", "single", "题目", "A", 3, "KP1", 5));
            put(question("2", "CS101", "L1", "single", "题目", "B", 3, "KP1", 5));
            put(question("3", "CS101", "L1", "single", "题目", "C", 3, "KP1", 5));
            put(question("4", "CS101", "L1", "single", "题目", "D", 3, "KP1", 5));
            put(question("5", "CS101", "L1", "single", "题目", "E", 3, "KP1", 5));

            ExamGenerateRequest req = new ExamGenerateRequest();
            req.setCount(3);
            req.setTargetScore(15);

            List<Question> exam = service.generateExam("CS101", req);
            int actual = exam.stream().mapToInt(q -> q.getScore() == null ? 0 : q.getScore()).sum();
            assertEquals(15, actual);
        }

        @Test
        @DisplayName("count 很大但题库充足时正常选题且无重复")
        void largeCountWithSufficientQuestions() {
            seedQuestions("CS101", 100);

            ExamGenerateRequest req = new ExamGenerateRequest();
            req.setCount(50);

            List<Question> exam = service.generateExam("CS101", req);
            assertEquals(50, exam.size());
            Set<String> ids = exam.stream().map(Question::getQuestionId).collect(Collectors.toSet());
            assertEquals(50, ids.size(), "selected questions should have no duplicates");
        }

        @Test
        @DisplayName("typeCounts 中某题型 count=0 时被忽略")
        void typeCountsWithZeroCountSkipped() {
            for (int i = 0; i < 10; i++) {
                put(question("S" + i, "CS101", "L1", "single", "单选" + i, "A", 3, "KP1", 5));
            }

            ExamGenerateRequest req = new ExamGenerateRequest();
            req.setTypeCounts(Map.of("single", 3, "multi", 0));
            req.setCount(3);

            List<Question> exam = service.generateExam("CS101", req);
            assertEquals(3, exam.size());
            assertTrue(exam.stream().allMatch(q -> "single".equals(q.getType())));
        }

        @Test
        @DisplayName("knowledgePointIdCounts 中某知识点 count=0 时被忽略")
        void knowledgePointCountsWithZeroCountSkipped() {
            for (int i = 0; i < 10; i++) {
                put(question("A" + i, "CS101", "L1", "single", "题目" + i, "A", 3, "KP1", 5));
            }

            ExamGenerateRequest req = new ExamGenerateRequest();
            req.setKnowledgePointIdCounts(Map.of("KP1", 3, "KP2", 0));
            req.setCount(3);

            List<Question> exam = service.generateExam("CS101", req);
            assertEquals(3, exam.size());
            assertTrue(exam.stream().allMatch(q -> "KP1".equals(q.getKnowledgePointId())));
        }

        @Test
        @DisplayName("difficultyMin / difficultyMax 为 null 时使用默认范围 1-5")
        void nullDifficultyRangeUsesDefaults() {
            for (int i = 0; i < 10; i++) {
                put(question("Q" + i, "CS101", "L1", "single", "题目" + i, "A", 1, "KP1", 5));
            }

            ExamGenerateRequest req = new ExamGenerateRequest();
            req.setCount(5);
            req.setDifficultyMin(null);
            req.setDifficultyMax(null);

            List<Question> exam = service.generateExam("CS101", req);
            assertEquals(5, exam.size());
        }
    }
}
