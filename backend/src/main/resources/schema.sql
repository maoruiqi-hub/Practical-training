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
    score INT
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
