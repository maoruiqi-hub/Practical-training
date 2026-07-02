package com.neu.CoursePlatform.controller;

import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.entity.KnowledgePoint;
import com.neu.CoursePlatform.entity.LearningTask;
import com.neu.CoursePlatform.entity.Question;
import com.neu.CoursePlatform.entity.TaskQuestion;
import com.neu.CoursePlatform.service.KnowledgePointService;
import com.neu.CoursePlatform.service.LearningTaskService;
import com.neu.CoursePlatform.service.QuestionService;
import com.neu.CoursePlatform.service.TaskQuestionService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuestionControllerTest {

    @Mock private QuestionService questionService;
    @Mock private TaskQuestionService taskQuestionService;
    @Mock private LearningTaskService taskService;
    @Mock private KnowledgePointService knowledgePointService;
    @Mock private Auth auth;
    @Mock private HttpSession session;
    @InjectMocks private QuestionController controller;

    // ============ list ============

    @Test
    void listRequiresCoursePermission() {
        when(auth.canModifyCourse(session, "CS101")).thenReturn(false);

        Result<List<Question>> result = controller.list(Map.of("course_id", "CS101"), session);

        assertEquals(500, result.getCode());
    }

    @Test
    void listRequiresLoginWhenNoCourse() {
        when(auth.isLoggedIn(session)).thenReturn(false);

        Result<List<Question>> result = controller.list(Map.of(), session);

        assertEquals(500, result.getCode());
    }

    @Test
    void listInvalidDifficulty() {
        Result<List<Question>> result = controller.list(Map.of("difficulty", "abc"), session);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("difficulty"));
    }

    @Test
    void listSuccess() {
        when(auth.isLoggedIn(session)).thenReturn(true);
        when(questionService.filterQuestions(null, null, null, null, null, null))
                .thenReturn(List.of(new Question()));

        Result<List<Question>> result = controller.list(Map.of(), session);

        assertEquals(200, result.getCode());
    }

    @Test
    void listWithAllFilters() {
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);
        when(questionService.filterQuestions(eq("CS101"), eq("L001"), eq("kp1"), eq("choice"), eq(3), eq("Java")))
                .thenReturn(List.of());

        Result<List<Question>> result = controller.list(Map.of(
                "course_id", "CS101", "lesson_id", "L001", "knowledge_point_id", "kp1",
                "type", "choice", "difficulty", "3", "keyword", "Java"), session);

        assertEquals(200, result.getCode());
    }

    // ============ detail ============

    @Test
    void detailRequiresLogin() {
        when(auth.isLoggedIn(session)).thenReturn(false);

        Result<Question> result = controller.detail("q1", session);

        assertEquals(500, result.getCode());
    }

    @Test
    void detailNotFound() {
        when(auth.isLoggedIn(session)).thenReturn(true);
        when(questionService.getById("q1")).thenReturn(null);

        Result<Question> result = controller.detail("q1", session);

        assertEquals(500, result.getCode());
    }

    @Test
    void detailReturnsQuestion() {
        when(auth.isLoggedIn(session)).thenReturn(true);
        Question q = new Question();
        q.setQuestionId("q1");
        when(questionService.getById("q1")).thenReturn(q);

        Result<Question> result = controller.detail("q1", session);

        assertEquals(200, result.getCode());
    }

    @Test
    void detailWithKnowledgePoint() {
        when(auth.isLoggedIn(session)).thenReturn(true);
        Question q = new Question();
        q.setQuestionId("q1");
        q.setKnowledgePointId("kp1");
        when(questionService.getById("q1")).thenReturn(q);
        KnowledgePoint kp = new KnowledgePoint();
        kp.setKnowledgePointId("kp1");
        when(knowledgePointService.getById("kp1")).thenReturn(kp);

        Result<Question> result = controller.detail("q1", session);

        assertEquals(200, result.getCode());
        assertNotNull(result.getData().getKnowledgePoint());
    }

    // ============ listByCourse ============

    @Test
    void listByCourseRequiresPermission() {
        when(auth.canModifyCourse(session, "CS101")).thenReturn(false);

        Result<List<Question>> result = controller.listByCourse("CS101", session);

        assertEquals(500, result.getCode());
    }

    @Test
    void listByCourseSuccess() {
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);
        when(questionService.listByCourseCode("CS101")).thenReturn(List.of(new Question()));

        Result<List<Question>> result = controller.listByCourse("CS101", session);

        assertEquals(200, result.getCode());
    }

    // ============ listByLesson ============

    @Test
    void listByLessonRequiresLogin() {
        when(auth.isLoggedIn(session)).thenReturn(false);

        Result<List<Question>> result = controller.listByLesson("L001", session);

        assertEquals(500, result.getCode());
    }

    @Test
    void listByLessonSuccess() {
        when(auth.isLoggedIn(session)).thenReturn(true);
        when(questionService.listByLessonNo("L001")).thenReturn(List.of());

        Result<List<Question>> result = controller.listByLesson("L001", session);

        assertEquals(200, result.getCode());
    }

    // ============ search ============

    @Test
    void searchRequiresLogin() {
        when(auth.isLoggedIn(session)).thenReturn(false);

        Result<List<Question>> result = controller.search("keyword", session);

        assertEquals(500, result.getCode());
    }

    @Test
    void searchReturnsResults() {
        when(auth.isLoggedIn(session)).thenReturn(true);
        when(questionService.searchByKeyword("Java")).thenReturn(List.of(new Question()));

        Result<List<Question>> result = controller.search("Java", session);

        assertEquals(200, result.getCode());
    }

    // ============ filter ============

    @Test
    void filterRequiresCoursePermission() {
        when(auth.canModifyCourse(session, "CS101")).thenReturn(false);

        Result<List<Question>> result = controller.filter("CS101", null, null, null, null, null, session);

        assertEquals(500, result.getCode());
    }

    @Test
    void filterRequiresLoginWhenNoCourse() {
        when(auth.isLoggedIn(session)).thenReturn(false);

        Result<List<Question>> result = controller.filter(null, null, null, null, null, null, session);

        assertEquals(500, result.getCode());
    }

    @Test
    void filterSuccess() {
        when(auth.isLoggedIn(session)).thenReturn(true);
        when(questionService.filterQuestions(null, null, null, null, null, null)).thenReturn(List.of());

        Result<List<Question>> result = controller.filter(null, null, null, null, null, null, session);

        assertEquals(200, result.getCode());
    }

    // ============ add ============

    @Test
    void addRequiresPermission() {
        Question q = new Question();
        q.setCourseCode("CS101");
        when(auth.canModifyCourse(session, "CS101")).thenReturn(false);

        Result<Void> result = controller.add(q, session);

        assertEquals(500, result.getCode());
    }

    @Test
    void addSuccess() {
        Question q = new Question();
        q.setCourseCode("CS101");
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);

        Result<Void> result = controller.add(q, session);

        assertEquals(200, result.getCode());
        verify(questionService).save(q);
    }

    @Test
    void addWithKnowledgePoint() {
        Question q = new Question();
        q.setCourseCode("CS101");
        q.setKnowledgePointId("kp1");
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);
        KnowledgePoint kp = new KnowledgePoint();
        kp.setKnowledgePointId("kp1");
        kp.setCourseCode("CS101");
        when(knowledgePointService.getById("kp1")).thenReturn(kp);

        Result<Void> result = controller.add(q, session);

        assertEquals(200, result.getCode());
    }

    @Test
    void addKnowledgePointNotFound() {
        Question q = new Question();
        q.setCourseCode("CS101");
        q.setKnowledgePointId("kp999");
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);
        when(knowledgePointService.getById("kp999")).thenReturn(null);

        Result<Void> result = controller.add(q, session);

        assertEquals(500, result.getCode());
    }

    @Test
    void addKnowledgePointWrongCourse() {
        Question q = new Question();
        q.setCourseCode("CS101");
        q.setKnowledgePointId("kp1");
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);
        KnowledgePoint kp = new KnowledgePoint();
        kp.setKnowledgePointId("kp1");
        kp.setCourseCode("CS102");
        when(knowledgePointService.getById("kp1")).thenReturn(kp);

        Result<Void> result = controller.add(q, session);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("不属于当前课程"));
    }

    // ============ update ============

    @Test
    void updateQuestionNotFound() {
        when(questionService.getById("q1")).thenReturn(null);

        Result<Void> result = controller.update("q1", new Question(), session);

        assertEquals(500, result.getCode());
    }

    @Test
    void updateRequiresPermission() {
        Question existing = new Question();
        existing.setCourseCode("CS101");
        when(questionService.getById("q1")).thenReturn(existing);
        when(auth.canModifyCourse(session, "CS101")).thenReturn(false);

        Result<Void> result = controller.update("q1", new Question(), session);

        assertEquals(500, result.getCode());
    }

    @Test
    void updateSuccess() {
        Question existing = new Question();
        existing.setCourseCode("CS101");
        when(questionService.getById("q1")).thenReturn(existing);
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);

        Question update = new Question();
        update.setStem("Updated text");

        Result<Void> result = controller.update("q1", update, session);

        assertEquals(200, result.getCode());
        assertEquals("q1", update.getQuestionId());
        assertEquals("CS101", update.getCourseCode());
        verify(questionService).updateById(update);
    }

    @Test
    void updateKnowledgePointError() {
        Question existing = new Question();
        existing.setCourseCode("CS101");
        when(questionService.getById("q1")).thenReturn(existing);
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);
        Question update = new Question();
        update.setKnowledgePointId("kp999");
        when(knowledgePointService.getById("kp999")).thenReturn(null);

        Result<Void> result = controller.update("q1", update, session);

        assertEquals(500, result.getCode());
    }

    // ============ linkKnowledgePoint ============

    @Test
    void linkKnowledgePointQuestionNotFound() {
        when(questionService.getById("q1")).thenReturn(null);

        Result<Question> result = controller.linkKnowledgePoint("q1", Map.of("knowledge_point_id", "kp1"), session);

        assertEquals(500, result.getCode());
    }

    @Test
    void linkKnowledgePointRequiresPermission() {
        Question q = new Question();
        q.setCourseCode("CS101");
        when(questionService.getById("q1")).thenReturn(q);
        when(auth.canModifyCourse(session, "CS101")).thenReturn(false);

        Result<Question> result = controller.linkKnowledgePoint("q1", Map.of("knowledge_point_id", "kp1"), session);

        assertEquals(500, result.getCode());
    }

    @Test
    void linkKnowledgePointBodyNull() {
        Question q = new Question();
        q.setCourseCode("CS101");
        when(questionService.getById("q1")).thenReturn(q);
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);

        Result<Question> result = controller.linkKnowledgePoint("q1", null, session);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("knowledgePointId"));
    }

    @Test
    void linkKnowledgePointIdMissing() {
        Question q = new Question();
        q.setCourseCode("CS101");
        when(questionService.getById("q1")).thenReturn(q);
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);

        Result<Question> result = controller.linkKnowledgePoint("q1", Map.of(), session);

        assertEquals(500, result.getCode());
    }

    @Test
    void linkKnowledgePointNotFound() {
        Question q = new Question();
        q.setCourseCode("CS101");
        when(questionService.getById("q1")).thenReturn(q);
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);
        when(knowledgePointService.getById("kp999")).thenReturn(null);

        Result<Question> result = controller.linkKnowledgePoint("q1", Map.of("knowledge_point_id", "kp999"), session);

        assertEquals(500, result.getCode());
    }

    @Test
    void linkKnowledgePointWrongCourse() {
        Question q = new Question();
        q.setCourseCode("CS101");
        when(questionService.getById("q1")).thenReturn(q);
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);
        KnowledgePoint kp = new KnowledgePoint();
        kp.setKnowledgePointId("kp1");
        kp.setCourseCode("CS102");
        when(knowledgePointService.getById("kp1")).thenReturn(kp);

        Result<Question> result = controller.linkKnowledgePoint("q1", Map.of("knowledge_point_id", "kp1"), session);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("不属于当前课程"));
    }

    @Test
    void linkKnowledgePointSuccess() {
        Question q = new Question();
        q.setCourseCode("CS101");
        q.setQuestionId("q1");
        when(questionService.getById("q1")).thenReturn(q);
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);
        KnowledgePoint kp = new KnowledgePoint();
        kp.setKnowledgePointId("kp1");
        kp.setCourseCode("CS101");
        when(knowledgePointService.getById("kp1")).thenReturn(kp);

        Result<Question> result = controller.linkKnowledgePoint("q1", Map.of("knowledge_point_id", "kp1"), session);

        assertEquals(200, result.getCode());
        assertEquals("kp1", q.getKnowledgePointId());
        verify(questionService).updateById(q);
    }

    // ============ delete ============

    @Test
    void deleteQuestionNotFound() {
        when(questionService.getById("q1")).thenReturn(null);

        Result<Void> result = controller.delete("q1", session);

        assertEquals(500, result.getCode());
    }

    @Test
    void deleteRequiresPermission() {
        Question q = new Question();
        q.setCourseCode("CS101");
        when(questionService.getById("q1")).thenReturn(q);
        when(auth.canModifyCourse(session, "CS101")).thenReturn(false);

        Result<Void> result = controller.delete("q1", session);

        assertEquals(500, result.getCode());
    }

    @Test
    void deleteSuccess() {
        Question q = new Question();
        q.setCourseCode("CS101");
        when(questionService.getById("q1")).thenReturn(q);
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);

        Result<Void> result = controller.delete("q1", session);

        assertEquals(200, result.getCode());
        verify(questionService).removeById("q1");
    }

    // ============ listTaskQuestions ============

    @Test
    void listTaskQuestionsRequiresLogin() {
        when(auth.isLoggedIn(session)).thenReturn(false);

        Result<List<TaskQuestion>> result = controller.listTaskQuestions("task-1", session);

        assertEquals(500, result.getCode());
    }

    @Test
    void listTaskQuestionsSuccess() {
        when(auth.isLoggedIn(session)).thenReturn(true);
        when(taskQuestionService.listByTaskNo("task-1")).thenReturn(List.of(new TaskQuestion()));

        Result<List<TaskQuestion>> result = controller.listTaskQuestions("task-1", session);

        assertEquals(200, result.getCode());
    }

    // ============ addToTask ============

    @Test
    void addToTaskTaskNotFound() {
        when(taskService.getById("task-1")).thenReturn(null);

        Result<Void> result = controller.addToTask("task-1", List.of("q1"), session);

        assertEquals(500, result.getCode());
    }

    @Test
    void addToTaskRequiresPermission() {
        LearningTask task = new LearningTask();
        task.setCourseCode("CS101");
        when(taskService.getById("task-1")).thenReturn(task);
        when(auth.canModifyCourse(session, "CS101")).thenReturn(false);

        Result<Void> result = controller.addToTask("task-1", List.of("q1"), session);

        assertEquals(500, result.getCode());
    }

    @Test
    void addToTaskHandlesIllegalArgument() {
        LearningTask task = new LearningTask();
        task.setCourseCode("CS101");
        when(taskService.getById("task-1")).thenReturn(task);
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);
        doThrow(new IllegalArgumentException("题目已存在")).when(taskQuestionService).addQuestionsToTask("task-1", List.of("q1"));

        Result<Void> result = controller.addToTask("task-1", List.of("q1"), session);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("题目已存在"));
    }

    @Test
    void addToTaskSuccess() {
        LearningTask task = new LearningTask();
        task.setCourseCode("CS101");
        when(taskService.getById("task-1")).thenReturn(task);
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);

        Result<Void> result = controller.addToTask("task-1", List.of("q1", "q2"), session);

        assertEquals(200, result.getCode());
        verify(taskQuestionService).addQuestionsToTask("task-1", List.of("q1", "q2"));
    }

    // ============ removeFromTask ============

    @Test
    void removeFromTaskTaskNotFound() {
        when(taskService.getById("task-1")).thenReturn(null);

        Result<Void> result = controller.removeFromTask("task-1", "q1", session);

        assertEquals(500, result.getCode());
    }

    @Test
    void removeFromTaskRequiresPermission() {
        LearningTask task = new LearningTask();
        task.setCourseCode("CS101");
        when(taskService.getById("task-1")).thenReturn(task);
        when(auth.canModifyCourse(session, "CS101")).thenReturn(false);

        Result<Void> result = controller.removeFromTask("task-1", "q1", session);

        assertEquals(500, result.getCode());
    }

    @Test
    void removeFromTaskHandlesIllegalArgument() {
        LearningTask task = new LearningTask();
        task.setCourseCode("CS101");
        when(taskService.getById("task-1")).thenReturn(task);
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);
        doThrow(new IllegalArgumentException("题目未关联")).when(taskQuestionService).removeQuestionFromTask("task-1", "q1");

        Result<Void> result = controller.removeFromTask("task-1", "q1", session);

        assertEquals(500, result.getCode());
    }

    @Test
    void removeFromTaskSuccess() {
        LearningTask task = new LearningTask();
        task.setCourseCode("CS101");
        when(taskService.getById("task-1")).thenReturn(task);
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);

        Result<Void> result = controller.removeFromTask("task-1", "q1", session);

        assertEquals(200, result.getCode());
        verify(taskQuestionService).removeQuestionFromTask("task-1", "q1");
    }
}
