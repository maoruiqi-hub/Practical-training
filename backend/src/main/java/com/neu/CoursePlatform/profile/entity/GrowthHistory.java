package com.neu.CoursePlatform.profile.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Date;

@Data
@NoArgsConstructor
public class GrowthHistory {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer studentNo;
    private Integer courseCode;
    private Integer amount;
    private String type;
    private String source;
    private String sourceId;
    private Date createdAt;
}
