package com.neu.CoursePlatform.service.demo;

import com.neu.CoursePlatform.entity.Question;
import com.neu.CoursePlatform.entity.StudentTowerNode;
import com.neu.CoursePlatform.entity.StudentTowerRun;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DemoTowerDataService {
    private final DemoAccountSupport accountSupport;

    public DemoTowerDataService(DemoAccountSupport accountSupport) {
        this.accountSupport = accountSupport;
    }

    public boolean isDemoSecondLevel(StudentTowerRun run, StudentTowerNode node) {
        if (run == null || node == null) return false;
        if (!accountSupport.isDemoStudent(run.getStudentNo())) return false;
        return Integer.valueOf(2).equals(node.getNodeOrder()) && isBattleRoom(node.getRoomType());
    }

    public Map<String, Object> secondLevelQuestionPack(StudentTowerRun run, StudentTowerNode node, String mode) {
        List<Question> questions = secondLevelQuestions(run, node);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("packId", "demo-dangshenghang-level-2");
        result.put("runId", run.getRunId());
        result.put("nodeId", node.getNodeId());
        result.put("mode", mode == null || mode.isBlank() ? "battle" : mode);
        result.put("source", "demo_fixed");
        result.put("aiReason", "演示账号固定题包：第二关每次打开均返回同一组五道选择题。");
        result.put("strategy", Map.of(
                "strategyVersion", 20260703,
                "demoUsername", "dangshenghang",
                "fixedLevel", 2,
                "targetCount", questions.size()
        ));
        result.put("questions", questions.stream().map(this::questionDto).toList());
        return result;
    }

    public boolean isPerfectSecondLevelDemo(StudentTowerRun run, StudentTowerNode node, List<Map<String, Object>> answers) {
        if (!isDemoSecondLevel(run, node) || answers == null || answers.size() != 5) return false;
        return answers.stream().allMatch(answer -> Boolean.TRUE.equals(answer.get("correct")));
    }

    public Map<String, Object> perfectSecondLevelDiagnosis(StudentTowerRun run, StudentTowerNode node,
                                                           double correctRate, List<Map<String, Object>> answers) {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("summary", "第二关 5 道选择题全部答对，说明你已经能稳定识别本关核心概念，并能把判断规则准确应用到具体题目中。");
        report.put("weaknesses", List.of());
        report.put("reviewFocus", List.of("保持对题干关键词的定位", "继续用选项排除法验证答案", "把本关规则迁移到下一关综合题"));
        report.put("source", "demo_fixed");
        report.put("fixedScenario", "dangshenghang_level_2_all_correct");
        report.put("correctRate", correctRate);
        report.put("questionCount", answers.size());
        report.put("correctCount", answers.size());
        report.put("knowledgePointId", node.getKnowledgePointId());
        report.put("abilityPointId", node.getAbilityPointId());
        report.put("runId", run.getRunId());
        report.put("nodeId", node.getNodeId());
        return report;
    }

    private List<Question> secondLevelQuestions(StudentTowerRun run, StudentTowerNode node) {
        return List.of(
                question(run, node, "demo-dangshenghang-l2-q1", "single",
                        "在 Python 中，哪一个关键字用于定义函数？",
                        List.of("A. def", "B. class", "C. import", "D. return"), "A", 1),
                question(run, node, "demo-dangshenghang-l2-q2", "single",
                        "表达式 len([3, 5, 7]) 的结果是？",
                        List.of("A. 2", "B. 3", "C. 5", "D. 7"), "B", 1),
                question(run, node, "demo-dangshenghang-l2-q3", "single",
                        "下面哪个写法可以判断变量 score 是否大于等于 60？",
                        List.of("A. score => 60", "B. score = 60", "C. score >= 60", "D. score == > 60"), "C", 2),
                question(run, node, "demo-dangshenghang-l2-q4", "single",
                        "for i in range(3) 会依次产生哪些值？",
                        List.of("A. 1, 2, 3", "B. 0, 1, 2", "C. 0, 1, 2, 3", "D. 3, 2, 1"), "B", 2),
                question(run, node, "demo-dangshenghang-l2-q5", "single",
                        "如果需要在条件不满足时执行另一段代码，应使用哪个关键字？",
                        List.of("A. while", "B. elif 或 else", "C. try", "D. pass"), "B", 2)
        );
    }

    private Question question(StudentTowerRun run, StudentTowerNode node, String id, String type, String stem,
                              List<String> options, String answer, int difficulty) {
        Question question = new Question();
        question.setQuestionId(id);
        question.setCourseCode(run.getCourseCode());
        question.setLessonNo("");
        question.setType(type);
        question.setStem(stem);
        question.setOptions(writeOptions(options));
        question.setAnswer(answer);
        question.setDifficulty(difficulty);
        question.setKnowledgePointId(node.getKnowledgePointId());
        question.setScore(20);
        return question;
    }

    private Map<String, Object> questionDto(Question question) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("questionId", question.getQuestionId());
        item.put("courseCode", question.getCourseCode());
        item.put("lessonNo", question.getLessonNo());
        item.put("type", question.getType());
        item.put("stem", question.getStem());
        item.put("options", question.getOptions());
        item.put("answer", question.getAnswer());
        item.put("difficulty", question.getDifficulty());
        item.put("knowledgePointId", question.getKnowledgePointId());
        item.put("score", question.getScore());
        return item;
    }

    private boolean isBattleRoom(String roomType) {
        return "battle".equals(roomType) || "elite".equals(roomType) || "boss".equals(roomType);
    }

    private String writeOptions(List<String> options) {
        return "[\"" + String.join("\",\"", options) + "\"]";
    }
}
