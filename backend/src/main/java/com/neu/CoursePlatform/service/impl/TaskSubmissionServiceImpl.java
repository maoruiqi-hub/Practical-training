package com.neu.CoursePlatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
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
import com.neu.CoursePlatform.entity.TaskQuestion;
import com.neu.CoursePlatform.mapper.TaskSubmissionMapper;
import com.neu.CoursePlatform.service.KnowledgePointService;
import com.neu.CoursePlatform.service.CourseGameConfigService;
import com.neu.CoursePlatform.service.FloorProgressService;
import com.neu.CoursePlatform.service.LearningTaskService;
import com.neu.CoursePlatform.service.LearningEvidenceService;
import com.neu.CoursePlatform.service.QuestionService;
import com.neu.CoursePlatform.service.StudentService;
import com.neu.CoursePlatform.service.SubmissionAnswerService;
import com.neu.CoursePlatform.service.TaskSubmissionService;
import com.neu.CoursePlatform.service.TaskQuestionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TaskSubmissionServiceImpl extends ServiceImpl<TaskSubmissionMapper, TaskSubmission> implements TaskSubmissionService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final List<String> OPTION_LETTERS = List.of(
            "A", "B", "C", "D", "E", "F", "G", "H", "I", "J",
            "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T",
            "U", "V", "W", "X", "Y", "Z");
    private static final Pattern MARKED_OPTION_PATTERN =
            Pattern.compile("^\\s*([A-Za-z])\\s*(?:[.)]|[:：]|\\u3001|\\uFF0E)\\s*(.*)$");
    private static final Pattern SPACED_OPTION_PATTERN =
            Pattern.compile("^\\s*([A-Za-z])\\s+(.+)$");
    private final LearningTaskService taskService;
    private final StudentService studentService;
    private final QuestionService questionService;
    private final SubmissionAnswerService answerService;
    private final KnowledgePointService knowledgePointService;
    private final CourseGameConfigService gameConfigService;
    private final FloorProgressService floorProgressService;
    private final GameEventPublisher gameEventPublisher;
    private final LearningEvidenceService learningEvidenceService;
    private final TaskQuestionService taskQuestionService;

    public TaskSubmissionServiceImpl(LearningTaskService taskService, StudentService studentService,
                                     QuestionService questionService, SubmissionAnswerService answerService,
                                     KnowledgePointService knowledgePointService,
                                     CourseGameConfigService gameConfigService,
                                     FloorProgressService floorProgressService,
                                     GameEventPublisher gameEventPublisher,
                                     LearningEvidenceService learningEvidenceService,
                                     TaskQuestionService taskQuestionService) {
        this.taskService = taskService;
        this.studentService = studentService;
        this.questionService = questionService;
        this.answerService = answerService;
        this.knowledgePointService = knowledgePointService;
        this.gameConfigService = gameConfigService;
        this.floorProgressService = floorProgressService;
        this.gameEventPublisher = gameEventPublisher;
        this.learningEvidenceService = learningEvidenceService;
        this.taskQuestionService = taskQuestionService;
    }

    @Override
    public List<TaskSubmission> listByStudentNo(String studentNo) {
        return baseMapper.selectByStudentNo(studentNo);
    }

    @Override
    public List<TaskSubmission> listByStudentNoAndCourse(String studentNo, String courseCode) {
        if (courseCode == null || courseCode.isBlank()) {
            return listByStudentNo(studentNo);
        }
        List<TaskSubmission> submissions = baseMapper.selectByStudentNoAndCourse(studentNo, courseCode);
        return submissions == null ? List.of() : submissions;
    }

    @Override
    public List<TaskSubmission> listByTaskNo(String taskNo) {
        return baseMapper.selectByTaskNo(taskNo);
    }

    @Override
    public List<TaskSubmission> listByCourseCode(String courseCode) {
        return baseMapper.selectByCourseCode(courseCode);
    }

    @Override
    public Map<String, Object> aggregateTaskStats(String taskNo) {
        Map<String, Object> stats = baseMapper.selectTaskStats(taskNo);
        return stats != null ? stats : Map.of();
    }

    @Override
    public List<Map<String, Object>> aggregateCourseTaskStats(String courseCode) {
        List<Map<String, Object>> stats = baseMapper.selectCourseTaskStats(courseCode);
        return stats != null ? stats : List.of();
    }

    @Override
    public List<TaskSubmissionDTO> listDtoByTaskNo(String taskNo) {
        List<TaskSubmissionDTO> fastList = baseMapper.selectLatestDtoByTaskNo(taskNo);
        if (fastList != null && !fastList.isEmpty()) {
            return fastList;
        }
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
        Set<String> studentNos = latestPerStudent.values().stream()
                .map(TaskSubmission::getStudentNo)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Collection<Student> students = studentNos.isEmpty() ? List.of() : studentService.listByIds(studentNos);
        if (students == null) {
            students = studentNos.stream()
                    .map(studentService::getById)
                    .filter(Objects::nonNull)
                    .toList();
        }
        Map<String, Student> studentIndex = students.stream()
                .collect(Collectors.toMap(Student::getStudentNo, student -> student, (a, b) -> a));
        List<TaskSubmissionDTO> dtos = new ArrayList<>();
        for (TaskSubmission sub : latestPerStudent.values()) {
            TaskSubmissionDTO dto = new TaskSubmissionDTO();
            dto.setSubmissionId(sub.getSubmissionId());
            dto.setTaskNo(sub.getTaskNo());
            dto.setTaskName(task != null ? task.getDescription() : "");
            dto.setTaskType(task != null ? task.getTaskType() : "");
            dto.setStudentNo(sub.getStudentNo());
            Student stu = studentIndex.get(sub.getStudentNo());
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
            List<Map<String, Object>> answers = standardizedQuizAnswers(sub);
            sub.setScore(capToTaskScore(sub, autoScoreChoices(answers)));
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
        if (sub.getSubmissionId() == null || sub.getSubmissionId().isBlank()) {
            throw new IllegalStateException("提交记录未生成有效编号");
        }
        baseMapper.markSupersededPrevious(sub.getTaskNo(), sub.getStudentNo(), sub.getSubmissionId());
        publishAssessmentResultEvents(sub);
    }

    @Override
    public int autoScoreChoices(TaskSubmission sub) {
        if (!isQuizSubmission(sub)) return 0;
        return capToTaskScore(sub, autoScoreChoices(standardizedQuizAnswers(sub)));
    }

    private int autoScoreChoices(List<Map<String, Object>> answers) {
        int autoScore = 0;
        Map<String, Question> questionIndex = questionIndex(answers);
        for (Map<String, Object> ans : answers) {
            Question q = questionIndex.get(String.valueOf(ans.get("no")));
            if (q != null && isAutoGradable(q) && isAnswerCorrect(q, ans.get("response"))) {
                autoScore += q.getScore() != null ? q.getScore() : 0;
            }
        }
        return autoScore;
    }

    private boolean containsManualQuestions(TaskSubmission sub) {
        if (!isQuizSubmission(sub)) return false;
        return containsManualQuestions(standardizedQuizAnswers(sub));
    }

    private boolean containsManualQuestions(List<Map<String, Object>> answers) {
        Map<String, Question> questionIndex = questionIndex(answers);
        for (Map<String, Object> ans : answers) {
            Question q = questionIndex.get(String.valueOf(ans.get("no")));
            if (q != null && !isAutoGradable(q)) return true;
        }
        return false;
    }

    private void saveAnswerDetails(TaskSubmission sub) {
        LearningTask task = taskService.getById(sub.getTaskNo());
        if (!taskService.isQuizTask(task) || sub.getSubmissionId() == null) return;

        List<SubmissionAnswer> answers = buildSubmissionAnswers(sub, standardizedQuizAnswers(sub));
        if (!answers.isEmpty()) answerService.saveBatch(answers);
    }

    private List<SubmissionAnswer> buildSubmissionAnswers(TaskSubmission sub, List<Map<String, Object>> answers) {
        List<SubmissionAnswer> result = new ArrayList<>();
        Map<String, Question> questionIndex = questionIndex(answers);
        for (Map<String, Object> ans : answers) {
            String questionId = String.valueOf(ans.get("no"));
            Question q = questionIndex.get(questionId);
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

    private Map<String, Question> questionIndex(List<Map<String, Object>> answers) {
        List<String> ids = answers.stream()
                .map(answer -> answer.get("no"))
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .distinct()
                .toList();
        if (ids.isEmpty()) return Map.of();
        Collection<Question> questions = questionService.listByIds(ids);
        if (questions == null) {
            questions = ids.stream()
                    .map(questionService::getById)
                    .filter(Objects::nonNull)
                    .toList();
        }
        return questions.stream()
                .collect(Collectors.toMap(Question::getQuestionId, question -> question, (a, b) -> a));
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
        if (learningEvidenceService != null) {
            List<Map<String, Object>> evidenceAnswers = answers.stream()
                    .filter(answer -> Boolean.TRUE.equals(answer.getAutoGradable()))
                    .map(answer -> {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("questionId", answer.getQuestionId());
                        item.put("studentAnswer", answer.getStudentAnswer());
                        item.put("answered", answer.getStudentAnswer() != null && !answer.getStudentAnswer().isBlank());
                        return item;
                    }).toList();
            Set<String> allowedQuestionIds = answers.stream().map(SubmissionAnswer::getQuestionId)
                    .filter(Objects::nonNull).collect(Collectors.toSet());
            learningEvidenceService.recordVerifiedAnswers(sub.getStudentNo(), task.getCourseCode(),
                    sub.getSubmissionId(), "quiz", evidenceAnswers, allowedQuestionIds);
        }
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
        publishBossCompletionEvent(sub);
    }

    @Override
    public void publishBossCompletionEvent(TaskSubmission sub) {
        if (sub == null || sub.getTaskNo() == null || sub.getSubmissionId() == null
                || !"graded".equalsIgnoreCase(sub.getStatus())) return;
        LearningTask task = taskService.getById(sub.getTaskNo());
        if (task != null && isBossTask(task) && task.getScore() != null && task.getScore() > 0
                && sub.getScore() != null && sub.getScore() >= task.getScore()
                && gameConfigService.isEnabled(task.getCourseCode())) {
            gameEventPublisher.publish(GameEvent.builder().eventId(SharedIds.newId())
                    .eventType(GameEventTypes.BOSS_DEFEATED).studentId(sub.getStudentNo())
                    .courseId(task.getCourseCode()).sourceId(sub.getSubmissionId()).occurredAt(LocalDateTime.now())
                    .payload(Map.of("taskId", task.getTaskNo())).build());
        }
    }

    @Override
    @Transactional
    public void recordReviewedSubjectiveEvidence(TaskSubmission sub, List<Map<String, Object>> manualAnswers) {
        if (manualAnswers == null || manualAnswers.isEmpty()) return;
        LearningTask task = taskService.getById(sub.getTaskNo());
        if (task == null || !taskService.isQuizTask(task)) return;
        Map<String, SubmissionAnswer> answerIndex = answerService.listBySubmissionId(sub.getSubmissionId()).stream()
                .collect(Collectors.toMap(SubmissionAnswer::getQuestionId, answer -> answer, (a, b) -> a));
        List<Map<String, Object>> reviewed = new ArrayList<>();
        Set<String> allowed = new LinkedHashSet<>();
        Set<String> reviewedQuestionIds = new HashSet<>();
        for (Map<String, Object> grade : manualAnswers) {
            String questionId = String.valueOf(grade.getOrDefault("questionId", ""));
            if (!reviewedQuestionIds.add(questionId)) {
                throw new IllegalArgumentException("同一主观题不能重复干预");
            }
            SubmissionAnswer answer = answerIndex.get(questionId);
            if (answer == null || Boolean.TRUE.equals(answer.getAutoGradable())) {
                throw new IllegalArgumentException("主观题复核结果与当前提交不匹配");
            }
            int maxScore = answer.getMaxScore() == null ? 0 : answer.getMaxScore();
            int reviewedScore = Math.max(0, Math.min(maxScore, intValue(grade.get("score"), 0)));
            boolean correct = maxScore > 0 && reviewedScore >= maxScore;
            answer.setScore(reviewedScore);
            answer.setCorrect(correct);
            answerService.updateById(answer);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("questionId", questionId);
            item.put("studentAnswer", answer.getStudentAnswer());
            item.put("answered", answer.getStudentAnswer() != null && !answer.getStudentAnswer().isBlank());
            item.put("correct", correct);
            reviewed.add(item);
            allowed.add(questionId);
        }
        learningEvidenceService.recordReviewedAnswers(sub.getStudentNo(), task.getCourseCode(),
                sub.getSubmissionId(), reviewed, allowed);
    }

    @Override
    public int recalculateFinalScore(TaskSubmission sub) {
        if (sub == null) throw new IllegalArgumentException("提交记录不存在");
        LearningTask task = taskService.getById(sub.getTaskNo());
        if (task == null || !taskService.isQuizTask(task)) return capToTaskScore(sub, sub.getScore() == null ? 0 : sub.getScore());
        int total = answerService.listBySubmissionId(sub.getSubmissionId()).stream()
                .map(SubmissionAnswer::getScore)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        return capToTaskScore(sub, total);
    }

    private int intValue(Object value, int fallback) {
        if (value instanceof Number number) return number.intValue();
        try {
            return value == null ? fallback : Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return fallback;
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
            for (Map<String, Object> ans : standardizedQuizAnswers(sub)) {
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
                String questionId = String.valueOf(answer.get("no")).trim();
                if (questionId.isBlank()) throw new IllegalArgumentException("在线测验答题缺少题目编号");
                answer.put("no", questionId);
                answers.add(answer);
            }
            return answers;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("在线测验答题格式错误");
        }
    }

    private List<Map<String, Object>> standardizedQuizAnswers(TaskSubmission sub) {
        if (taskQuestionService == null) {
            throw new IllegalStateException("测验题目服务不可用");
        }
        List<TaskQuestion> taskQuestions = taskQuestionService.listByTaskNo(sub.getTaskNo());
        if (taskQuestions == null || taskQuestions.isEmpty()) {
            throw new IllegalArgumentException("当前测验未配置题目");
        }
        LinkedHashSet<String> expectedIds = taskQuestions.stream()
                .map(TaskQuestion::getQuestionId)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(id -> !id.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (expectedIds.isEmpty() || expectedIds.size() != taskQuestions.size()) {
            throw new IllegalStateException("测验题目配置为空或包含重复题目");
        }

        Map<String, Map<String, Object>> submitted = new LinkedHashMap<>();
        for (Map<String, Object> answer : parseQuizAnswers(sub)) {
            String questionId = String.valueOf(answer.get("no"));
            if (!expectedIds.contains(questionId)) {
                throw new IllegalArgumentException("答案包含不属于当前测验的题目：" + questionId);
            }
            if (submitted.putIfAbsent(questionId, answer) != null) {
                throw new IllegalArgumentException("答案包含重复题目：" + questionId);
            }
        }

        List<Map<String, Object>> standardized = new ArrayList<>();
        for (String questionId : expectedIds) {
            Map<String, Object> answer = new LinkedHashMap<>(submitted.getOrDefault(questionId, Map.of()));
            answer.put("no", questionId);
            answer.putIfAbsent("response", "");
            standardized.add(answer);
        }
        return standardized;
    }

    private int capToTaskScore(TaskSubmission sub, int score) {
        LearningTask task = taskService.getById(sub.getTaskNo());
        int nonNegativeScore = Math.max(0, score);
        if (task == null || task.getScore() == null || task.getScore() <= 0) return nonNegativeScore;
        return Math.min(task.getScore(), nonNegativeScore);
    }

    private boolean isAutoGradable(Question q) {
        return "single".equals(q.getType()) || "multi".equals(q.getType()) || "fill".equals(q.getType());
    }

    private boolean isAnswerCorrect(Question q, Object response) {
        if (q.getAnswer() == null) return false;
        String type = q.getType() == null ? "" : q.getType();
        if ("single".equals(type)) {
            String expected = canonicalChoiceToken(q.getAnswer(), q);
            String actual = canonicalChoiceToken(response, q);
            return !expected.isBlank() && expected.equals(actual);
        }
        if ("multi".equals(q.getType())) {
            Set<String> expected = choiceTokenSet(q.getAnswer(), q);
            Set<String> actual = choiceTokenSet(response, q);
            return !expected.isEmpty() && expected.equals(actual);
        }
        return normalizeFillText(response).equals(normalizeFillText(q.getAnswer()));
    }

    private Set<String> choiceTokenSet(Object answer, Question question) {
        return splitAnswerTokens(answer).stream()
                .map(token -> canonicalChoiceToken(token, question))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private List<String> splitAnswerTokens(Object answer) {
        if (answer == null) return List.of();
        if (answer instanceof Collection<?> collection) {
            return collection.stream()
                    .filter(Objects::nonNull)
                    .map(String::valueOf)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
        return Arrays.stream(String.valueOf(answer).split("[,，;；]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private String canonicalChoiceToken(Object value, Question question) {
        String text = value == null ? "" : String.valueOf(value).trim();
        if (text.isEmpty()) return "";
        if (text.matches("[A-Z]")) return "option:" + text;

        MarkedOption marked = markedOption(text);
        if (marked != null && marked.letter() != null && marked.letter().matches("[A-Z]")) {
            return "option:" + marked.letter();
        }

        String normalized = normalizeChoiceText(text);
        for (OptionEntry option : optionEntries(question)) {
            if (option.aliases().contains(normalized)) return "option:" + option.letter();
        }
        return "text:" + normalized;
    }

    private List<OptionEntry> optionEntries(Question question) {
        String options = question == null ? null : question.getOptions();
        if (options == null || options.isBlank()) return List.of();
        try {
            JsonNode root = objectMapper.readTree(options);
            if (root.isArray()) {
                List<OptionEntry> entries = new ArrayList<>();
                for (int i = 0; i < root.size(); i++) {
                    entries.add(optionEntry(fallbackLetter(i), root.get(i).asText(""), ""));
                }
                return entries;
            }
            if (root.isObject()) {
                List<OptionEntry> entries = new ArrayList<>();
                int index = 0;
                Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> field = fields.next();
                    entries.add(optionEntry(fallbackLetter(index++), field.getValue().asText(""), field.getKey()));
                }
                return entries;
            }
        } catch (Exception ignored) {
        }

        String[] lines = options.split("\\R");
        List<OptionEntry> entries = new ArrayList<>();
        for (String line : lines) {
            if (line == null || line.trim().isEmpty()) continue;
            entries.add(optionEntry(fallbackLetter(entries.size()), line, ""));
        }
        return entries;
    }

    private OptionEntry optionEntry(String fallbackLetter, Object rawValue, Object key) {
        String raw = rawValue == null ? "" : String.valueOf(rawValue).trim();
        String keyText = key == null ? "" : String.valueOf(key).trim();
        MarkedOption marked = markedOption(raw);
        String keyLetter = keyText.matches("[A-Za-z]") ? keyText.toUpperCase(Locale.ROOT) : "";
        String letter = marked != null ? marked.letter() : (!keyLetter.isBlank() ? keyLetter : fallbackLetter);
        String display = marked != null && !marked.rest().isBlank() ? marked.rest() : raw;
        Set<String> aliases = new TreeSet<>();

        addAlias(aliases, raw);
        addAlias(aliases, display);
        if (!keyText.isBlank()) {
            addAlias(aliases, keyText);
            addAlias(aliases, keyText + ". " + display);
            addAlias(aliases, keyText + "、" + display);
        }
        if (!letter.isBlank()) {
            addAlias(aliases, letter + ". " + display);
            addAlias(aliases, letter + "、" + display);
        }
        return new OptionEntry(letter, display, aliases);
    }

    private void addAlias(Set<String> aliases, String value) {
        String normalized = normalizeChoiceText(value);
        if (!normalized.isBlank()) aliases.add(normalized);
    }

    private MarkedOption markedOption(String value) {
        if (value == null) return null;
        Matcher marked = MARKED_OPTION_PATTERN.matcher(value);
        if (marked.matches()) {
            return new MarkedOption(marked.group(1).toUpperCase(Locale.ROOT), safeTrim(marked.group(2)));
        }
        Matcher spaced = SPACED_OPTION_PATTERN.matcher(value);
        if (spaced.matches()) {
            return new MarkedOption(spaced.group(1).toUpperCase(Locale.ROOT), safeTrim(spaced.group(2)));
        }
        return null;
    }

    private String fallbackLetter(int index) {
        return index >= 0 && index < OPTION_LETTERS.size() ? OPTION_LETTERS.get(index) : String.valueOf(index + 1);
    }

    private String normalizeChoiceText(Object value) {
        if (value == null) return "";
        return String.valueOf(value)
                .replace('\u3000', ' ')
                .trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }

    private String normalizeFillText(Object value) {
        return normalizeChoiceText(value);
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private record OptionEntry(String letter, String display, Set<String> aliases) {}

    private record MarkedOption(String letter, String rest) {}

    private String knowledgeName(String knowledgePointId) {
        if (knowledgePointId == null || knowledgePointId.isBlank()) return "";
        var point = knowledgePointService.getById(knowledgePointId);
        return point != null ? point.getName() : "";
    }
}
