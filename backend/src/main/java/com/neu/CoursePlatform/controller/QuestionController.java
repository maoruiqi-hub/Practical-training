package com.neu.CoursePlatform.controller;

import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.entity.LearningTask;
import com.neu.CoursePlatform.entity.Question;
import com.neu.CoursePlatform.entity.TaskQuestion;
import com.neu.CoursePlatform.service.LearningTaskService;
import com.neu.CoursePlatform.service.QuestionService;
import com.neu.CoursePlatform.service.TaskQuestionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/question")
public class QuestionController {

    private final QuestionService questionService;
    private final TaskQuestionService taskQuestionService;
    private final LearningTaskService taskService;
    private final Auth auth;

    public QuestionController(QuestionService questionService, TaskQuestionService taskQuestionService,
                              LearningTaskService taskService, Auth auth) {
        this.questionService = questionService;
        this.taskQuestionService = taskQuestionService;
        this.taskService = taskService;
        this.auth = auth;
    }

    /** 查看单个题目 | 登录用户 */
    @GetMapping("/{questionId}")
    public Result<Question> detail(@PathVariable String questionId, HttpSession session) {
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
        Question q = questionService.getById(questionId);
        return q != null ? Result.ok(q) : Result.fail("题目不存在");
    }

    // ==================== 题库 CRUD（教师） ====================

    /** 按课程查题目 | admin/授课教师 */
    @GetMapping("/course/{courseCode}")
    public Result<List<Question>> listByCourse(@PathVariable String courseCode, HttpSession session) {
        if (!auth.canModifyCourse(session, courseCode)) return Result.fail("无权限");
        return Result.ok(questionService.listByCourseCode(courseCode));
    }

    /** 按课时查题目 | admin/授课教师 */
    @GetMapping("/lesson/{lessonNo}")
    public Result<List<Question>> listByLesson(@PathVariable String lessonNo, HttpSession session) {
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
        return Result.ok(questionService.listByLessonNo(lessonNo));
    }

    /** 模糊搜索题目 | admin/授课教师 */
    @GetMapping("/search")
    public Result<List<Question>> search(@RequestParam String keyword, HttpSession session) {
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
        return Result.ok(questionService.searchByKeyword(keyword));
    }

    /** 新增题目 | admin/授课教师 */
    @PostMapping
    public Result<Void> add(@RequestBody Question question, HttpSession session) {
        if (!auth.canModifyCourse(session, question.getCourseCode())) return Result.fail("无权限");
        questionService.save(question);
        return Result.ok();
    }

    /** 修改题目 | admin/授课教师 */
    @PutMapping("/{questionId}")
    public Result<Void> update(@PathVariable String questionId, @RequestBody Question question, HttpSession session) {
        Question existing = questionService.getById(questionId);
        if (existing == null) return Result.fail("题目不存在");
        if (!auth.canModifyCourse(session, existing.getCourseCode())) return Result.fail("无权限");
        question.setQuestionId(questionId);
        questionService.updateById(question);
        return Result.ok();
    }

    /** 删除题目 | admin/授课教师 */
    @DeleteMapping("/{questionId}")
    public Result<Void> delete(@PathVariable String questionId, HttpSession session) {
        Question existing = questionService.getById(questionId);
        if (existing == null) return Result.fail("题目不存在");
        if (!auth.canModifyCourse(session, existing.getCourseCode())) return Result.fail("无权限");
        questionService.removeById(questionId);
        return Result.ok();
    }

    // ==================== 测验选题 ====================

    /** 查看某测验已选的题目 | 登录用户 */
    @GetMapping("/task/{taskNo}")
    public Result<List<TaskQuestion>> listTaskQuestions(@PathVariable String taskNo, HttpSession session) {
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
        return Result.ok(taskQuestionService.listByTaskNo(taskNo));
    }

    /** 向测验添加题目 | admin/授课教师 */
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

    /** 从测验移除题目 | admin/授课教师 */
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
}
