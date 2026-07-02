package com.neu.CoursePlatform.module5_analytics.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 学习风险预警实体
 */
@Data
@NoArgsConstructor
@TableName("analytics_risk_alert")
public class RiskAlert {

    /** 预警ID（UUID v4） */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** 学生ID（→ 模块4.Student） */
    private String studentId;

    /** 课程ID（→ 模块1.Course） */
    private String courseId;

    /** 风险类型：procrastination / low_score / score_decline / inactive / progress_lag / hp_critical / stuck */
    private String riskType;

    /** 风险等级：high / medium / low */
    private String riskLevel;

    /** 详细信息（JSON：命中规则、关键数据等） */
    private String detail;

    /** 状态：active / resolved */
    private String status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 处理时间 */
    private LocalDateTime resolvedAt;

    /** 处理人ID */
    private String resolvedBy;

    /**
     * 创建一条预警记录（便捷方法）
     */
    public static RiskAlert create(String studentId, String courseId,
                                    String riskType, String riskLevel, String detail) {
        RiskAlert alert = new RiskAlert();
        alert.setStudentId(studentId);
        alert.setCourseId(courseId);
        alert.setRiskType(riskType);
        alert.setRiskLevel(riskLevel);
        alert.setDetail(detail);
        alert.setStatus("active");
        alert.setCreatedAt(LocalDateTime.now());
        return alert;
    }
}
