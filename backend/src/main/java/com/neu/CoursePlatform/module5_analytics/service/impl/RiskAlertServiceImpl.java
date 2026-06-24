package com.neu.CoursePlatform.module5_analytics.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neu.CoursePlatform.module5_analytics.entity.RiskAlert;
import com.neu.CoursePlatform.module5_analytics.mapper.RiskAlertMapper;
import com.neu.CoursePlatform.module5_analytics.service.RiskAlertService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RiskAlertServiceImpl
        extends ServiceImpl<RiskAlertMapper, RiskAlert>
        implements RiskAlertService {

    /** 冷却期天数（同类型预警重新触发的间隔） */
    private static final int COOLDOWN_DAYS = 7;

    @Override
    public RiskAlert receiveEvent(String studentId, String courseId,
                                   String riskType, String riskLevel, String detail) {
        // 检查同类型活跃预警：如果存在 active 状态的，不重复创建
        if (hasActiveAlert(studentId, riskType)) {
            return null;
        }

        // 检查冷却期：7 天内已处理的同类型预警不重复触发
        LambdaQueryWrapper<RiskAlert> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RiskAlert::getStudentId, studentId)
               .eq(RiskAlert::getRiskType, riskType)
               .eq(RiskAlert::getStatus, "resolved")
               .orderByDesc(RiskAlert::getResolvedAt)
               .last("LIMIT 1");
        RiskAlert lastResolved = getOne(wrapper, false);
        if (lastResolved != null && lastResolved.getResolvedAt() != null) {
            if (lastResolved.getResolvedAt().plusDays(COOLDOWN_DAYS).isAfter(LocalDateTime.now())) {
                return null; // 冷却期内，不创建新预警
            }
        }

        RiskAlert alert = RiskAlert.create(studentId, courseId, riskType, riskLevel, detail);
        save(alert);
        return alert;
    }

    @Override
    public RiskStatus getStudentRiskStatus(String studentId) {
        List<RiskAlert> activeAlerts = baseMapper.selectActiveByStudent(studentId);
        String highestLevel = activeAlerts.stream()
                .map(RiskAlert::getRiskLevel)
                .filter(l -> "high".equals(l))
                .findFirst()
                .orElse(activeAlerts.stream()
                        .map(RiskAlert::getRiskLevel)
                        .filter(l -> "medium".equals(l))
                        .findFirst()
                        .orElse(activeAlerts.isEmpty() ? "none" : "low"));
        return new RiskStatus(studentId, activeAlerts, highestLevel);
    }

    @Override
    public List<RiskAlert> getActiveByClass(String classId, List<String> studentIds) {
        return baseMapper.selectActiveByClass(classId, studentIds);
    }

    @Override
    public boolean resolve(String alertId, String resolvedBy) {
        RiskAlert alert = getById(alertId);
        if (alert == null || !"active".equals(alert.getStatus())) {
            return false;
        }
        alert.setStatus("resolved");
        alert.setResolvedAt(LocalDateTime.now());
        alert.setResolvedBy(resolvedBy);
        return updateById(alert);
    }

    @Override
    public boolean hasActiveAlert(String studentId, String riskType) {
        return baseMapper.countActiveByType(studentId, riskType) > 0;
    }
}
