-- 学生表
CREATE TABLE IF NOT EXISTS student (
    student_no INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(64),
    college VARCHAR(128),
    class_name VARCHAR(128),
    course_grades TEXT,
    username VARCHAR(64) UNIQUE,
    password VARCHAR(128)
);

-- 教师表
CREATE TABLE IF NOT EXISTS teacher (
    teacher_no INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(64),
    college VARCHAR(128),
    major VARCHAR(128),
    phone VARCHAR(32),
    role VARCHAR(16) DEFAULT 'teacher',
    username VARCHAR(64) UNIQUE,
    password VARCHAR(128)
);

-- 课程表
CREATE TABLE IF NOT EXISTS course (
    course_code INT AUTO_INCREMENT PRIMARY KEY,
    course_name VARCHAR(128),
    teacher VARCHAR(64),
    credits INT,
    hours INT,
    cover_url VARCHAR(512)
);

-- 课时表
CREATE TABLE IF NOT EXISTS lesson (
    lesson_no INT AUTO_INCREMENT PRIMARY KEY,
    course_code INT,
    lesson_title VARCHAR(256),
    resource_type VARCHAR(32),
    resource_url VARCHAR(512),
    description TEXT
);

-- 学习任务表
CREATE TABLE IF NOT EXISTS learning_task (
    task_no INT AUTO_INCREMENT PRIMARY KEY,
    course_code INT,
    task_type VARCHAR(64),
    description TEXT,
    deadline DATETIME,
    submit_method VARCHAR(64),
    score INT,
    resource_url VARCHAR(512)
);

-- 任务提交记录表
CREATE TABLE IF NOT EXISTS task_submission (
    submission_id INT AUTO_INCREMENT PRIMARY KEY,
    task_no INT,
    student_no INT,
    content TEXT,
    file_path VARCHAR(512),
    submit_time DATETIME,
    score INT,
    status VARCHAR(32),
    feedback TEXT
);

-- 主观提交 AI 辅助评价表
CREATE TABLE IF NOT EXISTS submission_ai_review (
    review_id INT AUTO_INCREMENT PRIMARY KEY,
    submission_id INT,
    task_no INT,
    student_no INT,
    ai_score INT,
    dimensions TEXT,
    summary TEXT,
    suggestions TEXT,
    risk_level VARCHAR(32),
    status VARCHAR(32),
    create_time DATETIME
);

-- 在线测验逐题作答明细表
CREATE TABLE IF NOT EXISTS submission_answer (
    id INT AUTO_INCREMENT PRIMARY KEY,
    submission_id INT,
    task_no INT,
    student_no INT,
    question_id INT,
    question_stem TEXT,
    question_type VARCHAR(16),
    knowledge_point_id INT,
    student_answer TEXT,
    correct_answer TEXT,
    correct BOOLEAN,
    score INT,
    max_score INT,
    auto_gradable BOOLEAN,
    create_time DATETIME
);

ALTER TABLE submission_answer ADD COLUMN IF NOT EXISTS question_stem TEXT;
ALTER TABLE submission_answer ADD COLUMN IF NOT EXISTS knowledge_point_id INT;
ALTER TABLE submission_answer DROP COLUMN IF EXISTS knowledge_point;

-- 试卷版本表
CREATE TABLE IF NOT EXISTS exam (
    exam_id INT AUTO_INCREMENT PRIMARY KEY,
    course_code INT,
    task_no INT,
    title VARCHAR(128),
    generate_type VARCHAR(32),
    target_count INT,
    total_score INT,
    status VARCHAR(32),
    create_time DATETIME
);

-- 试卷题目快照表
CREATE TABLE IF NOT EXISTS exam_question (
    id INT AUTO_INCREMENT PRIMARY KEY,
    exam_id INT,
    question_id INT,
    sort_order INT,
    score_snapshot INT,
    question_type VARCHAR(16),
    knowledge_point_id INT,
    difficulty INT
);
ALTER TABLE exam_question ADD COLUMN IF NOT EXISTS knowledge_point_id INT;
ALTER TABLE exam_question DROP COLUMN IF EXISTS knowledge_point;

-- 知识点实体表
CREATE TABLE IF NOT EXISTS knowledge_point (
    knowledge_point_id INT AUTO_INCREMENT PRIMARY KEY,
    course_code INT,
    lesson_no VARCHAR(32),
    name VARCHAR(128),
    description TEXT
);
ALTER TABLE knowledge_point ADD COLUMN IF NOT EXISTS lesson_no VARCHAR(32);
ALTER TABLE knowledge_point ADD COLUMN IF NOT EXISTS description TEXT;

-- 知识点关系表
CREATE TABLE IF NOT EXISTS knowledge_relation (
    relation_id INT AUTO_INCREMENT PRIMARY KEY,
    course_code INT,
    from_knowledge_point_id INT,
    to_knowledge_point_id INT,
    relation_type VARCHAR(32)
);

-- 题库表
CREATE TABLE IF NOT EXISTS question (
    question_id INT AUTO_INCREMENT PRIMARY KEY,
    course_code INT,
    lesson_no VARCHAR(32),
    type VARCHAR(16),
    stem TEXT,
    options TEXT,
    answer VARCHAR(512),
    difficulty INT,
    knowledge_point_id INT,
    score INT
);

ALTER TABLE question ADD COLUMN IF NOT EXISTS knowledge_point_id INT;
UPDATE question q
JOIN knowledge_point kp ON kp.course_code = q.course_code AND kp.name = q.knowledge_point
SET q.knowledge_point_id = kp.knowledge_point_id
WHERE q.knowledge_point_id IS NULL;
ALTER TABLE question DROP COLUMN IF EXISTS knowledge_point;

-- 测验-题目关联表
CREATE TABLE IF NOT EXISTS task_question (
    id INT AUTO_INCREMENT PRIMARY KEY,
    task_no INT,
    question_id INT
);
