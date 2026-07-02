package com.neu.CoursePlatform.dto;
import lombok.Data;
import java.util.List;
@Data public class CourseQaRequest { private String question; private String resourceId; private List<String> previousMessages; }
