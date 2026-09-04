package com.neu.CoursePlatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neu.CoursePlatform.dto.TaskSubmissionDTO;
import com.neu.CoursePlatform.entity.TaskSubmission;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 任务提交记录 Mapper
 */
public interface TaskSubmissionMapper extends BaseMapper<TaskSubmission> {

    /** 按学生查提交记录 */
    List<TaskSubmission> selectByStudentNo(@Param("studentNo") String studentNo);

    List<TaskSubmission> selectByStudentNoAndCourse(@Param("studentNo") String studentNo,
                                                    @Param("courseCode") String courseCode);

    /** 按任务查提交记录 */
    List<TaskSubmission> selectByTaskNo(@Param("taskNo") String taskNo);

    List<TaskSubmission> selectByCourseCode(@Param("courseCode") String courseCode);

    Map<String, Object> selectTaskStats(@Param("taskNo") String taskNo);

    List<Map<String, Object>> selectCourseTaskStats(@Param("courseCode") String courseCode);

    List<TaskSubmissionDTO> selectLatestDtoByTaskNo(@Param("taskNo") String taskNo);

    int markSupersededPrevious(@Param("taskNo") String taskNo,
                               @Param("studentNo") String studentNo,
                               @Param("currentSubmissionId") String currentSubmissionId);
}
