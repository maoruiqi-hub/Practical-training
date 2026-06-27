package com.neu.CoursePlatform.module5_analytics.service.external;

import com.neu.CoursePlatform.module5_analytics.dto.external.KnowledgePointDTO;
import com.neu.CoursePlatform.module5_analytics.dto.external.MistakeStatsDTO;
import com.neu.CoursePlatform.module5_analytics.dto.external.StudentProgressDTO;
import com.neu.CoursePlatform.module5_analytics.dto.external.StudentScoreDTO;
import com.neu.CoursePlatform.module5_analytics.dto.TaskCompletionDTO;

import java.util.List;

/**
 * 外部模块数据提供者接口。
 * Phase 2 使用 Mock 实现，Phase 3+ 切换为真实模块调用。
 */
public interface ExternalDataProvider {

    /** 模块3：获取学生成绩列表 */
    List<StudentScoreDTO> getStudentScores(String studentId, String courseId);

    /** 模块3：获取班级错题统计 */
    List<MistakeStatsDTO> getClassMistakeStats(String courseId);

    /** 模块2：获取学生学习进度 */
    StudentProgressDTO getStudentProgress(String studentId, String courseId);

    /** 模块2：获取班级任务完成统计 */
    List<StudentProgressDTO> getClassProgressList(String classId, String courseId);

    /** 模块2：获取班级内某任务完成统计 */
    TaskCompletionDTO getTaskCompletion(String classId, String taskId);

    /** 模块1：获取课程知识点列表 */
    List<KnowledgePointDTO> getKnowledgePointsByCourse(String courseId);

    /** 模块4：获取班级学生ID列表 */
    List<String> getStudentIdsByClass(String classId);

    /** 模块2：获取学生学习日志（最近活跃时间） */
    java.time.LocalDateTime getLastActiveTime(String studentId);
}
