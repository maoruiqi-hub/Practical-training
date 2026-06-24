package com.neu.CoursePlatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.neu.CoursePlatform.dto.PaperGenerateRequest;
import com.neu.CoursePlatform.dto.PaperGenerateResult;
import com.neu.CoursePlatform.entity.Paper;

public interface PaperService extends IService<Paper> {

    PaperGenerateResult generateAndSave(String courseCode, PaperGenerateRequest request);

    void bindToTask(String paperId, String taskNo);
}
