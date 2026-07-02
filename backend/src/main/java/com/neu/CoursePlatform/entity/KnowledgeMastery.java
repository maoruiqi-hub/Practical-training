package com.neu.CoursePlatform.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import java.time.LocalDateTime;
@Data public class KnowledgeMastery { @TableId(value="mastery_id",type=IdType.ASSIGN_UUID) private String masteryId; private String studentNo; private String courseCode; private String knowledgePointId; private Integer masteryScore; private String sourceType; private String sourceId; private LocalDateTime updatedAt; }
