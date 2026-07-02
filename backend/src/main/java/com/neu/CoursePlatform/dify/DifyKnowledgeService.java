package com.neu.CoursePlatform.dify;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Dify 知识库服务 — 封装 RAG 检索逻辑。
 *
 * 课程相关的 AI 功能（讲解、答疑）通过此服务先从知识库检索相关课程资料，
 * 再将检索结果作为上下文传给 LLM，实现基于课程资料的准确回答。
 */
@Service
public class DifyKnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(DifyKnowledgeService.class);

    private final DifyClient difyClient;
    private final ObjectMapper objectMapper;

    public DifyKnowledgeService(DifyClient difyClient) {
        this.difyClient = difyClient;
        this.objectMapper = new ObjectMapper();
    }

    /** 上次检索失败的时间戳，用于退避（避免每次请求都打 401） */
    private volatile long lastRetrievalFailure = 0;
    private static final long RETRIEVAL_BACKOFF_MS = 60_000;

    /**
     * 检索与查询相关的课程资料片段。
     *
     * @param query 学生或教师的查询
     * @param topK  返回的片段数量
     * @return 相关文档片段列表
     */
    public List<KnowledgeSegment> retrieve(String query, int topK) {
        if (!difyClient.isConfigured()) {
            log.debug("Dify not configured, returning empty knowledge segments");
            return List.of();
        }

        // 退避：如果上次检索失败，60s 内不再重试
        if (System.currentTimeMillis() - lastRetrievalFailure < RETRIEVAL_BACKOFF_MS) {
            return List.of();
        }

        DifyResponse response = difyClient.retrieveKnowledge(query, topK);
        if (!response.isSuccess()) {
            lastRetrievalFailure = System.currentTimeMillis();
            log.debug("Knowledge retrieval failed (will retry after {}s): {}",
                    RETRIEVAL_BACKOFF_MS / 1000, response.getError());
            return List.of();
        }
        return parseSegments(response.getContent());
    }

    /**
     * 构建带知识库上下文的增强 prompt。
     * 将检索到的课程资料嵌入到用户问题之前，确保 LLM 回答基于课程内容。
     */
    public String buildRagPrompt(String query, String courseCode, String knowledgePoint) {
        List<KnowledgeSegment> segments = retrieve(query, 5);
        if (segments.isEmpty()) {
            return query;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("【参考课程资料】\n");
        for (int i = 0; i < segments.size(); i++) {
            KnowledgeSegment seg = segments.get(i);
            sb.append("资料").append(i + 1).append("：").append(seg.getContent());
            if (seg.getDocumentName() != null) {
                sb.append("（来源：").append(seg.getDocumentName()).append("）");
            }
            sb.append("\n");
        }
        sb.append("\n【用户问题】\n");
        sb.append(query);
        sb.append("\n\n请根据以上课程资料回答用户问题。如果资料中没有相关信息，请如实告知。");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private List<KnowledgeSegment> parseSegments(String json) {
        try {
            List<Map<String, Object>> records = objectMapper.readValue(json, new TypeReference<>() {});
            List<KnowledgeSegment> segments = new ArrayList<>();
            for (Map<String, Object> record : records) {
                KnowledgeSegment seg = new KnowledgeSegment();
                Object content = record.get("content");
                if (content != null) seg.setContent(content.toString());
                Object doc = record.get("document_name");
                if (doc != null) seg.setDocumentName(doc.toString());
                Object score = record.get("score");
                if (score instanceof Number) seg.setScore(((Number) score).doubleValue());
                segments.add(seg);
            }
            return segments;
        } catch (Exception e) {
            log.debug("Failed to parse knowledge segments: {}", e.getMessage());
            return List.of();
        }
    }

    /** 知识片段 */
    public static class KnowledgeSegment {
        private String content;
        private String documentName;
        private double score;

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getDocumentName() { return documentName; }
        public void setDocumentName(String documentName) { this.documentName = documentName; }
        public double getScore() { return score; }
        public void setScore(double score) { this.score = score; }
    }
}
