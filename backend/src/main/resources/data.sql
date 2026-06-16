-- ============ 教师 ============
INSERT IGNORE INTO teacher (teacher_no, name, college, major, phone, role, username, password) VALUES
(1, '管理员', '教务处', '教育技术', '13800000000', 'admin', 'admin', 'admin123'),
(2, '李明', '计算机学院', '软件工程', '13800000001', 'teacher', 'liming', '123456');

-- ============ 学生 ============
INSERT IGNORE INTO student (student_no, name, college, class_name, course_grades, username, password) VALUES
(1, '张三', '计算机学院', '计科202班', NULL, 'zhangsan', '123456'),
(2, '李四', '计算机学院', '计科201班', NULL, 'lisi', '123456');

-- ============ 课程 ============
INSERT IGNORE INTO course (course_code, course_name, teacher, credits, hours, cover_url) VALUES
(1, 'Python程序设计', '李明', 4, 64, NULL);

-- ============ 课时（Python 课程） ============
INSERT IGNORE INTO lesson (lesson_no, course_code, lesson_title, resource_type, resource_url, description) VALUES
(1, 1, 'Python简介与环境搭建', 'video', '/videos/python-intro.mp4', 'Python语言发展历史、特点及开发环境安装配置'),
(2, 1, '基本数据类型与运算符', 'video', '/videos/python-basics.mp4', '数字、字符串、布尔类型及常用运算符的使用'),
(3, 1, '列表、元组与字典', 'ppt', '/slides/python-collections.pptx', 'Python三大常用数据结构的定义与操作'),
(4, 1, '条件判断与循环', 'video', '/videos/python-control.mp4', 'if-elif-else条件判断、for/while循环控制'),
(5, 1, '函数定义与调用', 'video', '/videos/python-function.mp4', '函数定义、参数传递、返回值及lambda表达式'),
(6, 1, '文件操作与异常处理', 'doc', '/docs/python-file-exception.docx', '文件读写操作、with语句、try-except异常处理机制');

-- ============ 学习任务 ============
INSERT IGNORE INTO learning_task (task_no, course_code, task_type, description, deadline, submit_method, score) VALUES
(1, 1, '编程作业', '编写一个学生成绩管理系统，支持添加、查询、修改、删除成绩功能', '2026-07-15 23:59:59', '在线提交', 100),
(2, 1, '实验报告', '完成Python数据分析实验，使用pandas和matplotlib处理CSV数据并生成图表', '2026-07-30 23:59:59', '文档上传', 80),
(3, 1, '课堂测验', 'Python基础语法测验：数据类型、流程控制、函数定义等', '2026-06-25 23:59:59', '在线答题', 50);

-- ============ 测试数据：Lesson资源 & 作业提交 ============
INSERT IGNORE INTO lesson (lesson_no, course_code, lesson_title, resource_type, resource_url, description) VALUES
(7, 1, '图片资源测试', 'img', 'resource/LessonResource/red1.png', '测试课时资源上传');

INSERT IGNORE INTO task_submission (submission_id, task_no, student_no, content, file_path, submit_time, status, score, feedback) VALUES
(1, 1, 1, '测试提交', 'resource/HomeworkUpload/white1.png', NOW(), 'submitted', NULL, NULL),
(2, 1, 1, '完成了学生成绩管理系统，支持增删改查', NULL, '2026-06-20 15:30:00', 'graded', 85, '功能齐全，代码规范，缺少异常处理'),
(3, 2, 1, '使用pandas分析了CSV数据并生成柱状图', NULL, '2026-06-22 10:00:00', 'graded', 78, '图表清晰，分析维度可以更多'),
(4, 3, 1, '在线答题完成', NULL, '2026-06-18 09:00:00', 'graded', 92, NULL),
(5, 1, 2, '简单实现了命令行增删改查', NULL, '2026-06-21 20:00:00', 'graded', 65, '功能基本实现，缺少图形界面'),
(6, 2, 2, '完成了数据分析报告', NULL, '2026-06-23 16:00:00', 'graded', 88, '报告结构完整，分析深入'),
(7, 3, 2, '在线答题完成', NULL, '2026-06-19 11:00:00', 'graded', 75, NULL);
