INSERT INTO teacher (teacher_no, name, college, major, phone, role, username, password)
SELECT '1', 'Admin', 'Academic Affairs', 'Education Technology', '13800000000', 'admin', 'admin', 'admin123'
WHERE NOT EXISTS (SELECT 1 FROM teacher WHERE teacher_no = '1');

INSERT INTO teacher (teacher_no, name, college, major, phone, role, username, password)
SELECT '2', 'Li Ming', 'Computer Science', 'Software Engineering', '13800000001', 'teacher', 'liming', '123456'
WHERE NOT EXISTS (SELECT 1 FROM teacher WHERE teacher_no = '2');

INSERT INTO student (student_no, name, college, class_name, course_grades, username, password, phone)
SELECT '1', 'Zhang San', 'Computer Science', 'CS202', NULL, 'zhangsan', '123456', NULL
WHERE NOT EXISTS (SELECT 1 FROM student WHERE student_no = '1');

INSERT INTO student (student_no, name, college, class_name, course_grades, username, password, phone)
SELECT '2', 'Li Si', 'Computer Science', 'CS201', NULL, 'lisi', '123456', NULL
WHERE NOT EXISTS (SELECT 1 FROM student WHERE student_no = '2');

INSERT INTO course (course_code, course_name, teacher, teacher_no, credits, hours, cover_url, description)
SELECT '1', 'Python Programming', 'Li Ming', '2', 4, 64, NULL, 'Introductory Python programming course'
WHERE NOT EXISTS (SELECT 1 FROM course WHERE course_code = '1');

INSERT INTO lesson (lesson_no, course_code, lesson_title, resource_type, resource_url, description)
SELECT '1', '1', 'Python Introduction', 'video', '/videos/python-intro.mp4', 'Language overview and environment setup'
WHERE NOT EXISTS (SELECT 1 FROM lesson WHERE lesson_no = '1');

INSERT INTO learning_task (task_no, course_code, task_name, lesson_no, knowledge_points, task_type, description,
                           deadline, submit_method, score, grading_rule, status, allow_late, max_attempts,
                           attachment_formats, resource_url)
SELECT '1', '1', 'Student Grade Management System', '1', '["function","file"]', 'programming',
       'Implement a simple student grade management system',
       TIMESTAMP '2026-07-15 23:59:59', 'online', 100, 'Functionality 40; code quality 30; robustness 30',
       'published', 1, 3, '.zip,.py,.java', NULL
WHERE NOT EXISTS (SELECT 1 FROM learning_task WHERE task_no = '1');
