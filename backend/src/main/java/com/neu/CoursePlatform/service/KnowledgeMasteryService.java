package com.neu.CoursePlatform.service;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neu.CoursePlatform.dto.KnowledgeMasteryUpdateRequest;
import com.neu.CoursePlatform.entity.KnowledgeMastery;
import java.util.List;
public interface KnowledgeMasteryService extends IService<KnowledgeMastery> {

    KnowledgeMastery upsert(KnowledgeMasteryUpdateRequest request);

    List<KnowledgeMastery> listByStudentAndCourse(String studentNo, String courseCode);

    int removeByKnowledgePoint(String knowledgePointId);

    /** Returns a user-facing validation message, or {@code null} when valid. */
    String validateForUpsert(KnowledgeMasteryUpdateRequest request);
}
