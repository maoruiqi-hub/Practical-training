package com.neu.CoursePlatform.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Value("${course-platform.resource-root:../resource}")
    private String resourceRoot;

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        String base = Path.of(resourceRoot).toAbsolutePath().normalize().toUri().toString();
        if (!base.endsWith("/")) {
            base += "/";
        }
        registry.addResourceHandler("/resource/**").addResourceLocations(base);
        registry.addResourceHandler("/TaskResource/**").addResourceLocations(base + "TaskResource/");
        registry.addResourceHandler("/HomeworkUpload/**").addResourceLocations(base + "HomeworkUpload/");
        registry.addResourceHandler("/LessonResource/**").addResourceLocations(base + "LessonResource/");
        registry.addResourceHandler("/CourseResource/**").addResourceLocations(base + "CourseResource/");
    }
}
