-- Foundational demo data. Every statement is idempotent so it may safely run again.

INSERT INTO teacher (teacher_no, name, college, major, phone, role, username, password)
SELECT '1', '教务管理员', '教务处', '教育技术', '13800000000', 'admin', 'admin', '$2y$10$vw9EBac9jqqmeeVTIXcJpOVyf3BdxKZyvjTl03cyGJr01HQd/oYwG'
WHERE NOT EXISTS (SELECT 1 FROM teacher WHERE teacher_no = '1');

INSERT INTO teacher (teacher_no, name, college, major, phone, role, username, password)
SELECT '2', '李明', '软件学院', '软件工程', '13800000001', 'teacher', 'liming', '$2y$10$g.6iH4XGxY0ajdQsi6nxNOxd/Yej86soWE.GpXeRGdPFiEDnVPBuO'
WHERE NOT EXISTS (SELECT 1 FROM teacher WHERE teacher_no = '2');

INSERT INTO student (student_no, name, college, class_name, course_grades, username, password, phone)
SELECT '1', '张三', '软件学院', '软件工程2班', '{}', 'zhangsan', '$2y$10$g.6iH4XGxY0ajdQsi6nxNOxd/Yej86soWE.GpXeRGdPFiEDnVPBuO', '13821010299'
WHERE NOT EXISTS (SELECT 1 FROM student WHERE student_no = '1');

INSERT INTO student (student_no, name, college, class_name, course_grades, username, password, phone)
SELECT v.student_no, v.name, '软件学院', v.class_name, '{}', v.username, '$2y$10$g.6iH4XGxY0ajdQsi6nxNOxd/Yej86soWE.GpXeRGdPFiEDnVPBuO', v.phone
FROM (VALUES
    ('2', '徐清源', '软件工程1班', 'xuqingyuan', '13821010302'),
    ('3', '沈佳怡', '软件工程1班', 'shenjiayi', '13821010303'),
    ('4', '俞景皓', '软件工程1班', 'yujinghao', '13821010304'),
    ('5', '秦瑶', '软件工程2班', 'qinyao', '13821010305'),
    ('6', '罗明泽', '软件工程2班', 'luomingze', '13821010306'),
    ('7', '林亦辰', '软件工程1班', 'linyichen', '13821010101'),
    ('8', '陈若曦', '软件工程1班', 'chenruoxi', '13821010102'),
    ('9', '周子墨', '软件工程1班', 'zhouzimo', '13821010103'),
    ('10', '刘雨桐', '软件工程1班', 'liuyutong', '13821010104'),
    ('11', '赵明轩', '软件工程1班', 'zhaomingxuan', '13821010105'),
    ('12', '王思涵', '软件工程1班', 'wangsihan', '13821010106'),
    ('13', '孙嘉宁', '软件工程1班', 'sunjianing', '13821010107'),
    ('14', '高梓豪', '软件工程1班', 'gaozihang', '13821010108'),
    ('15', '何沐阳', '软件工程1班', 'hemuyang', '13821010109'),
    ('16', '郭雨晴', '软件工程1班', 'guoyuqing', '13821010110'),
    ('17', '郑浩然', '软件工程1班', 'zhenghaoran', '13821010111'),
    ('18', '唐诗语', '软件工程1班', 'tangshiyu', '13821010112'),
    ('19', '李思远', '软件工程2班', 'lisiyuan', '13821010201'),
    ('20', '宋佳琪', '软件工程2班', 'songjiaqi', '13821010202'),
    ('21', '许文博', '软件工程2班', 'xuwenbo', '13821010203'),
    ('22', '韩雨泽', '软件工程2班', 'hanyuze', '13821010204'),
    ('23', '马欣怡', '软件工程2班', 'maxinyi', '13821010205'),
    ('24', '朱昊天', '软件工程2班', 'zhuhaotian', '13821010206'),
    ('25', '梁可欣', '软件工程2班', 'liangkexin', '13821010207'),
    ('26', '罗俊熙', '软件工程2班', 'luojunxi', '13821010208'),
    ('27', '谢安然', '软件工程2班', 'xieanran', '13821010209'),
    ('28', '曹逸凡', '软件工程2班', 'caoyifan', '13821010210'),
    ('29', '邓梓萱', '软件工程2班', 'dengzixuan', '13821010211'),
    ('30', '彭一诺', '软件工程2班', 'pengyinuo', '13821010212')
) AS v(student_no, name, class_name, username, phone)
WHERE NOT EXISTS (SELECT 1 FROM student s WHERE s.student_no = v.student_no);

INSERT INTO course (course_code, course_name, teacher, teacher_no, credits, hours, cover_url, description)
SELECT '1', 'Python 程序设计', '李明', '2', 4, 64, NULL, '面向软件工程专业的 Python 编程基础课程。'
WHERE NOT EXISTS (SELECT 1 FROM course WHERE course_code = '1');

