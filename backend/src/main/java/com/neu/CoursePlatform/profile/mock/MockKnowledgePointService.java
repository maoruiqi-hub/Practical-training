package com.neu.CoursePlatform.profile.mock;

import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import java.util.*;

@Service
@Profile("mock")
public class MockKnowledgePointService {

    /** Mock 课程能力点列表（对齐模块1 getAbilityMap 契约） */
    public List<Map<String, Object>> getAbilityMap(Integer courseCode) {
        return List.of(
            Map.of("id", "AP01", "name", "基础语法", "description", "变量、数据类型、运算符", "level", 1),
            Map.of("id", "AP02", "name", "控制流程", "description", "条件判断、循环结构", "level", 1),
            Map.of("id", "AP03", "name", "函数定义", "description", "def、参数、返回值", "level", 2),
            Map.of("id", "AP04", "name", "数据结构", "description", "列表、字典、集合、元组", "level", 2),
            Map.of("id", "AP05", "name", "文件操作", "description", "文件读写、CSV/JSON处理", "level", 3),
            Map.of("id", "AP06", "name", "数据分析", "description", "NumPy、Pandas数据处理", "level", 3),
            Map.of("id", "AP07", "name", "可视化", "description", "Matplotlib图表绘制", "level", 4)
        );
    }

    /** Mock 课程知识点列表（对齐模块1 getKnowledgePointsByCourse 契约） */
    public List<Map<String, Object>> getKnowledgePointsByCourse(Integer courseCode) {
        return List.of(
            Map.of("id", "KP01", "name", "变量与类型", "level", 1),
            Map.of("id", "KP02", "name", "条件判断", "level", 1),
            Map.of("id", "KP03", "name", "循环结构", "level", 2),
            Map.of("id", "KP04", "name", "函数定义", "level", 2),
            Map.of("id", "KP05", "name", "列表与字典", "level", 3),
            Map.of("id", "KP06", "name", "文件读写", "level", 3),
            Map.of("id", "KP07", "name", "NumPy数组", "level", 4),
            Map.of("id", "KP08", "name", "Pandas DataFrame", "level", 4),
            Map.of("id", "KP09", "name", "数据清洗", "level", 5),
            Map.of("id", "KP10", "name", "分组聚合", "level", 5),
            Map.of("id", "KP11", "name", "Matplotlib绘图", "level", 5),
            Map.of("id", "KP12", "name", "统计分析", "level", 6),
            Map.of("id", "KP13", "name", "综合项目", "level", 6)
        );
    }
}
