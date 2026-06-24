package com.neu.CoursePlatform.profile.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Recommendation {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer studentNo;
    private Integer courseCode;
    private String type;
    private String targetId;
    private String targetName;
    private String reason;
    private Integer priority;
    private String feedback;
    private java.util.Date createdAt;
}
