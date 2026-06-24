package com.neu.CoursePlatform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neu.CoursePlatform.entity.SubmissionAnswer;
import com.neu.CoursePlatform.mapper.SubmissionAnswerMapper;
import com.neu.CoursePlatform.service.SubmissionAnswerService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SubmissionAnswerServiceImpl extends ServiceImpl<SubmissionAnswerMapper, SubmissionAnswer> implements SubmissionAnswerService {

    @Override
    public List<SubmissionAnswer> listBySubmissionId(String submissionId) {
        return baseMapper.selectBySubmissionId(submissionId);
    }

    @Override
    public List<SubmissionAnswer> listByStudentNo(String studentNo, String taskNo, String knowledgePointId, String type) {
        return baseMapper.selectByStudentNo(studentNo, taskNo, knowledgePointId, type);
    }

    @Override
    public List<SubmissionAnswer> listWrongByStudentNo(String studentNo) {
        return baseMapper.selectWrongByStudentNo(studentNo);
    }

    @Override
    public List<SubmissionAnswer> listByTaskNo(String taskNo) {
        return baseMapper.selectByTaskNo(taskNo);
    }
}
