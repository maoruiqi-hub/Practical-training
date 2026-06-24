package com.neu.CoursePlatform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neu.CoursePlatform.entity.GradeResult;
import com.neu.CoursePlatform.mapper.GradeResultMapper;
import com.neu.CoursePlatform.service.GradeResultService;
import org.springframework.stereotype.Service;

@Service
public class GradeResultServiceImpl extends ServiceImpl<GradeResultMapper, GradeResult> implements GradeResultService {
}
