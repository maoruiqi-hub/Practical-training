-- ============ 教师 ============
INSERT IGNORE INTO teacher (teacher_no, name, college, major, phone, role, username, password) VALUES
(1, '管理员', '教务处', '教育技术', '13800000000', 'admin', 'admin', 'admin123'),
(2, '李明', '计算机学院', '软件工程', '13800000001', 'teacher', 'liming', '123456');

-- ============ 学生 ============
INSERT IGNORE INTO student (student_no, name, college, class_name, course_grades, username, password) VALUES
(1, '张三', '计算机学院', '计科202班', NULL, 'zhangsan', '123456'),
(2, '李四', '计算机学院', '计科201班', NULL, 'lisi', '123456');

-- ============ 课程 ============
INSERT IGNORE INTO course (course_code, course_name, teacher, teacher_no, credits, hours, cover_url) VALUES
(1, 'Python程序设计', '李明', 2, 4, 64, NULL);

-- ============ 课时（Python 课程） ============
INSERT IGNORE INTO lesson (lesson_no, course_code, lesson_title, resource_type, resource_url, description) VALUES
(1, 1, 'Python简介与环境搭建', 'video', '/videos/python-intro.mp4', 'Python语言发展历史、特点及开发环境安装配置'),
(2, 1, '基本数据类型与运算符', 'video', '/videos/python-basics.mp4', '数字、字符串、布尔类型及常用运算符的使用'),
(3, 1, '列表、元组与字典', 'ppt', '/slides/python-collections.pptx', 'Python三大常用数据结构的定义与操作'),
(4, 1, '条件判断与循环', 'video', '/videos/python-control.mp4', 'if-elif-else条件判断、for/while循环控制'),
(5, 1, '函数定义与调用', 'video', '/videos/python-function.mp4', '函数定义、参数传递、返回值及lambda表达式'),
(6, 1, '文件操作与异常处理', 'doc', '/docs/python-file-exception.docx', '文件读写操作、with语句、try-except异常处理机制');

-- ============ 学习任务 ============
INSERT IGNORE INTO learning_task (task_no, course_code, task_name, lesson_no, knowledge_points, task_type, description, deadline, submit_method, score, grading_rule, status, allow_late, max_attempts, attachment_formats, resource_url) VALUES
(1, 1, '学生成绩管理系统', 5, '["函数定义","文件操作"]', '编程作业', '编写一个学生成绩管理系统，支持添加、查询、修改、删除成绩功能', '2026-07-15 23:59:59', '在线提交', 100, '功能完整性40分+代码规范30分+异常处理30分', 'published', 1, 3, '.zip,.py,.java', NULL),
(2, 1, 'Python数据分析实验报告', 4, '["pandas","matplotlib","数据可视化"]', '实验报告', '完成Python数据分析实验，使用pandas和matplotlib处理CSV数据并生成图表', '2026-07-30 23:59:59', '文档上传', 80, '数据预处理30分+图表质量30分+分析结论40分', 'published', 0, 1, '.pdf,.doc,.docx', NULL),
(3, 1, 'Python基础语法测验', 2, '["基本数据类型","流程控制","函数定义"]', '课堂测验', 'Python基础语法测验：数据类型、流程控制、函数定义等', '2026-06-25 23:59:59', '在线答题', 50, '单选题10分/题+多选题15分/题+填空题10分/题+简答题25分', 'published', 0, 1, '', NULL);

-- ============ 测试数据：Lesson资源 & 作业提交 ============
INSERT IGNORE INTO lesson (lesson_no, course_code, lesson_title, resource_type, resource_url, description) VALUES
(7, 1, '图片资源测试', 'img', 'resource/LessonResource/red1.png', '测试课时资源上传');

