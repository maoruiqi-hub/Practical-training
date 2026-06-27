package com.neu.CoursePlatform.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class ApiContractMappingTest {

    @Autowired
    private RequestMappingHandlerMapping mappings;

    @Test
    void courseContentModuleExposesDesignContractRoutes() {
        assertMapping(RequestMethod.GET, "/api/courses/{courseCode}");
        assertMapping(RequestMethod.GET, "/api/courses/{courseCode}/structure");
        assertMapping(RequestMethod.GET, "/api/knowledge-points");
        assertMapping(RequestMethod.GET, "/api/knowledge-graph");
        assertMapping(RequestMethod.GET, "/api/resources/{resourceId}/preview");
        assertMapping(RequestMethod.POST, "/api/courses/{courseCode}/resources");
        assertMapping(RequestMethod.POST, "/api/courses/{courseCode}/knowledge-points/extract");
        assertMapping(RequestMethod.POST, "/api/knowledge-points/{knowledgePointId}/explain");
        assertMapping(RequestMethod.GET, "/api/courses/{courseCode}/weak-points");
    }

    @Test
    void taskProcessModuleExposesDesignContractRoutes() {
        assertMapping(RequestMethod.GET, "/api/tasks");
        assertMapping(RequestMethod.GET, "/api/tasks/{taskNo}");
        assertMapping(RequestMethod.POST, "/api/tasks");
        assertMapping(RequestMethod.PUT, "/api/tasks/{taskNo}");
        assertMapping(RequestMethod.DELETE, "/api/tasks/{taskNo}");
        assertMapping(RequestMethod.POST, "/api/tasks/{taskNo}/submit");
        assertMapping(RequestMethod.GET, "/api/tasks/{taskNo}/submissions");
        assertMapping(RequestMethod.GET, "/api/students/{studentNo}/submissions");
        assertMapping(RequestMethod.GET, "/api/students/{studentNo}/progress");
        assertMapping(RequestMethod.POST, "/api/learning-logs");
    }

    @Test
    void assessmentModuleExposesDesignContractRoutes() {
        assertMapping(RequestMethod.GET, "/api/questions");
        assertMapping(RequestMethod.POST, "/api/questions");
        assertMapping(RequestMethod.PUT, "/api/questions/{questionId}");
        assertMapping(RequestMethod.DELETE, "/api/questions/{questionId}");
        assertMapping(RequestMethod.POST, "/api/questions/{questionId}/link-kp");
        assertMapping(RequestMethod.GET, "/api/students/{studentNo}/scores");
        assertMapping(RequestMethod.GET, "/api/students/{studentNo}/mistakes");
        assertMapping(RequestMethod.GET, "/api/courses/{courseCode}/mistake-stats");
        assertMapping(RequestMethod.GET, "/api/exams");
        assertMapping(RequestMethod.GET, "/api/exams/{examId}");
    }

    @Test
    void profileModuleExposesDesignContractRoutes() {
        assertMapping(RequestMethod.GET, "/api/students/{studentId}/profile");
        assertMapping(RequestMethod.GET, "/api/students/{studentId}/competency");
        assertMapping(RequestMethod.GET, "/api/students/{studentId}/recommendations");
        assertMapping(RequestMethod.POST, "/api/students/{studentId}/profile/generate");
        assertMapping(RequestMethod.POST, "/api/students/{studentId}/competency/update");
        assertMapping(RequestMethod.POST, "/api/students/{studentId}/recommendations/generate");
        assertMapping(RequestMethod.POST, "/api/students/{studentId}/growth/add");
        assertMapping(RequestMethod.POST, "/api/students/{studentId}/achievements");
        assertMapping(RequestMethod.GET, "/api/students/{studentId}/tower-map");
        assertMapping(RequestMethod.GET, "/api/leaderboard");
        assertMapping(RequestMethod.GET, "/api/profile/{studentNo}/{courseCode}");
    }

    @Test
    void analyticsModuleExposesDesignContractRoutes() {
        assertMapping(RequestMethod.POST, "/api/classes");
        assertMapping(RequestMethod.GET, "/api/classes");
        assertMapping(RequestMethod.GET, "/api/classes/{id}");
        assertMapping(RequestMethod.GET, "/api/classes/{classId}/scores");
        assertMapping(RequestMethod.GET, "/api/classes/{classId}/score-trends");
        assertMapping(RequestMethod.GET, "/api/classes/{classId}/weak-points");
        assertMapping(RequestMethod.GET, "/api/classes/{id}/risk-alerts");
        assertMapping(RequestMethod.POST, "/api/classes/{id}/risk-detect");
        assertMapping(RequestMethod.GET, "/api/students/{id}/risk-status");
        assertMapping(RequestMethod.POST, "/api/students/{studentId}/intervention");
    }

    private void assertMapping(RequestMethod method, String path) {
        boolean found = mappings.getHandlerMethods().keySet().stream()
                .anyMatch(info -> hasMethod(info, method) && patterns(info).contains(path));
        assertTrue(found, () -> method + " " + path + " is not mapped");
    }

    private boolean hasMethod(RequestMappingInfo info, RequestMethod method) {
        Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
        return methods.isEmpty() || methods.contains(method);
    }

    private Set<String> patterns(RequestMappingInfo info) {
        if (info.getPathPatternsCondition() != null) {
            return info.getPathPatternsCondition().getPatternValues();
        }
        return info.getPatternsCondition().getPatterns();
    }
}
