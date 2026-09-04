package com.neu.CoursePlatform.module5_analytics.service.external;

import com.neu.CoursePlatform.module5_analytics.dto.TaskCompletionDTO;
import com.neu.CoursePlatform.module5_analytics.dto.external.KnowledgePointDTO;
import com.neu.CoursePlatform.module5_analytics.dto.external.MistakeStatsDTO;
import com.neu.CoursePlatform.module5_analytics.dto.external.StudentProgressDTO;
import com.neu.CoursePlatform.module5_analytics.dto.external.StudentScoreDTO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Mock 外部数据提供者。
 * Phase 2 期间使用，生成合理的测试数据以便独立开发。
 * Phase 3+ 当其他模块就绪后替换为真实实现。
 */
@Service
@Profile("mock")  // 只在明确的 mock 环境启用；默认使用真实数据适配器
public class MockExternalDataProvider implements ExternalDataProvider {

    private final Random rng = new Random(42); // 固定种子，可复现

    private static final String[] KP_NAMES = {
        "变量与数据类型", "条件判断", "循环结构", "函数定义",
        "列表与字典", "文件读写", "NumPy数组", "Pandas入门",
        "数据清洗", "分组聚合", "Matplotlib可视化", "基础统计"
    };

    @Override
    public List<StudentScoreDTO> getStudentScores(String studentId, String courseId) {
        List<StudentScoreDTO> scores = new ArrayList<>();
        // 生成 3~6 条模拟成绩
        int count = 3 + rng.nextInt(4);
        for (int i = 0; i < count; i++) {
            StudentScoreDTO s = new StudentScoreDTO();
            s.setStudentId(studentId != null ? studentId : "mock-student-" + (i + 1));
            s.setStudentName("学生" + (i + 1));
            s.setCourseId(courseId);
            s.setTargetId("exam-" + i);
            s.setTargetType(i % 2 == 0 ? "exam" : "task");
            s.setTotalScore(100.0);
            s.setScore(40.0 + rng.nextDouble() * 60.0);  // 40~100
            s.setScoredAt(LocalDateTime.now().minusDays(rng.nextInt(30)));
            scores.add(s);
        }
        return scores;
    }

    @Override
    public List<MistakeStatsDTO> getClassMistakeStats(String courseId) {
        List<MistakeStatsDTO> stats = new ArrayList<>();
        for (int i = 0; i < KP_NAMES.length; i++) {
            MistakeStatsDTO s = new MistakeStatsDTO();
            s.setKnowledgePointId("kp-" + (i + 1));
            s.setKnowledgePointName(KP_NAMES[i]);
            s.setTotalAttempts(50 + rng.nextInt(100));
            s.setMistakeCount(rng.nextInt(s.getTotalAttempts()));
            s.setMistakeRate((double) s.getMistakeCount() / s.getTotalAttempts());
            stats.add(s);
        }
        // 按错误率降序
        stats.sort((a, b) -> Double.compare(b.getMistakeRate(), a.getMistakeRate()));
        return stats;
    }

    @Override
    public StudentProgressDTO getStudentProgress(String studentId, String courseId) {
        StudentProgressDTO p = new StudentProgressDTO();
        p.setStudentId(studentId);
        p.setStudentName("学生");
        p.setTotalTasks(12);
        p.setCompletedTasks(3 + rng.nextInt(10));
        p.setSubmittedTasks(p.getCompletedTasks() + rng.nextInt(2));
        p.setCompletionRate((double) p.getCompletedTasks() / p.getTotalTasks());
        return p;
    }

    @Override
    public List<StudentProgressDTO> getClassProgressList(String classId, String courseId) {
        List<StudentProgressDTO> list = new ArrayList<>();
        for (String studentId : getStudentIdsByClass(classId)) {
            StudentProgressDTO p = new StudentProgressDTO();
            p.setStudentId(studentId);
            p.setStudentName(studentId);
            p.setTotalTasks(12);
            p.setCompletedTasks(rng.nextInt(13));
            p.setCompletionRate((double) p.getCompletedTasks() / p.getTotalTasks());
            list.add(p);
        }
        return list;
    }

    @Override
    public TaskCompletionDTO getTaskCompletion(String classId, String taskId) {
        List<String> studentIds = getStudentIdsByClass(classId);
        int submitted = Math.max(0, studentIds.size() - 3);
        TaskCompletionDTO dto = new TaskCompletionDTO();
        dto.setTaskId(taskId);
        dto.setTaskName("任务-" + taskId);
        dto.setTotalStudents(studentIds.size());
        dto.setSubmittedCount(submitted);
        dto.setNotSubmittedCount(studentIds.size() - submitted);
        dto.setLateSubmittedCount(Math.min(2, submitted));
        dto.setSubmissionRate(studentIds.isEmpty() ? 0D : (double) submitted / studentIds.size());
        dto.setNotSubmittedStudentIds(studentIds.subList(submitted, studentIds.size()));
        return dto;
    }

    @Override
    public List<KnowledgePointDTO> getKnowledgePointsByCourse(String courseId) {
        List<KnowledgePointDTO> list = new ArrayList<>();
        for (int i = 0; i < KP_NAMES.length; i++) {
            KnowledgePointDTO kp = new KnowledgePointDTO();
            kp.setId("kp-" + (i + 1));
            kp.setName(KP_NAMES[i]);
            kp.setCourseId(courseId);
            kp.setLevel((i / 4) + 1);
            list.add(kp);
        }
        return list;
    }

    @Override
    public List<String> getStudentIdsByClass(String classId) {
        List<String> ids = new ArrayList<>();
        for (int i = 1; i <= 15; i++) {
            ids.add("student-" + i);
        }
        return ids;
    }

    @Override
    public LocalDateTime getLastActiveTime(String studentId) {
        // 大部分学生近期活跃，少数模拟不活跃
        if (studentId.hashCode() % 7 == 0) {
            return LocalDateTime.now().minusDays(5); // 5天前 → 触发 inactive
        }
        return LocalDateTime.now().minusHours(rng.nextInt(48));
    }
}
