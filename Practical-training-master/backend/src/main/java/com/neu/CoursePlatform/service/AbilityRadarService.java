*** Begin Patch
*** Add File: Practical-training-master/backend/src/main/java/com/neu/CoursePlatform/entity/StudentTowerQuestionPack.java
package com.neu.CoursePlatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("student_tower_question_pack")
public class StudentTowerQuestionPack {
    @TableId(value = "pack_id", type = IdType.ASSIGN_UUID)
    private String packId;
    private String runId;
    private String nodeId;
    private String studentNo;
    private String courseCode;
    private String mode;
    private String questionIdsJson;
    private String source;
    private String strategyJson;
    private String aiReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
*** Add File: Practical-training-master/backend/src/main/java/com/neu/CoursePlatform/mapper/StudentTowerQuestionPackMapper.java
package com.neu.CoursePlatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neu.CoursePlatform.entity.StudentTowerQuestionPack;

public interface StudentTowerQuestionPackMapper extends BaseMapper<StudentTowerQuestionPack> { }
*** Add File: Practical-training-master/backend/src/main/java/com/neu/CoursePlatform/service/TowerQuestionPackService.java
package com.neu.CoursePlatform.service;

import java.util.Map;

public interface TowerQuestionPackService {
    Map<String, Object> getOrCreateQuestionPack(String studentNo, String runId, String nodeId, String mode);
    Map<String, Object> regenerateQuestionPack(String studentNo, String runId, String nodeId, String mode);
}
*** Add File: Practical-training-master/backend/src/main/java/com/neu/CoursePlatform/service/AbilityRadarService.java
package com.neu.CoursePlatform.service;

import java.util.Map;

public interface AbilityRadarService {
    Map<String, Object> getAbilityRadar(String studentNo, String courseCode, String runId, String nodeId);
}
*** End Patch
