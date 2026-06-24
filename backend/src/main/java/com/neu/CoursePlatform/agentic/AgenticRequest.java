package com.neu.CoursePlatform.agentic;
import lombok.Data;
import java.util.Map;
@Data public class AgenticRequest { private String courseCode; private String resourceId; private String knowledgePointId; private String content; private Map<String,Object> context; }
