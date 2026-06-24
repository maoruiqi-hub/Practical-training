package com.neu.CoursePlatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.neu.CoursePlatform.entity.KnowledgePointFloorStatus;
import com.neu.CoursePlatform.mapper.KnowledgePointFloorStatusMapper;
import com.neu.CoursePlatform.service.KnowledgePointFloorStatusService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class KnowledgePointFloorStatusServiceImpl implements KnowledgePointFloorStatusService {

    private final KnowledgePointFloorStatusMapper statusMapper;

    public KnowledgePointFloorStatusServiceImpl(KnowledgePointFloorStatusMapper statusMapper) {
        this.statusMapper = statusMapper;
    }

    @Override
    public KnowledgePointFloorStatus updateStatus(String studentNo, String courseCode,
                                                   String knowledgePointId, String status) {
        KnowledgePointFloorStatus floorStatus = statusMapper.selectOne(
                new LambdaQueryWrapper<KnowledgePointFloorStatus>()
                        .eq(KnowledgePointFloorStatus::getStudentNo, studentNo)
                        .eq(KnowledgePointFloorStatus::getCourseCode, courseCode)
                        .eq(KnowledgePointFloorStatus::getKnowledgePointId, knowledgePointId));
        if (floorStatus == null) {
            floorStatus = new KnowledgePointFloorStatus();
            floorStatus.setStudentNo(studentNo);
            floorStatus.setCourseCode(courseCode);
            floorStatus.setKnowledgePointId(knowledgePointId);
        }
        floorStatus.setStatus(status);
        floorStatus.setUpdatedAt(LocalDateTime.now());
        if (floorStatus.getFloorStatusId() == null) statusMapper.insert(floorStatus);
        else statusMapper.updateById(floorStatus);
        return floorStatus;
    }

    @Override
    public List<KnowledgePointFloorStatus> listByStudentAndCourse(String studentNo, String courseCode) {
        return statusMapper.selectList(new LambdaQueryWrapper<KnowledgePointFloorStatus>()
                .eq(KnowledgePointFloorStatus::getStudentNo, studentNo)
                .eq(KnowledgePointFloorStatus::getCourseCode, courseCode)
                .orderByAsc(KnowledgePointFloorStatus::getKnowledgePointId));
    }
}
