package com.neu.CoursePlatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neu.CoursePlatform.entity.LearningAnswerEvidence;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

public interface LearningAnswerEvidenceMapper extends BaseMapper<LearningAnswerEvidence> {
    @Insert("""
            INSERT INTO learning_answer_evidence (
                evidence_id, student_no, course_code, question_id, knowledge_point_id,
                difficulty, attempt_no, first_attempt, correct, answer_content,
                source_type, source_id, idempotency_key, formula_version, answered_at
            ) VALUES (
                #{item.evidenceId}, #{item.studentNo}, #{item.courseCode}, #{item.questionId},
                #{item.knowledgePointId}, #{item.difficulty}, #{item.attemptNo},
                #{item.firstAttempt}, #{item.correct}, #{item.answerContent},
                #{item.sourceType}, #{item.sourceId}, #{item.idempotencyKey},
                #{item.formulaVersion}, #{item.answeredAt}
            )
            ON CONFLICT (student_no, idempotency_key) DO NOTHING
            """)
    int insertIfAbsent(@Param("item") LearningAnswerEvidence item);
}