INSERT IGNORE INTO task_submission (submission_id, task_no, student_no, attempt_number, content, file_path, submit_time, is_overdue, status, score, feedback) VALUES
(1, 1, 1, 1, '测试提交', 'resource/HomeworkUpload/white1.png', NOW(), 0, 'submitted', NULL, NULL),
(2, 1, 1, 2, '完成了学生成绩管理系统，支持增删改查', NULL, '2026-06-20 15:30:00', 0, 'graded', 85, '功能齐全，代码规范，缺少异常处理'),
(3, 2, 1, 1, '使用pandas分析了CSV数据并生成柱状图', NULL, '2026-06-22 10:00:00', 0, 'graded', 78, '图表清晰，分析维度可以更多'),
(4, 3, 1, 1, '在线答题完成', NULL, '2026-06-18 09:00:00', 0, 'graded', 92, NULL),
(5, 1, 2, 1, '简单实现了命令行增删改查', NULL, '2026-06-21 20:00:00', 0, 'graded', 65, '功能基本实现，缺少图形界面'),
(6, 2, 2, 1, '完成了数据分析报告', NULL, '2026-06-23 16:00:00', 0, 'graded', 88, '报告结构完整，分析深入'),
(7, 3, 2, 1, '在线答题完成', NULL, '2026-06-19 11:00:00', 0, 'graded', 75, NULL);

