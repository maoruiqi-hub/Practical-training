package com.neu.CoursePlatform.dto;
import lombok.Data;
import java.util.List;
@Data public class LectureRequest { private String resourceId; private Integer pageNumber; private String question; private List<String> previousMessages; }
