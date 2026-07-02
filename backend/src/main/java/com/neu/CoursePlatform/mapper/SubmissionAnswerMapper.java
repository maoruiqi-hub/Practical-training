package com.neu.CoursePlatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neu.CoursePlatform.entity.SubmissionAnswer;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 在线测验逐题作答明细 Mapper
 */
public interface SubmissionAnswerMapper extends BaseMapper<SubmissionAnswer> {

    List<SubmissionAnswer> selectBySubmissionId(@Param("submissionId") String submissionId);

    List<SubmissionAnswer> selectByStudentNo(@Param("studentNo") String studentNo,
                                             @Param("taskNo") String taskNo,
                                             @Param("knowledgePointId") String knowledgePointId,
                                             @Param("type") String type);

    List<SubmissionAnswer> selectWrongByStudentNo(@Param("studentNo") String studentNo);

    List<SubmissionAnswer> selectByTaskNo(@Param("taskNo") String taskNo);
}
