-- 学生表
CREATE TABLE IF NOT EXISTS student (
    student_no INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(64),
    college VARCHAR(128),
    class_name VARCHAR(128),
    course_grades TEXT,
    username VARCHAR(64) UNIQUE,
    password VARCHAR(128),
    phone VARCHAR(32)
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
    knowledge_point VARCHAR(128),
    score INT
);

-- 测验-题目关联表
CREATE TABLE IF NOT EXISTS task_question (
    id INT AUTO_INCREMENT PRIMARY KEY,
    task_no INT,
    question_id INT
);

-- 学生画像表
CREATE TABLE IF NOT EXISTS student_profile (
    id INT AUTO_INCREMENT PRIMARY KEY,
    student_no INT,
    course_code INT,
    hp INT DEFAULT 100,
    atk INT DEFAULT 50,
    def INT DEFAULT 50,
    exp INT DEFAULT 0,
    level INT DEFAULT 1,
    coins INT DEFAULT 0,
    energy INT DEFAULT 5,
    status VARCHAR(32) DEFAULT '正常学习',
    consecutive_correct INT DEFAULT 0,
    recent_answers VARCHAR(255) DEFAULT '',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 能力评分表
CREATE TABLE IF NOT EXISTS competency_score (
    id INT AUTO_INCREMENT PRIMARY KEY,
    student_no INT,
    course_code INT,
    ability_point_id VARCHAR(64),
    ability_point_name VARCHAR(128),
    score INT DEFAULT 50,
    last_updated DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 推荐记录表
CREATE TABLE IF NOT EXISTS recommendation (
    id INT AUTO_INCREMENT PRIMARY KEY,
    student_no INT,
    course_code INT,
    type VARCHAR(32),
    target_id VARCHAR(64),
    target_name VARCHAR(256),
    reason TEXT,
    priority INT DEFAULT 0,
    feedback VARCHAR(16),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 成就记录表
CREATE TABLE IF NOT EXISTS achievement (
    id INT AUTO_INCREMENT PRIMARY KEY,
    student_no INT,
    course_code INT,
    achievement_type VARCHAR(32),
    name VARCHAR(128),
    description VARCHAR(512),
    earned_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    metadata TEXT
);

-- 能力评分变更历史表
CREATE TABLE IF NOT EXISTS competency_score_history (
    id INT AUTO_INCREMENT PRIMARY KEY,
    student_no INT,
    course_code INT,
    ability_point_id VARCHAR(64),
    old_score INT,
    new_score INT,
    change_reason VARCHAR(128),
    changed_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 为已存在的student_profile表补充新列
ALTER TABLE student_profile ADD COLUMN IF NOT EXISTS consecutive_correct INT DEFAULT 0;
ALTER TABLE student_profile ADD COLUMN IF NOT EXISTS recent_answers VARCHAR(255) DEFAULT '';
ALTER TABLE student_profile ADD COLUMN IF NOT EXISTS last_activity_date DATETIME;
ALTER TABLE student_profile ADD COLUMN IF NOT EXISTS recent_scores VARCHAR(255) DEFAULT '';

-- 成长值变更明细表
CREATE TABLE IF NOT EXISTS growth_history (
    id INT AUTO_INCREMENT PRIMARY KEY,
    student_no INT,
    course_code INT,
    amount INT,
    type VARCHAR(32),
    source VARCHAR(64),
    source_id VARCHAR(64),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
