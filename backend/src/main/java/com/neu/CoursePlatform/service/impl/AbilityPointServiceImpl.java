package com.neu.CoursePlatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neu.CoursePlatform.entity.AbilityPoint;
import com.neu.CoursePlatform.mapper.AbilityPointMapper;
import com.neu.CoursePlatform.service.AbilityPointService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AbilityPointServiceImpl extends ServiceImpl<AbilityPointMapper, AbilityPoint> implements AbilityPointService {
    @Override
    public List<AbilityPoint> listByCourseCode(String courseCode) {
        return list(new LambdaQueryWrapper<AbilityPoint>().eq(AbilityPoint::getCourseCode, courseCode));
    }
}