-- ============ 题库 ============
INSERT IGNORE INTO question (question_id, course_code, lesson_no, type, stem, options, answer, difficulty, knowledge_point, score) VALUES
(1, 1, '1', 'single', 'Python中属于不可变类型的是？', '["int","list","dict","set"]', 'int', 1, '基本数据类型', 10),
(2, 1, '2', 'single', '获取列表长度的函数是？', '["len()","size()","length()","count()"]', 'len()', 2, '基本数据类型', 10),
(3, 1, '3', 'multi', '以下哪些是Python关键字？', '["if","def","class","var"]', 'if,def,class', 2, '基本语法', 15),
(4, 1, '5', 'fill', '定义函数使用的Python关键字是____', NULL, 'def', 2, '函数定义', 10),
(5, 1, '5', 'essay', '简述面向对象的三大特性，并各举一个例子', NULL, '封装：将数据和方法包装在类中；继承：子类继承父类的属性和方法；多态：不同类实现相同接口的不同行为', 4, '面向对象', 25),
(6, 1, '5', 'program', '编写一个函数，接收整数列表并返回其中的最大值', NULL, '评分要点：函数定义清晰；能处理空列表边界；返回最大值而不是打印最大值', 3, '函数定义', 20),
(7, 1, '1', 'single', 'Python中用于输出内容的函数是？', '["print()","echo()","printf()","write()"]', 'print()', 1, '基本语法', 5),
(8, 1, '1', 'single', '以下哪个符号用于单行注释？', '["#","//","/* */","--"]', '#', 1, '基本语法', 5),
(9, 1, '1', 'single', '表达式 3 // 2 的结果是？', '["1","1.5","2","0"]', '1', 2, '运算符', 10),
(10, 1, '2', 'single', '以下哪种结构是键值对集合？', '["list","tuple","dict","set"]', 'dict', 2, '基本数据类型', 10),
(11, 1, '2', 'single', 'Python列表的下标从几开始？', '["0","1","-1","任意数字"]', '0', 1, '列表', 5),
(12, 1, '3', 'single', '用于捕获异常的关键字是？', '["try","catch","error","except"]', 'try', 3, '异常处理', 10),
(13, 1, '3', 'multi', '以下哪些是Python内置数据结构？', '["list","dict","queue","tuple"]', 'list,dict,tuple', 2, '基本数据类型', 15),
(14, 1, '3', 'multi', '以下哪些表达式结果为True？', '["3 > 2","len([1,2]) == 2","bool(\"\")","1 == \"1\""]', '3 > 2,len([1,2]) == 2', 3, '条件判断', 15),
(15, 1, '3', 'multi', '以下哪些方式可以遍历列表？', '["for x in items","for i in range(len(items))","while循环配合下标","switch循环"]', 'for x in items,for i in range(len(items)),while循环配合下标', 3, '循环结构', 15),
(16, 1, '4', 'multi', '关于函数参数，以下说法正确的是？', '["可以设置默认值","可以使用关键字参数","参数名称必须是数字","可以返回多个值"]', '可以设置默认值,可以使用关键字参数,可以返回多个值', 3, '函数定义', 15),
(17, 1, '4', 'fill', 'Python中创建类使用的关键字是____', NULL, 'class', 2, '面向对象', 10),
(18, 1, '4', 'fill', '向列表末尾添加元素常用的方法是____', NULL, 'append', 2, '列表', 10),
(19, 1, '4', 'fill', '字典通过____获取所有键', NULL, 'keys', 3, '字典', 10),
(20, 1, '4', 'fill', '打开文件时，with语句会自动完成资源的____', NULL, '关闭', 3, '文件操作', 10),
(21, 1, '5', 'essay', '说明列表和元组的主要区别，并给出适合使用元组的场景', NULL, '列表可变，元组不可变；元组适合表示固定结构数据、不可变配置或函数多返回值', 3, '基本数据类型', 20),
(22, 1, '5', 'essay', '简述异常处理 try-except-finally 的执行流程', NULL, 'try执行可能出错代码；except处理异常；finally无论是否异常都会执行，常用于释放资源', 4, '异常处理', 20),
(23, 1, '5', 'essay', '解释什么是函数的返回值，返回值和打印输出有什么区别？', NULL, '返回值交给调用方继续使用；打印输出只是展示到控制台，不等同于函数结果', 3, '函数定义', 20),
(24, 1, '5', 'essay', '简述继承的作用，并说明过度继承可能带来的问题', NULL, '继承可复用父类属性和方法；过度继承会增加耦合、层级复杂、理解和维护困难', 5, '面向对象', 25),
(25, 1, '5', 'program', '编写函数 count_words(text)，统计字符串中每个单词出现次数并返回字典', NULL, '评分要点：正确拆分单词；使用字典计数；返回结果；能处理空字符串', 3, '字典', 20),
(26, 1, '5', 'program', '编写函数 is_prime(n)，判断一个整数是否为素数', NULL, '评分要点：处理 n < 2；只需检查到平方根；返回布尔值；逻辑正确', 4, '循环结构', 25),
(27, 1, '5', 'program', '编写函数 flatten(items)，将二维列表展开为一维列表', NULL, '评分要点：遍历嵌套列表；保持元素顺序；返回新列表；不修改原列表', 3, '列表', 20),
(28, 1, '5', 'program', '编写 Student 类，包含姓名和成绩，并提供判断是否及格的方法', NULL, '评分要点：类定义正确；初始化属性；方法返回是否及格；命名清晰', 4, '面向对象', 25),
(29, 1, '5', 'program', '读取文本文件内容并统计非空行数量，写出核心代码或函数', NULL, '评分要点：使用with打开文件；逐行读取；过滤空行；返回统计数量', 4, '文件操作', 25),
(30, 1, '5', 'program', '实现一个装饰器，统计函数执行耗时并打印函数名和耗时', NULL, '评分要点：理解闭包；保留原函数调用；计算执行前后时间；返回原函数结果', 5, '装饰器', 30);

-- 测验套题（Python基础测试）
INSERT IGNORE INTO learning_task (task_no, course_code, task_name, lesson_no, knowledge_points, task_type, description, deadline, submit_method, score, grading_rule, status, allow_late, max_attempts, attachment_formats, resource_url) VALUES
(4, 1, 'Python基础综合测试', 5, '["基本语法","面向对象","函数定义","异常处理"]', 'quiz', 'Python基础测试，考察基本语法和面向对象概念', '2026-07-20 23:59:59', '在线答题', 70, '系统自动评阅客观题，主观题教师复核', 'published', 0, 1, '', NULL);

INSERT IGNORE INTO task_question (id, task_no, question_id) VALUES
(1, 4, 1), (2, 4, 3), (3, 4, 5);
