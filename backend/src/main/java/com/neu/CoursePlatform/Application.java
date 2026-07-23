package com.neu.CoursePlatform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * AI智慧课程平台 - 后端启动类
 */
@SpringBootApplication
@EnableAsync
@MapperScan({"com.neu.CoursePlatform.mapper", "com.neu.CoursePlatform.profile.mapper", "com.neu.CoursePlatform.module5_analytics.mapper"})
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