INSERT INTO lesson (lesson_no, course_code, lesson_title, resource_type, resource_url, description)
SELECT v.lesson_no, '1', v.lesson_title, 'video', v.resource_url, v.description
FROM (VALUES
    ('1', 'Python 课程导论', '/LessonResource/python/lesson_01_python_intro_voice.mp4', '认识 Python 语言特点、开发环境与学习路径。'),
    ('2', 'Python 基础语法与数据类型', '/LessonResource/python/lesson_02_basic_syntax_voice.mp4', '掌握变量、缩进、注释、基础数据类型和类型转换。'),
    ('3', '运算符与表达式', '/LessonResource/python/lesson_03_operators_voice.mp4', '掌握算术、比较、逻辑等运算符及表达式优先级。'),
    ('4', '程序控制结构', '/LessonResource/python/lesson_04_control_flow_voice.mp4', '使用条件分支和循环结构组织程序流程。'),
    ('5', '列表与元组', '/LessonResource/python/lesson_05_list_tuple_voice.mp4', '使用列表、元组、切片和常用容器操作。'),
    ('6', '字典与集合', '/LessonResource/python/lesson_06_dict_set_voice.mp4', '使用字典和集合完成键值处理与去重运算。'),
    ('7', '字符串处理', '/LessonResource/python/lesson_07_string_processing_voice.mp4', '掌握字符串切片、格式化、常用方法和基础正则。'),
    ('8', '函数定义与调用', '/LessonResource/python/lesson_08_functions_voice.mp4', '定义函数、设计参数与返回值，并理解作用域。'),
    ('9', '模块与包', '/LessonResource/python/lesson_09_modules_voice.mp4', '使用模块、包和常用标准库组织代码。'),
    ('10', '文件读写', '/LessonResource/python/lesson_10_file_io_voice.mp4', '读写文本、CSV 和 JSON 等常见文件。'),
    ('11', '异常处理', '/LessonResource/python/lesson_11_exceptions_voice.mp4', '通过异常捕获和自定义异常提升程序健壮性。'),
    ('12', '面向对象基础', '/LessonResource/python/lesson_12_oop_basic_voice.mp4', '理解类、对象、属性和方法的基本建模方式。'),
    ('13', '面向对象进阶', '/LessonResource/python/lesson_13_oop_advanced_voice.mp4', '掌握继承、多态和常用高级面向对象特性。'),
    ('14', '数据分析基础', '/LessonResource/python/lesson_14_data_analysis_voice.mp4', '使用 Python 完成基础数据处理与分析。'),
    ('15', 'Web 开发入门', '/LessonResource/python/lesson_15_web_intro_voice.mp4', '认识 Web 请求、路由和基础 Web 应用开发。'),
    ('16', '网络爬虫基础', '/LessonResource/python/lesson_16_crawler_voice.mp4', '掌握网页请求、解析和合规采集的基本方法。'),
    ('17', 'Python 项目综合实践', '/LessonResource/python/lesson_17_project_practice_voice.mp4', '综合运用所学知识完成一个小型 Python 项目。')
) AS v(lesson_no, lesson_title, resource_url, description)
WHERE NOT EXISTS (SELECT 1 FROM lesson l WHERE l.lesson_no = v.lesson_no);

INSERT INTO course_resource (course_code, title, resource_type, file_url, preview_file_url, preview_status,
                             original_filename, chapter, uploaded_by, uploaded_at)
SELECT '1', '第' || v.lesson_no || '讲 ' || v.lesson_title, 'video', v.resource_url, v.preview_url, 'ready',
       v.filename, '第' || v.lesson_no || '讲', '2', CURRENT_TIMESTAMP
