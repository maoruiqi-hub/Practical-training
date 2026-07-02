package com.neu.CoursePlatform.service;
import com.neu.CoursePlatform.dto.WeakKnowledgePointDTO;
import java.util.List;
public interface WeakPointService { List<WeakKnowledgePointDTO> listByCourseCode(String courseCode); }
