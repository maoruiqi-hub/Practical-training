package com.neu.CoursePlatform.agentic;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.Map;
@Data @AllArgsConstructor public class AgenticResponse { private boolean success; private Map<String,Object> data; private String message; public static AgenticResponse unavailable(){return new AgenticResponse(false,Map.of(),"Agentic 服务不可用");} }
