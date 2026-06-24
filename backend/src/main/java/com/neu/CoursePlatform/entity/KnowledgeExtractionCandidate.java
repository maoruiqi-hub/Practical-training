package com.neu.CoursePlatform.entity;
import com.baomidou.mybatisplus.annotation.IdType; import com.baomidou.mybatisplus.annotation.TableId; import lombok.Data; import java.time.LocalDateTime;
@Data public class KnowledgeExtractionCandidate { @TableId(value="candidate_id",type=IdType.AUTO) private String candidateId; private String courseCode; private String resourceId; private String name; private String description; private String chapter; private Integer importance; private String status; private LocalDateTime createdAt; }
