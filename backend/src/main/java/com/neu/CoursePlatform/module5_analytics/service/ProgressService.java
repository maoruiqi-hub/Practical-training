package com.neu.CoursePlatform.module5_analytics.service;

import com.neu.CoursePlatform.module5_analytics.dto.ClassProgressDTO;
import com.neu.CoursePlatform.module5_analytics.dto.TaskCompletionDTO;
import com.neu.CoursePlatform.module5_analytics.dto.external.StudentProgressDTO;
import com.neu.CoursePlatform.module5_analytics.service.external.ExternalDataProvider;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 学习进度监控服务（T5, R3.1-R3.4）
 */
@Service
public class ProgressService {

    private final ExternalDataProvider dataProvider;
    /** 进度落后阈值：低于班级平均的百分比 */
    private static final double LAG_THRESHOLD = 0.20;

    public ProgressService(@Lazy ExternalDataProvider dataProvider) {
        this.dataProvider = dataProvider;
    }

    /**
     * 班级整体学习进度（R3.1）
     */
    public ClassProgressDTO getClassProgress(String classId, String courseId) {
        List<StudentProgressDTO> all = dataProvider.getClassProgressList(classId, courseId);
        double avgRate = all.stream().mapToDouble(StudentProgressDTO::getCompletionRate).average().orElse(0);
        int totalTasks = all.isEmpty() ? 0 : all.get(0).getTotalTasks();
        double avgCompleted = all.stream().mapToInt(StudentProgressDTO::getCompletedTasks).average().orElse(0);

        // 找出进度落后学生（低于平均 20% 以上）
        List<ClassProgressDTO.LaggingStudent> lagging = new ArrayList<>();
        for (StudentProgressDTO sp : all) {
            if (sp.getCompletionRate() < avgRate - LAG_THRESHOLD) {
                ClassProgressDTO.LaggingStudent ls = new ClassProgressDTO.LaggingStudent();
                ls.setStudentId(sp.getStudentId());
                ls.setStudentName(sp.getStudentName());
                ls.setCompletionRate(round2(sp.getCompletionRate()));
                ls.setGapFromAvg(round2(avgRate - sp.getCompletionRate()));
                lagging.add(ls);
            }
        }
        lagging.sort((a, b) -> Double.compare(a.getCompletionRate(), b.getCompletionRate()));

        ClassProgressDTO dto = new ClassProgressDTO();
        dto.setAvgCompletionRate(round2(avgRate));
        dto.setTotalTasks(totalTasks);
        dto.setAvgCompletedTasks(round2(avgCompleted));
        dto.setLaggingStudents(lagging);
        return dto;
    }

    /**
     * 任务完成率详情（R3.2）
     */
    public TaskCompletionDTO getTaskCompletion(String classId, String taskId) {
        return dataProvider.getTaskCompletion(classId, taskId);
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
