package com.neu.CoursePlatform.service;

import com.neu.CoursePlatform.dto.ExamGenerateRequest;
import com.neu.CoursePlatform.dto.ExamGenerateResult;
import com.neu.CoursePlatform.entity.Exam;
import com.neu.CoursePlatform.entity.Question;
import com.neu.CoursePlatform.mapper.ExamMapper;
import com.neu.CoursePlatform.service.impl.ExamServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.*;

import static com.neu.CoursePlatform.service.TeacherServiceTest.setBaseMapper;
import static org.junit.jupiter.api.Assertions.*;

class ExamServiceTest {

    private ExamServiceImpl service;
    private Map<String, Exam> examStore;

    @BeforeEach
    void setUp() throws Exception {
        examStore = new LinkedHashMap<>();

        ExamMapper proxy = (ExamMapper) Proxy.newProxyInstance(
                ExamMapper.class.getClassLoader(), new Class<?>[]{ExamMapper.class},
                (p, method, args) -> examMapperInvoke(examStore, method.getName(), args));

        // QuestionService 返回预置题目
        QuestionService qs = proxyQs();

        // ExamQuestionService 接受批量保存
        ExamQuestionService eqs = (ExamQuestionService) Proxy.newProxyInstance(
                ExamQuestionService.class.getClassLoader(), new Class<?>[]{ExamQuestionService.class},
                (p, method, args) -> {
                    if ("saveBatch".equals(method.getName())) return true;
                    return null;
                });

        service = new ExamServiceImpl(qs, eqs);
        setBaseMapper(service, proxy);
    }

    @Test
    void generateAndSaveCreatesExam() {
        ExamGenerateRequest req = new ExamGenerateRequest();
        req.setCount(2);
        req.setStrategy("random");
        req.setTypes(List.of("single"));

        ExamGenerateResult result = service.generateAndSave("1", req);
        assertNotNull(result);
        assertNotNull(result.getExam());
        assertEquals("1", result.getExam().getCourseCode());
        assertEquals(2, result.getExam().getTargetCount());
        assertEquals("random", result.getExam().getGenerateType());
        assertEquals("draft", result.getExam().getStatus());
        assertNotNull(result.getExam().getExamId());
    }

    @Test
    void generateWithKnowledgeStrategy() {
        ExamGenerateRequest req = new ExamGenerateRequest();
        req.setCount(2);
        req.setStrategy("knowledge");
        req.setKnowledgePointIds(List.of("kp-1"));

        ExamGenerateResult result = service.generateAndSave("1", req);
        assertEquals("knowledge", result.getExam().getGenerateType());
    }

    @Test
    void generateWithDifficultyStrategy() {
        ExamGenerateRequest req = new ExamGenerateRequest();
        req.setCount(2);
        req.setStrategy("difficulty");

        ExamGenerateResult result = service.generateAndSave("1", req);
        assertEquals("difficulty", result.getExam().getGenerateType());
    }

    @Test
    void titleReflectsStrategy() {
        ExamGenerateRequest req = new ExamGenerateRequest();
        req.setCount(1);
        req.setStrategy("knowledge");
        ExamGenerateResult r1 = service.generateAndSave("1", req);
        assertTrue(r1.getExam().getTitle().contains("知识点"));

        req.setStrategy("difficulty");
        ExamGenerateResult r2 = service.generateAndSave("1", req);
        assertTrue(r2.getExam().getTitle().contains("难度平衡"));

        req.setStrategy("random");
        ExamGenerateResult r3 = service.generateAndSave("1", req);
        assertTrue(r3.getExam().getTitle().contains("随机"));
    }

    @Test
    void bindToTaskPublishesExam() {
        Exam exam = new Exam();
        exam.setExamId("exam-1");
        exam.setStatus("draft");
        examStore.put("exam-1", exam);

        service.bindToTask("exam-1", "task-1");

        assertEquals("task-1", exam.getTaskNo());
        assertEquals("published", exam.getStatus());
    }

    @Test
    void bindToTaskThrowsWhenExamNotFound() {
        assertThrows(IllegalArgumentException.class,
                () -> service.bindToTask("nonexistent", "task-1"));
    }

    // ============ helpers ============

    private QuestionService proxyQs() {
        return (QuestionService) Proxy.newProxyInstance(
                QuestionService.class.getClassLoader(), new Class<?>[]{QuestionService.class},
                (p, method, args) -> {
                    if ("generateExam".equals(method.getName())) {
                        Question q1 = new Question();
                        q1.setQuestionId("q1");
                        q1.setType("single");
                        q1.setScore(5);
                        q1.setKnowledgePointId("kp-1");
                        q1.setDifficulty(1);
                        Question q2 = new Question();
                        q2.setQuestionId("q2");
                        q2.setType("single");
                        q2.setScore(5);
                        q2.setKnowledgePointId("kp-1");
                        q2.setDifficulty(2);
                        return List.of(q1, q2);
                    }
                    return null;
                });
    }

    static Object examMapperInvoke(Map<String, Exam> store, String name, Object[] args) {
        switch (name) {
            case "insert": {
                Exam e = (Exam) args[0];
                if (e.getExamId() == null) e.setExamId("exam-" + (store.size() + 1));
                store.put(e.getExamId(), e);
                return 1;
            }
            case "selectById": return store.get(String.valueOf(args[0]));
            case "updateById": { Exam e = (Exam) args[0]; store.put(e.getExamId(), e); return 1; }
            case "selectList": return new ArrayList<>(store.values());
            case "deleteById": return store.remove(String.valueOf(args[0])) != null ? 1 : 0;
            case "selectCount": return (long) store.size();
            default: return null;
        }
    }
}
