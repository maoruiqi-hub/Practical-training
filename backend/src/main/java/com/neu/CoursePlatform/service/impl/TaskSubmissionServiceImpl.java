package com.neu.CoursePlatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neu.CoursePlatform.dto.TaskSubmissionDTO;
import com.neu.CoursePlatform.common.GameEventTypes;
import com.neu.CoursePlatform.common.SharedIds;
import com.neu.CoursePlatform.common.event.GameEvent;
import com.neu.CoursePlatform.common.event.GameEventPublisher;
import com.neu.CoursePlatform.entity.LearningTask;
import com.neu.CoursePlatform.entity.Question;
import com.neu.CoursePlatform.entity.Student;
import com.neu.CoursePlatform.entity.SubmissionAnswer;
import com.neu.CoursePlatform.entity.TaskSubmission;
import com.neu.CoursePlatform.mapper.TaskSubmissionMapper;
import com.neu.CoursePlatform.service.KnowledgePointService;
import com.neu.CoursePlatform.service.CourseGameConfigService;
import com.neu.CoursePlatform.service.FloorProgressService;
import com.neu.CoursePlatform.service.LearningTaskService;
import com.neu.CoursePlatform.service.QuestionService;
import com.neu.CoursePlatform.service.StudentService;
import com.neu.CoursePlatform.service.SubmissionAnswerService;
import com.neu.CoursePlatform.service.TaskSubmissionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TaskSubmissionServiceImpl extends ServiceImpl<TaskSubmissionMapper, TaskSubmission> implements TaskSubmissionService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final LearningTaskService taskService;
    private final StudentService studentService;
    private final QuestionService questionService;
    private final SubmissionAnswerService answerService;
    private final KnowledgePointService knowledgePointService;
    private final CourseGameConfigService gameConfigService;
    private final FloorProgressService floorProgressService;
    private final GameEventPublisher gameEventPublisher;

    public TaskSubmissionServiceImpl(LearningTaskService taskService, StudentService studentService,
                                     QuestionService questionService, SubmissionAnswerService answerService,
                                     KnowledgePointService knowledgePointService,
                                     CourseGameConfigService gameConfigService,
                                     FloorProgressService floorProgressService,
                                     GameEventPublisher gameEventPublisher) {
        this.taskService = taskService;
        this.studentService = studentService;
        this.questionService = questionService;
        this.answerService = answerService;
        this.knowledgePointService = knowledgePointService;
        this.gameConfigService = gameConfigService;
        this.floorProgressService = floorProgressService;
        this.gameEventPublisher = gameEventPublisher;
    }

    @Override
    public List<TaskSubmission> listByStudentNo(String studentNo) {
        return baseMapper.selectByStudentNo(studentNo);
    }

    @Override
    public List<TaskSubmission> listByTaskNo(String taskNo) {
        return baseMapper.selectByTaskNo(taskNo);
    }

    @Override
    public List<TaskSubmissionDTO> listDtoByTaskNo(String taskNo) {
        LearningTask task = taskService.getById(taskNo);
        // 每个学生只取最新一条有效提交（排除 superseded）
        List<TaskSubmission> list = baseMapper.selectByTaskNo(taskNo);
        // 过滤掉被覆盖的旧提交
        list = list.stream()
                .filter(s -> !"superseded".equals(s.getStatus()))
                .collect(Collectors.toList());
        // 按学生分组，每个学生只保留 attemptNumber 最大的
        Map<String, TaskSubmission> latestPerStudent = new LinkedHashMap<>();
        for (TaskSubmission sub : list) {
            String key = sub.getStudentNo();
            TaskSubmission existing = latestPerStudent.get(key);
            if (existing == null || sub.getAttemptNumber() > (existing.getAttemptNumber() != null ? existing.getAttemptNumber() : 0)) {
                latestPerStudent.put(key, sub);
            }
        }
        List<TaskSubmissionDTO> dtos = new ArrayList<>();
        for (TaskSubmission sub : latestPerStudent.values()) {
            TaskSubmissionDTO dto = new TaskSubmissionDTO();
            dto.setSubmissionId(sub.getSubmissionId());
            dto.setTaskNo(sub.getTaskNo());
            dto.setTaskName(task != null ? task.getDescription() : "");
            dto.setTaskType(task != null ? task.getTaskType() : "");
            dto.setStudentNo(sub.getStudentNo());
            Student stu = studentService.getById(sub.getStudentNo());
            dto.setStudentName(stu != null ? stu.getName() : "");
            dto.setContent(sub.getContent());
            dto.setFilePath(sub.getFilePath());
            dto.setSubmitTime(sub.getSubmitTime());
            dto.setScore(sub.getScore());
            dto.setStatus(sub.getStatus());
            dto.setFeedback(sub.getFeedback());
            dto.setAttemptNumber(sub.getAttemptNumber());
            dtos.add(dto);
        }
        return dtos;
    }

    @Override
    public boolean isTaskOverdue(String taskNo) {
        LearningTask task = taskService.getById(taskNo);
        return task != null && task.getDeadline() != null && LocalDateTime.now().isAfter(task.getDeadline());
    }

    @Override
    public boolean hasSubmitted(String taskNo, String studentNo) {
        return baseMapper.selectCount(new QueryWrapper<TaskSubmission>()
                .eq("task_no", taskNo).eq("student_no", studentNo)) > 0;
    }

    @Override
    public int countByStudentAndTask(String taskNo, String studentNo) {
        Long count = baseMapper.selectCount(new QueryWrapper<TaskSubmission>()
                .eq("task_no", taskNo).eq("student_no", studentNo));
        return count != null ? count.intValue() : 0;
    }

    @Override
    public String getTaskCourseCode(String taskNo) {
        LearningTask task = taskService.getById(taskNo);
        return task != null ? task.getCourseCode() : null;
    }

    @Override
    public Map<String, Object> buildGradeDetail(String submissionId) {
        TaskSubmission sub = getById(submissionId);
        if (sub == null) return null;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("submissionId", sub.getSubmissionId());
        result.put("studentNo", sub.getStudentNo());
        result.put("content", sub.getContent());
        LearningTask task = taskService.getById(sub.getTaskNo());
        result.put("taskName", task != null ? task.getDescription() : "");
        result.put("taskType", task != null ? task.getTaskType() : "");
        result.put("score", sub.getScore());
        result.put("status", sub.getStatus());
        result.put("feedback", sub.getFeedback());
        List<Map<String, Object>> details = buildAnswerDetails(sub);
        result.put("details", details);
        result.put("autoScore", autoScoreChoices(sub));
        result.put("needsManualReview", containsManualQuestions(sub));
        return result;
    }

    @Override
    public void applyInitialGrading(TaskSubmission sub) {
        LearningTask task = taskService.getById(sub.getTaskNo());
        if (task == null) {
            sub.setStatus("submitted");
            return;
        }

        String taskType = task.getTaskType();

        // 测验/试卷类：系统自动评阅客观题
        if (taskService.isQuizTask(task)) {
            List<Map<String, Object>> answers = parseQuizAnswers(sub);
            sub.setScore(autoScoreChoices(answers));
            if (containsManualQuestions(answers)) {
                sub.setStatus("submitted");
                sub.setFeedback("客观题已自动评阅，主观题/编程题待教师复核");
            } else {
                sub.setStatus("graded");
                sub.setFeedback("系统已自动评阅");
            }
            return;
        }

        // 视频/阅读类：自动完成
        if ("video".equals(taskType) || "reading".equals(taskType)) {
            sub.setStatus("graded");
            sub.setFeedback("系统自动记录完成");
            return;
        }

        // 报告/作业/实践类：必须教师人工评阅
        sub.setStatus("submitted");
        sub.setFeedback("待教师评阅");
    }

    @Override
    @Transactional
    public void submitWithGrading(TaskSubmission sub) {
        applyInitialGrading(sub);
        save(sub);
        saveAnswerDetails(sub);
        publishAssessmentResultEvents(sub);
    }

    @Override
    public int autoScoreChoices(TaskSubmission sub) {
        if (!isQuizSubmission(sub)) return 0;
        return autoScoreChoices(parseQuizAnswers(sub));
    }

    @Override
    public void supersedePrevious(String taskNo, String studentNo) {
        List<TaskSubmission> oldSubs = baseMapper.selectList(new QueryWrapper<TaskSubmission>()
                .eq("task_no", taskNo)
                .eq("student_no", studentNo)
                .ne("status", "superseded"));
        for (TaskSubmission s : oldSubs) {
            s.setStatus("superseded");
            baseMapper.updateById(s);
        }
    }

    private int autoScoreChoices(List<Map<String, Object>> answers) {
        int autoScore = 0;
        for (Map<String, Object> ans : answers) {
            Question q = questionService.getById(String.valueOf(ans.get("no")));
            if (q != null && isAutoGradable(q) && isAnswerCorrect(q, ans.get("response"))) {
                autoScore += q.getScore() != null ? q.getScore() : 0;
            }
        }
        return autoScore;
    }

    private boolean containsManualQuestions(TaskSubmission sub) {
        if (!isQuizSubmission(sub)) return false;
        return containsManualQuestions(parseQuizAnswers(sub));
    }

    private boolean containsManualQuestions(List<Map<String, Object>> answers) {
        for (Map<String, Object> ans : answers) {
            Question q = questionService.getById(String.valueOf(ans.get("no")));
            if (q != null && !isAutoGradable(q)) return true;
        }
        return false;
    }

    private void saveAnswerDetails(TaskSubmission sub) {
        LearningTask task = taskService.getById(sub.getTaskNo());
        if (!taskService.isQuizTask(task) || sub.getSubmissionId() == null) return;

        List<SubmissionAnswer> answers = buildSubmissionAnswers(sub, parseQuizAnswers(sub));
        if (!answers.isEmpty()) answerService.saveBatch(answers);
    }

    private List<SubmissionAnswer> buildSubmissionAnswers(TaskSubmission sub, List<Map<String, Object>> answers) {
        List<SubmissionAnswer> result = new ArrayList<>();
        for (Map<String, Object> ans : answers) {
            String questionId = String.valueOf(ans.get("no"));
            Question q = questionService.getById(questionId);
            if (q == null) continue;

            Object response = ans.get("response");
            boolean autoGradable = isAutoGradable(q);
            boolean correct = autoGradable && isAnswerCorrect(q, response);
            int maxScore = q.getScore() != null ? q.getScore() : 0;

            SubmissionAnswer item = new SubmissionAnswer();
            item.setSubmissionId(sub.getSubmissionId());
            item.setTaskNo(sub.getTaskNo());
            item.setStudentNo(sub.getStudentNo());
            item.setQuestionId(questionId);
            item.setQuestionStem(q.getStem());
            item.setQuestionType(q.getType());
            item.setKnowledgePointId(q.getKnowledgePointId());
            item.setStudentAnswer(response == null ? "" : String.valueOf(response));
            item.setCorrectAnswer(q.getAnswer());
            item.setAutoGradable(autoGradable);
            item.setCorrect(autoGradable ? correct : null);
            item.setMaxScore(maxScore);
            item.setScore(correct ? maxScore : 0);
            item.setCreateTime(LocalDateTime.now());
            result.add(item);
        }
        return result;
    }

    /**
     * 模块三只发布判分结果事件；掌握度等下游数据由对应模块订阅事件后自行维护。
     * 楼层事件委托模块一的 FloorProgressService 产生，避免模块三写模块一表。
     */
    @Override
    public void publishAssessmentResultEvents(TaskSubmission sub) {
        LearningTask task = taskService.getById(sub.getTaskNo());
        if (task == null || !taskService.isQuizTask(task)) return;
        List<SubmissionAnswer> answers = answerService.listBySubmissionId(sub.getSubmissionId());
        for (SubmissionAnswer answer : answers) {
            if (!Boolean.TRUE.equals(answer.getAutoGradable()) || answer.getCorrect() == null) continue;
            GameEvent answerEvent = GameEvent.builder().eventId(SharedIds.newId())
                    .eventType(Boolean.TRUE.equals(answer.getCorrect())
                            ? GameEventTypes.ANSWER_CORRECT : GameEventTypes.ANSWER_WRONG)
                    .studentId(sub.getStudentNo()).courseId(task.getCourseCode())
                    .sourceId(answer.getId()).occurredAt(LocalDateTime.now())
                    .payload(Map.of("question_id", answer.getQuestionId(),
                            "knowledge_point_id", answer.getKnowledgePointId() == null ? "" : answer.getKnowledgePointId(),
                            "difficulty", questionDifficulty(answer.getQuestionId()),
                            "is_first_attempt", isFirstAttempt(answer), "attempt_count", attemptCount(answer),
                            "time_spent_ms", 0, "error_type", Boolean.TRUE.equals(answer.getCorrect()) ? "" : "wrong_answer")).build();
            gameEventPublisher.publish(answerEvent);
        }
        answers.stream().filter(answer -> Boolean.TRUE.equals(answer.getAutoGradable()) && answer.getKnowledgePointId() != null && !answer.getKnowledgePointId().isBlank())
                .collect(Collectors.groupingBy(SubmissionAnswer::getKnowledgePointId)).forEach((knowledgePointId, pointAnswers) -> {
                    boolean allCorrect = pointAnswers.stream().filter(answer -> Boolean.TRUE.equals(answer.getAutoGradable()))
                            .allMatch(answer -> Boolean.TRUE.equals(answer.getCorrect()));
                    floorProgressService.recordQuizResult(sub.getStudentNo(), task.getCourseCode(), knowledgePointId,
                            sub.getSubmissionId(), allCorrect, pointAnswers.stream().mapToInt(answer -> answer.getMaxScore() == null ? 0 : answer.getMaxScore()).sum());
                });
        boolean bossPassed = !answers.isEmpty() && answers.stream()
                .filter(answer -> Boolean.TRUE.equals(answer.getAutoGradable()))
                .allMatch(answer -> Boolean.TRUE.equals(answer.getCorrect()));
        if (bossPassed && isBossTask(task) && gameConfigService.isEnabled(task.getCourseCode())) {
            gameEventPublisher.publish(GameEvent.builder().eventId(SharedIds.newId())
                    .eventType(GameEventTypes.BOSS_DEFEATED).studentId(sub.getStudentNo())
                    .courseId(task.getCourseCode()).sourceId(sub.getSubmissionId()).occurredAt(LocalDateTime.now())
                    .payload(Map.of("taskId", task.getTaskNo())).build());
        }
    }

    private boolean isBossTask(LearningTask task) {
        return task.getTaskType() != null && ("boss".equalsIgnoreCase(task.getTaskType())
                || "boss_exam".equalsIgnoreCase(task.getTaskType()));
    }

    private int questionDifficulty(String questionId) {
        Question question = questionService.getById(questionId);
        return question == null || question.getDifficulty() == null ? 1 : question.getDifficulty();
    }

    private int attemptCount(SubmissionAnswer answer) {
        return answerService.listByStudentNo(answer.getStudentNo(), null, answer.getKnowledgePointId(), null).stream()
                .filter(item -> answer.getQuestionId().equals(item.getQuestionId())).toList().size();
    }

    private boolean isFirstAttempt(SubmissionAnswer answer) { return attemptCount(answer) <= 1; }

    private List<Map<String, Object>> buildAnswerDetails(TaskSubmission sub) {
        List<Map<String, Object>> details = new ArrayList<>();
        List<SubmissionAnswer> savedAnswers = answerService.listBySubmissionId(sub.getSubmissionId());
        if (!savedAnswers.isEmpty()) {
            for (SubmissionAnswer answer : savedAnswers) {
                Question q = questionService.getById(answer.getQuestionId());
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("questionId", answer.getQuestionId());
                item.put("stem", answer.getQuestionStem() != null ? answer.getQuestionStem() : (q != null ? q.getStem() : ""));
                item.put("type", answer.getQuestionType());
                item.put("knowledgePointId", answer.getKnowledgePointId());
                item.put("knowledgePointName", knowledgeName(answer.getKnowledgePointId()));
                item.put("studentAnswer", answer.getStudentAnswer());
                item.put("correctAnswer", answer.getCorrectAnswer());
                item.put("score", answer.getMaxScore());
                item.put("earnedScore", answer.getScore());
                item.put("autoGradable", Boolean.TRUE.equals(answer.getAutoGradable()));
                item.put("correct", answer.getCorrect());
                details.add(item);
            }
            return details;
        }

        try {
            for (Map<String, Object> ans : parseQuizAnswers(sub)) {
                Question q = questionService.getById(String.valueOf(ans.get("no")));
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("questionId", ans.get("no"));
                item.put("stem", q != null ? q.getStem() : "");
                item.put("type", q != null ? q.getType() : "");
                item.put("knowledgePointId", q != null ? q.getKnowledgePointId() : "");
                item.put("knowledgePointName", q != null ? knowledgeName(q.getKnowledgePointId()) : "");
                item.put("studentAnswer", ans.getOrDefault("response", ""));
                item.put("correctAnswer", q != null ? q.getAnswer() : "");
                item.put("score", q != null ? q.getScore() : 0);
                item.put("earnedScore", q != null && isAutoGradable(q) && isAnswerCorrect(q, ans.get("response")) ? q.getScore() : 0);
                item.put("autoGradable", q != null && isAutoGradable(q));
                item.put("correct", q != null && isAnswerCorrect(q, ans.get("response")));
                details.add(item);
            }
        } catch (IllegalArgumentException e) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("parseError", e.getMessage());
            details.add(item);
        }
        return details;
    }

    private boolean isQuizSubmission(TaskSubmission sub) {
        if (sub == null || sub.getTaskNo() == null) return false;
        LearningTask task = taskService.getById(sub.getTaskNo());
        return taskService.isQuizTask(task);
    }

    private List<Map<String, Object>> parseQuizAnswers(TaskSubmission sub) {
        if (sub.getContent() == null || sub.getContent().isBlank()) {
            throw new IllegalArgumentException("在线测验答题内容不能为空");
        }
        try {
            List<?> rawAnswers = objectMapper.readValue(sub.getContent(), List.class);
            List<Map<String, Object>> answers = new ArrayList<>();
            for (Object item : rawAnswers) {
                if (!(item instanceof Map<?, ?> rawMap)) {
                    throw new IllegalArgumentException("在线测验答题格式错误");
                }
                Map<String, Object> answer = new LinkedHashMap<>();
                rawMap.forEach((key, value) -> answer.put(String.valueOf(key), value));
                if (answer.get("no") == null) {
                    throw new IllegalArgumentException("在线测验答题缺少题目编号");
                }
                answers.add(answer);
            }
            return answers;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("在线测验答题格式错误");
        }
    }

    private boolean isAutoGradable(Question q) {
        return "single".equals(q.getType()) || "multi".equals(q.getType()) || "fill".equals(q.getType());
    }

    private boolean isAnswerCorrect(Question q, Object response) {
        if (q.getAnswer() == null) return false;
        String studentAnswer = response == null ? "" : String.valueOf(response).trim();
        String correctAnswer = q.getAnswer().trim();
        if ("multi".equals(q.getType())) {
            return splitAnswerSet(correctAnswer).equals(splitAnswerSet(studentAnswer));
        }
        return correctAnswer.equalsIgnoreCase(studentAnswer);
    }

    private Set<String> splitAnswerSet(String answer) {
        return Arrays.stream(answer.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private String knowledgeName(String knowledgePointId) {
        if (knowledgePointId == null || knowledgePointId.isBlank()) return "";
        var point = knowledgePointService.getById(knowledgePointId);
        return point != null ? point.getName() : "";
    }
}
