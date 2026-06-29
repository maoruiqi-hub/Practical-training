package com.neu.CoursePlatform.service;

import com.neu.CoursePlatform.dto.ExamGenerateRequest;
import com.neu.CoursePlatform.entity.Question;
import com.neu.CoursePlatform.mapper.QuestionMapper;
import com.neu.CoursePlatform.service.impl.QuestionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.*;

import static com.neu.CoursePlatform.service.TeacherServiceTest.setBaseMapper;
import static org.junit.jupiter.api.Assertions.*;

class QuestionServiceTest {

    private QuestionServiceImpl service;
    private Map<String, Question> store;

    @BeforeEach
    void setUp() throws Exception {
        store = new LinkedHashMap<>();
        // 10 道预置题目
        store.put("1", q("1", "1", "single", "1+1=?", "A", 1, "kp-1", 5));
        store.put("2", q("2", "1", "single", "2+2=?", "B", 2, "kp-1", 5));
        store.put("3", q("3", "1", "multi", "选ABCD", "A,B,C,D", 3, "kp-2", 10));
        store.put("4", q("4", "1", "multi", "选AB", "A,B", 2, "kp-2", 10));
        store.put("5", q("5", "1", "fill", "填空___", "答案", 1, "kp-1", 5));
        store.put("6", q("6", "1", "fill", "另一填空", "x", 1, "kp-3", 5));
        store.put("7", q("7", "1", "essay", "论述题", "要点", 5, "kp-3", 20));
        store.put("8", q("8", "1", "program", "写代码", "code", 4, "kp-4", 15));
        store.put("9", q("9", "1", "single", "判断对错", "A", 1, "kp-1", 5));
        store.put("10", q("10", "1", "single", "第四选C", "C", 3, "kp-1", 5));

        QuestionMapper proxy = (QuestionMapper) Proxy.newProxyInstance(
                QuestionMapper.class.getClassLoader(),
                new Class<?>[]{QuestionMapper.class},
                (p, method, args) -> qInvoke(store, method.getName(), args)
        );

        service = new QuestionServiceImpl();
        setBaseMapper(service, proxy);
    }

    // ============ 随机组卷 ============

    @Test
    void randomReturnsCorrectCount() {
        ExamGenerateRequest req = new ExamGenerateRequest();
        req.setCount(5);
        req.setStrategy("random");
        assertEquals(5, service.generateExam("1", req).size());
    }

    @Test
    void randomReturnsAllUnique() {
        ExamGenerateRequest req = new ExamGenerateRequest();
        req.setCount(5);
        List<Question> result = service.generateExam("1", req);
        Set<String> ids = new HashSet<>();
        result.forEach(q -> ids.add(q.getQuestionId()));
        assertEquals(5, ids.size());
    }

    // ============ 按题型过滤 ============

    @Test
    void filterByType() {
        ExamGenerateRequest req = new ExamGenerateRequest();
        req.setCount(3);
        req.setTypes(List.of("single"));
        List<Question> result = service.generateExam("1", req);
        assertEquals(3, result.size());
        assertTrue(result.stream().allMatch(q -> "single".equals(q.getType())));
    }

    // ============ 按难度过滤 ============

    @Test
    void filterByDifficulty() {
        ExamGenerateRequest req = new ExamGenerateRequest();
        req.setCount(2);
        req.setDifficultyMin(4);
        req.setDifficultyMax(5);
        List<Question> result = service.generateExam("1", req);
        assertFalse(result.isEmpty());
        assertTrue(result.stream().allMatch(q -> q.getDifficulty() >= 4));
    }

    // ============ 按知识点组卷 ============

    @Test
    void knowledgeStrategy() {
        ExamGenerateRequest req = new ExamGenerateRequest();
        req.setCount(3);
        req.setStrategy("knowledge");
        req.setKnowledgePointIds(List.of("kp-1", "kp-2"));
        assertEquals(3, service.generateExam("1", req).size());
    }

    // ============ 难度平衡组卷 ============

    @Test
    void difficultyStrategy() {
        ExamGenerateRequest req = new ExamGenerateRequest();
        req.setCount(3);
        req.setStrategy("difficulty");
        assertEquals(3, service.generateExam("1", req).size());
    }

    // ============ 题型数量组卷 ============

    @Test
    void typeCounts() {
        ExamGenerateRequest req = new ExamGenerateRequest();
        req.setCount(3); // 必须匹配 typeCounts 总和
        Map<String, Integer> tc = new LinkedHashMap<>();
        tc.put("single", 2);
        tc.put("fill", 1);
        req.setTypeCounts(tc);
        List<Question> result = service.generateExam("1", req);
        assertEquals(3, result.size());
        assertEquals(2, result.stream().filter(q -> "single".equals(q.getType())).count());
    }

    // ============ 知识点题量组卷 ============

