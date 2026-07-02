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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    private final QuestionService questionService;
    private final TaskQuestionService taskQuestionService;
    private final LearningTaskService taskService;
    private final KnowledgePointService knowledgePointService;
    private final Auth auth;

    public QuestionController(QuestionService questionService,
                              TaskQuestionService taskQuestionService,
                              LearningTaskService taskService,
                              KnowledgePointService knowledgePointService,
                              Auth auth) {
        this.questionService = questionService;
        this.taskQuestionService = taskQuestionService;
        this.taskService = taskService;
        this.knowledgePointService = knowledgePointService;
        this.auth = auth;
    }

    /**
     * 规范接口：GET /api/questions?course_id=&knowledge_point_id=&difficulty=
     * 兼容现有前端的 courseCode / lessonNo / knowledgePointId 参数。
     */
    @GetMapping
    public Result<List<Question>> list(@RequestParam Map<String, String> params, HttpSession session) {
        String courseCode = firstNonBlank(params.get("course_id"), params.get("courseCode"));
        String lessonNo = firstNonBlank(params.get("lesson_id"), params.get("lessonNo"));
        String knowledgePointId = firstNonBlank(params.get("knowledge_point_id"), params.get("knowledgePointId"));
        String type = firstNonBlank(params.get("type"), params.get("question_type"), params.get("questionType"));
        String keyword = params.get("keyword");

        Integer difficulty;
        try {
            difficulty = parseInteger(params.get("difficulty"));
        } catch (NumberFormatException e) {
            return Result.fail("difficulty 必须是整数");
        }

        if (courseCode != null && !courseCode.isBlank()) {
            if (!auth.canModifyCourse(session, courseCode)) return Result.fail("无权限");
        } else if (!auth.isLoggedIn(session)) {
            return Result.fail("请先登录");
        }

        return Result.ok(questionService.filterQuestions(courseCode, lessonNo, knowledgePointId, type, difficulty, keyword));
    }

    @GetMapping("/{questionId}")
    public Result<Question> detail(@PathVariable String questionId, HttpSession session) {
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
        Question question = questionService.getById(questionId);
        if (question != null && question.getKnowledgePointId() != null && !question.getKnowledgePointId().isBlank()) {
            question.setKnowledgePoint(knowledgePointService.getById(question.getKnowledgePointId()));
        }
        return question != null ? Result.ok(question) : Result.fail("题目不存在");
    }

    @GetMapping("/course/{courseCode}")
    public Result<List<Question>> listByCourse(@PathVariable String courseCode, HttpSession session) {
        if (!auth.canModifyCourse(session, courseCode)) return Result.fail("无权限");
        return Result.ok(questionService.listByCourseCode(courseCode));
    }

    @GetMapping("/lesson/{lessonNo}")
    public Result<List<Question>> listByLesson(@PathVariable String lessonNo, HttpSession session) {
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
        return Result.ok(questionService.listByLessonNo(lessonNo));
    }

    @GetMapping("/search")
    public Result<List<Question>> search(@RequestParam String keyword, HttpSession session) {
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
        return Result.ok(questionService.searchByKeyword(keyword));
    }

    @GetMapping("/filter")
    public Result<List<Question>> filter(@RequestParam(required = false) String courseCode,
                                         @RequestParam(required = false) String lessonNo,
                                         @RequestParam(required = false) String knowledgePointId,
                                         @RequestParam(required = false) String type,
                                         @RequestParam(required = false) Integer difficulty,
                                         @RequestParam(required = false) String keyword,
                                         HttpSession session) {
        if (courseCode != null && !courseCode.isBlank()) {
            if (!auth.canModifyCourse(session, courseCode)) return Result.fail("无权限");
        } else if (!auth.isLoggedIn(session)) {
            return Result.fail("请先登录");
        }
        return Result.ok(questionService.filterQuestions(courseCode, lessonNo, knowledgePointId, type, difficulty, keyword));
    }

    @PostMapping
    public Result<Void> add(@RequestBody Question question, HttpSession session) {
        if (!auth.canModifyCourse(session, question.getCourseCode())) return Result.fail("无权限");
        try {
            syncKnowledgePoint(question);
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
        questionService.save(question);
        return Result.ok();
    }

    @PutMapping("/{questionId}")
    public Result<Void> update(@PathVariable String questionId, @RequestBody Question question, HttpSession session) {
        Question existing = questionService.getById(questionId);
        if (existing == null) return Result.fail("题目不存在");
        if (!auth.canModifyCourse(session, existing.getCourseCode())) return Result.fail("无权限");

        question.setQuestionId(questionId);
        question.setCourseCode(existing.getCourseCode());
        try {
            syncKnowledgePoint(question);
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
        questionService.updateById(question);
        return Result.ok();
    }

    @PostMapping("/{questionId}/link-kp")
    public Result<Question> linkKnowledgePoint(@PathVariable String questionId,
                                               @RequestBody Map<String, String> body,
                                               HttpSession session) {
        Question question = questionService.getById(questionId);
        if (question == null) return Result.fail("题目不存在");
        if (!auth.canModifyCourse(session, question.getCourseCode())) return Result.fail("无权限");

        String knowledgePointId = body == null ? null : firstNonBlank(body.get("knowledge_point_id"), body.get("knowledgePointId"));
        if (knowledgePointId == null || knowledgePointId.isBlank()) return Result.fail("缺少 knowledgePointId");

        KnowledgePoint point = knowledgePointService.getById(knowledgePointId);
        if (point == null) return Result.fail("知识点不存在");
        if (!question.getCourseCode().equals(point.getCourseCode())) return Result.fail("知识点不属于当前课程");

        question.setKnowledgePointId(knowledgePointId);
        question.setKnowledgePoint(point);
        questionService.updateById(question);
        return Result.ok(question);
    }

    @DeleteMapping("/{questionId}")
    public Result<Void> delete(@PathVariable String questionId, HttpSession session) {
        Question existing = questionService.getById(questionId);
        if (existing == null) return Result.fail("题目不存在");
        if (!auth.canModifyCourse(session, existing.getCourseCode())) return Result.fail("无权限");
        questionService.removeById(questionId);
        return Result.ok();
    }

    @GetMapping("/task/{taskNo}")
    public Result<List<TaskQuestion>> listTaskQuestions(@PathVariable String taskNo, HttpSession session) {
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
        return Result.ok(taskQuestionService.listByTaskNo(taskNo));
    }

    @PostMapping("/task/{taskNo}")
    public Result<Void> addToTask(@PathVariable String taskNo, @RequestBody List<String> questionIds, HttpSession session) {
        LearningTask task = taskService.getById(taskNo);
        if (task == null) return Result.fail("任务不存在");
        if (!auth.canModifyCourse(session, task.getCourseCode())) return Result.fail("无权限");
        try {
            taskQuestionService.addQuestionsToTask(taskNo, questionIds);
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
        return Result.ok();
    }

    @DeleteMapping("/task/{taskNo}/{questionId}")
    public Result<Void> removeFromTask(@PathVariable String taskNo, @PathVariable String questionId, HttpSession session) {
        LearningTask task = taskService.getById(taskNo);
        if (task == null) return Result.fail("任务不存在");
        if (!auth.canModifyCourse(session, task.getCourseCode())) return Result.fail("无权限");
        try {
            taskQuestionService.removeQuestionFromTask(taskNo, questionId);
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
        return Result.ok();
    }

    private void syncKnowledgePoint(Question question) {
        if (question.getKnowledgePointId() == null || question.getKnowledgePointId().isBlank()) return;
        KnowledgePoint point = knowledgePointService.getById(question.getKnowledgePointId());
        if (point == null) throw new IllegalArgumentException("知识点不存在");
        if (!point.getCourseCode().equals(question.getCourseCode())) {
            throw new IllegalArgumentException("知识点不属于当前课程");
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }

    private Integer parseInteger(String value) {
        return value == null || value.isBlank() ? null : Integer.valueOf(value);
    }
}
