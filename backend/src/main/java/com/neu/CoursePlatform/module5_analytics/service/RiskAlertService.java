package com.neu.CoursePlatform.module5_analytics.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.neu.CoursePlatform.module5_analytics.entity.RiskAlert;

import java.util.List;

/**
 * 风险预警 Service 接口
 */
public interface RiskAlertService extends IService<RiskAlert> {

    /**
     * 接收游戏事件/外部事件，创建预警（R8.2-R8.3）
     * 同类型活跃预警 7 天冷却期内不重复创建
     */
    RiskAlert receiveEvent(String studentId, String courseId,
                            String riskType, String riskLevel, String detail);

    /**
     * 查询学生当前风险状态（R8.5）
     * @return active 预警列表 + 最高风险等级
     */
    RiskStatus getStudentRiskStatus(String studentId);

    /**
     * 查询班级活跃预警列表（R4.5）
     */
    List<RiskAlert> getActiveByClass(String classId, List<String> studentIds);

    /**
     * 标记预警为已处理（R4.7）
     */
    boolean resolve(String alertId, String resolvedBy);

    /**
     * 检查是否存在同类型活跃预警（去重）
     */
    boolean hasActiveAlert(String studentId, String riskType);

    /**
     * 风险状态 DTO
     */
    record RiskStatus(String studentId, List<RiskAlert> activeAlerts, String highestLevel) {}
}