    @Test
    void knowledgePointCounts() {
        ExamGenerateRequest req = new ExamGenerateRequest();
        req.setCount(3);
        Map<String, Integer> kc = new LinkedHashMap<>();
        kc.put("kp-1", 2);
        kc.put("kp-2", 1);
        req.setKnowledgePointIdCounts(kc);
        assertEquals(3, service.generateExam("1", req).size());
    }

    // ============ 难度比例组卷 ============

    @Test
    void difficultyRatios() {
        ExamGenerateRequest req = new ExamGenerateRequest();
        req.setCount(6);
        Map<Integer, Integer> ratios = new LinkedHashMap<>();
        ratios.put(1, 50);
        ratios.put(3, 30);
        ratios.put(5, 20);
        req.setDifficultyRatios(ratios);
        assertEquals(6, service.generateExam("1", req).size());
    }

    // ============ 异常情况 ============

    @Test
    void throwsOnNullRequest() {
        assertThrows(IllegalArgumentException.class, () -> service.generateExam("1", null));
    }

    @Test
    void throwsOnZeroCount() {
        ExamGenerateRequest req = new ExamGenerateRequest();
        req.setCount(0);
        assertThrows(IllegalArgumentException.class, () -> service.generateExam("1", req));
    }

    @Test
    void throwsWhenNotEnough() {
        ExamGenerateRequest req = new ExamGenerateRequest();
        req.setCount(100);
        req.setDifficultyMin(5);
        req.setDifficultyMax(5);
        assertThrows(IllegalArgumentException.class, () -> service.generateExam("1", req));
    }

    @Test
    void throwsWhenNoCandidates() {
        ExamGenerateRequest req = new ExamGenerateRequest();
        req.setCount(5);
        req.setDifficultyMin(6);
        req.setDifficultyMax(7);
        assertThrows(IllegalArgumentException.class, () -> service.generateExam("1", req));
    }

    @Test
    void throwsOnTargetScoreMismatch() {
        ExamGenerateRequest req = new ExamGenerateRequest();
        req.setCount(2);
        req.setTypes(List.of("single"));
        req.setTargetScore(999);
        assertThrows(IllegalArgumentException.class, () -> service.generateExam("1", req));
    }

    // ============ 查询 ============

    @Test
    void listByCourseCode() {
        assertEquals(10, service.listByCourseCode("1").size());
    }

    @Test
    void searchByKeyword() {
        assertEquals(1, service.searchByKeyword("1+1=?").size());
    }

    // ============ helpers ============

    private Question q(String id, String cc, String type, String stem,
                       String answer, int diff, String kp, int score) {
        Question q = new Question();
        q.setQuestionId(id);
        q.setCourseCode(cc);
        q.setType(type);
        q.setStem(stem);
        q.setAnswer(answer);
        q.setDifficulty(diff);
        q.setKnowledgePointId(kp);
        q.setScore(score);
        return q;
    }

    static Object qInvoke(Map<String, Question> store, String methodName, Object[] args) {
        switch (methodName) {
            case "selectByCourseCode": {
                String cc = (String) args[0];
                return store.values().stream()
                        .filter(q -> cc != null && cc.equals(q.getCourseCode())).toList();
            }
            case "selectByLessonNo": {
                String ln = (String) args[0];
                return store.values().stream()
                        .filter(q -> ln != null && ln.equals(q.getLessonNo())).toList();
            }
            case "selectByKeyword": {
                String kw = (String) args[0];
                if (kw == null || kw.isEmpty()) return new ArrayList<>(store.values());
                return store.values().stream()
                        .filter(q -> q.getStem() != null && q.getStem().contains(kw)).toList();
            }
            case "selectByFilter": {
                String cc = (String) args[0];
                String ln = (String) args[1];
                String kp = (String) args[2];
                String type = (String) args[3];
                Integer diff = (Integer) args[4];
                String kw = (String) args[5];
                return store.values().stream()
                        .filter(q -> isBlank(cc) || cc.equals(q.getCourseCode()))
                        .filter(q -> isBlank(ln) || ln.equals(q.getLessonNo()))
                        .filter(q -> isBlank(kp) || kp.equals(q.getKnowledgePointId()))
                        .filter(q -> isBlank(type) || type.equals(q.getType()))
                        .filter(q -> diff == null || (q.getDifficulty() != null && q.getDifficulty().equals(diff)))
                        .filter(q -> isBlank(kw) || (q.getStem() != null && q.getStem().contains(kw)))
                        .toList();
            }
            case "insert": {
                if (args != null && args.length == 1 && args[0] instanceof Question q) {
                    store.put(q.getQuestionId(), q);
                    return 1;
                }
                return 0;
            }
            case "selectById": return store.get(String.valueOf(args[0]));
            case "selectList": return new ArrayList<>(store.values());
            case "updateById": { Question q = (Question) args[0]; store.put(q.getQuestionId(), q); return 1; }
            case "deleteById": return store.remove(String.valueOf(args[0])) != null ? 1 : 0;
            case "selectCount": return (long) store.size();
            default: return null;
        }
    }

    private static boolean isBlank(String s) { return s == null || s.isEmpty(); }
}