FROM (VALUES
    ('1', 'Python 课程导论', '/LessonResource/python/lesson_01_python_intro_voice.mp4', '/LessonResource/python/lesson_01_python_intro_cover.png', 'lesson_01_python_intro_voice.mp4'),
    ('2', 'Python 基础语法与数据类型', '/LessonResource/python/lesson_02_basic_syntax_voice.mp4', '/LessonResource/python/lesson_02_basic_syntax_cover.png', 'lesson_02_basic_syntax_voice.mp4'),
    ('3', '运算符与表达式', '/LessonResource/python/lesson_03_operators_voice.mp4', '/LessonResource/python/lesson_03_operators_cover.png', 'lesson_03_operators_voice.mp4'),
    ('4', '程序控制结构', '/LessonResource/python/lesson_04_control_flow_voice.mp4', '/LessonResource/python/lesson_04_control_flow_cover.png', 'lesson_04_control_flow_voice.mp4'),
    ('5', '列表与元组', '/LessonResource/python/lesson_05_list_tuple_voice.mp4', '/LessonResource/python/lesson_05_list_tuple_cover.png', 'lesson_05_list_tuple_voice.mp4'),
    ('6', '字典与集合', '/LessonResource/python/lesson_06_dict_set_voice.mp4', '/LessonResource/python/lesson_06_dict_set_cover.png', 'lesson_06_dict_set_voice.mp4'),
    ('7', '字符串处理', '/LessonResource/python/lesson_07_string_processing_voice.mp4', '/LessonResource/python/lesson_07_string_processing_cover.png', 'lesson_07_string_processing_voice.mp4'),
    ('8', '函数定义与调用', '/LessonResource/python/lesson_08_functions_voice.mp4', '/LessonResource/python/lesson_08_functions_cover.png', 'lesson_08_functions_voice.mp4'),
    ('9', '模块与包', '/LessonResource/python/lesson_09_modules_voice.mp4', '/LessonResource/python/lesson_09_modules_cover.png', 'lesson_09_modules_voice.mp4'),
    ('10', '文件读写', '/LessonResource/python/lesson_10_file_io_voice.mp4', '/LessonResource/python/lesson_10_file_io_cover.png', 'lesson_10_file_io_voice.mp4'),
    ('11', '异常处理', '/LessonResource/python/lesson_11_exceptions_voice.mp4', '/LessonResource/python/lesson_11_exceptions_cover.png', 'lesson_11_exceptions_voice.mp4'),
    ('12', '面向对象基础', '/LessonResource/python/lesson_12_oop_basic_voice.mp4', '/LessonResource/python/lesson_12_oop_basic_cover.png', 'lesson_12_oop_basic_voice.mp4'),
    ('13', '面向对象进阶', '/LessonResource/python/lesson_13_oop_advanced_voice.mp4', '/LessonResource/python/lesson_13_oop_advanced_cover.png', 'lesson_13_oop_advanced_voice.mp4'),
    ('14', '数据分析基础', '/LessonResource/python/lesson_14_data_analysis_voice.mp4', '/LessonResource/python/lesson_14_data_analysis_cover.png', 'lesson_14_data_analysis_voice.mp4'),
    ('15', 'Web 开发入门', '/LessonResource/python/lesson_15_web_intro_voice.mp4', '/LessonResource/python/lesson_15_web_intro_cover.png', 'lesson_15_web_intro_voice.mp4'),
    ('16', '网络爬虫基础', '/LessonResource/python/lesson_16_crawler_voice.mp4', '/LessonResource/python/lesson_16_crawler_cover.png', 'lesson_16_crawler_voice.mp4'),
    ('17', 'Python 项目综合实践', '/LessonResource/python/lesson_17_project_practice_voice.mp4', '/LessonResource/python/lesson_17_project_practice_cover.png', 'lesson_17_project_practice_voice.mp4')
) AS v(lesson_no, lesson_title, resource_url, preview_url, filename)
WHERE NOT EXISTS (
    SELECT 1 FROM course_resource r WHERE r.course_code = '1' AND r.file_url = v.resource_url
);

INSERT INTO analytics_class (id, name, course_id, teacher_id, semester, created_at, updated_at)
SELECT '7b8d2e3f-9f8a-4ab6-a2cc-101010101001', '软件工程1班', '1', '2', '2025-2026-2', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM analytics_class WHERE id = '7b8d2e3f-9f8a-4ab6-a2cc-101010101001');

INSERT INTO analytics_class (id, name, course_id, teacher_id, semester, created_at, updated_at)
SELECT '7b8d2e3f-9f8a-4ab6-a2cc-101010101002', '软件工程2班', '1', '2', '2025-2026-2', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM analytics_class WHERE id = '7b8d2e3f-9f8a-4ab6-a2cc-101010101002');

INSERT INTO analytics_class_student (class_id, student_id, enrolled_at)
SELECT '7b8d2e3f-9f8a-4ab6-a2cc-101010101001', s.student_no, CURRENT_TIMESTAMP
FROM student s
WHERE s.class_name = '软件工程1班'
  AND NOT EXISTS (
      SELECT 1 FROM analytics_class_student m
      WHERE m.class_id = '7b8d2e3f-9f8a-4ab6-a2cc-101010101001' AND m.student_id = s.student_no
  );

INSERT INTO analytics_class_student (class_id, student_id, enrolled_at)
SELECT '7b8d2e3f-9f8a-4ab6-a2cc-101010101002', s.student_no, CURRENT_TIMESTAMP
FROM student s
WHERE s.class_name = '软件工程2班'
  AND NOT EXISTS (
      SELECT 1 FROM analytics_class_student m
      WHERE m.class_id = '7b8d2e3f-9f8a-4ab6-a2cc-101010101002' AND m.student_id = s.student_no
  );

INSERT INTO learning_task (task_no, course_code, task_name, lesson_no, knowledge_points, task_type, description,
                           deadline, submit_method, score, grading_rule, status, allow_late, max_attempts,
                           attachment_formats, resource_url)
SELECT '1', '1', '学生成绩管理系统', '1', '["function","file"]', 'programming',
       '使用 Python 实现一个简单的学生成绩管理系统。',
       TIMESTAMP '2026-07-15 23:59:59', 'online', 100, '功能实现 40 分；代码质量 30 分；健壮性 30 分。',
       'published', 1, 3, '.zip,.py,.java', NULL
WHERE NOT EXISTS (SELECT 1 FROM learning_task WHERE task_no = '1');
