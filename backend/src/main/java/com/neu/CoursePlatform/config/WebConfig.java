package com.neu.CoursePlatform.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        // 静态资源映射：数据库存的是相对路径如 TaskResource/xxx.doc
        String base = "file:///C:/Users/宋芷萱/Desktop/实训/Practical-training/resource/";
        registry.addResourceHandler("/resource/**").addResourceLocations(base);
        registry.addResourceHandler("/TaskResource/**").addResourceLocations(base + "TaskResource/");
        registry.addResourceHandler("/HomeworkUpload/**").addResourceLocations(base + "HomeworkUpload/");
        registry.addResourceHandler("/LessonResource/**").addResourceLocations(base + "LessonResource/");
        registry.addResourceHandler("/CourseResource/**").addResourceLocations(base + "CourseResource/");
    }
}
