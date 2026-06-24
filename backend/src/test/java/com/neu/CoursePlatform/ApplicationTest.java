package com.neu.CoursePlatform;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 基础测试类 - 验证 Spring Boot 上下文加载
 */
@SpringBootTest
@ActiveProfiles("test")
class ApplicationTest {

    @Test
    void contextLoads() {
        // 验证 Spring 上下文正常加载
    }
}
