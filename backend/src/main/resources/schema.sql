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
    teacher_no INT,
    credits INT,
    hours INT,
    cover_url VARCHAR(512),
    description TEXT,
    applicable_major VARCHAR(256),
    course_objectives TEXT
);

ALTER TABLE course ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE course ADD COLUMN IF NOT EXISTS applicable_major VARCHAR(256);
ALTER TABLE course ADD COLUMN IF NOT EXISTS course_objectives TEXT;
ALTER TABLE course ADD COLUMN IF NOT EXISTS teacher_no INT;

-- 知识点表
CREATE TABLE IF NOT EXISTS knowledge_point (
    knowledge_point_id INT AUTO_INCREMENT PRIMARY KEY,
    course_code INT NOT NULL,
    name VARCHAR(256) NOT NULL,
    description TEXT,
    chapter VARCHAR(256),
    importance INT,
    generation_method VARCHAR(32) DEFAULT 'manual',
    INDEX idx_knowledge_point_course (course_code)
);

-- 知识点关系表：from -> to；hierarchy 表示父节点到子节点，prerequisite 表示前置到后置
CREATE TABLE IF NOT EXISTS knowledge_relation (
    relation_id INT AUTO_INCREMENT PRIMARY KEY,
    course_code INT NOT NULL,
    from_knowledge_point_id INT NOT NULL,
    to_knowledge_point_id INT NOT NULL,
    relation_type VARCHAR(32) NOT NULL,
    UNIQUE KEY uk_knowledge_relation (from_knowledge_point_id, to_knowledge_point_id, relation_type),
    INDEX idx_knowledge_relation_course (course_code)
);

-- 课程资源表
CREATE TABLE IF NOT EXISTS course_resource (
    resource_id INT AUTO_INCREMENT PRIMARY KEY,
    course_code INT NOT NULL,
    title VARCHAR(256) NOT NULL,
    resource_type VARCHAR(32) NOT NULL,
    file_url VARCHAR(512) NOT NULL,
    preview_file_url VARCHAR(512),
    preview_status VARCHAR(32) NOT NULL DEFAULT 'not_required',
    preview_error VARCHAR(512),
    original_filename VARCHAR(512),
    chapter VARCHAR(256),
    knowledge_point_id INT,
    file_size BIGINT,
    uploaded_by VARCHAR(64),
    uploaded_at DATETIME NOT NULL,
    INDEX idx_course_resource_course (course_code),
    INDEX idx_course_resource_knowledge_point (knowledge_point_id)
);

ALTER TABLE course_resource ADD COLUMN IF NOT EXISTS preview_file_url VARCHAR(512);
ALTER TABLE course_resource ADD COLUMN IF NOT EXISTS preview_status VARCHAR(32) NOT NULL DEFAULT 'not_required';
ALTER TABLE course_resource ADD COLUMN IF NOT EXISTS preview_error VARCHAR(512);

CREATE TABLE IF NOT EXISTS ability_point (ability_point_id INT AUTO_INCREMENT PRIMARY KEY, course_code INT NOT NULL, name VARCHAR(128) NOT NULL, description TEXT, INDEX idx_ability_point_course(course_code));
CREATE TABLE IF NOT EXISTS ability_knowledge_point (id INT AUTO_INCREMENT PRIMARY KEY, ability_point_id INT NOT NULL, knowledge_point_id INT NOT NULL, UNIQUE KEY uk_ability_knowledge_point(ability_point_id, knowledge_point_id));
CREATE TABLE IF NOT EXISTS knowledge_mastery (mastery_id VARCHAR(36) PRIMARY KEY, student_no INT NOT NULL, course_code INT NOT NULL, knowledge_point_id INT NOT NULL, mastery_score INT NOT NULL, source_type VARCHAR(32), source_id VARCHAR(64), updated_at DATETIME NOT NULL, UNIQUE KEY uk_knowledge_mastery(student_no,course_code,knowledge_point_id));
CREATE TABLE IF NOT EXISTS knowledge_extraction_candidate (candidate_id INT AUTO_INCREMENT PRIMARY KEY, course_code INT NOT NULL, resource_id INT NOT NULL, name VARCHAR(256) NOT NULL, description TEXT, chapter VARCHAR(256), importance INT, status VARCHAR(32) NOT NULL, created_at DATETIME NOT NULL, INDEX idx_extraction_candidate_course(course_code));
ALTER TABLE knowledge_extraction_candidate ADD COLUMN IF NOT EXISTS importance INT;

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
    task_name VARCHAR(256),
    lesson_no INT,
    knowledge_points TEXT,
    task_type VARCHAR(64),
    description TEXT,
    deadline DATETIME,
    submit_method VARCHAR(64),
    score INT,
    grading_rule TEXT,
    status VARCHAR(16) DEFAULT 'published',
    allow_late TINYINT DEFAULT 0,
    max_attempts INT DEFAULT 3,
    attachment_formats VARCHAR(256),
    resource_url VARCHAR(512)
);

-- 任务提交记录表
CREATE TABLE IF NOT EXISTS task_submission (
    submission_id INT AUTO_INCREMENT PRIMARY KEY,
    task_no INT,
    student_no INT,
    attempt_number INT DEFAULT 1,
    content TEXT,
    file_path VARCHAR(512),
    submit_time DATETIME,
    is_overdue TINYINT DEFAULT 0,
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

ALTER TABLE knowledge_point ADD COLUMN IF NOT EXISTS lesson_no VARCHAR(32);
ALTER TABLE knowledge_point ADD COLUMN IF NOT EXISTS description TEXT;

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

-- 模块一、模块三爬塔集成新增表：UUID 主键，跨模块引用均为 VARCHAR(36)。
-- 旧教学业务表保持原 INT 主键，避免破坏已导入的历史数据。
CREATE TABLE IF NOT EXISTS course_game_config (
    id VARCHAR(36) PRIMARY KEY,
    course_id VARCHAR(36) NOT NULL UNIQUE,
    game_mode_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS knowledge_point_floor_status (
    id VARCHAR(36) PRIMARY KEY,
    student_id VARCHAR(36) NOT NULL,
    course_id VARCHAR(36) NOT NULL,
    knowledge_point_id VARCHAR(36) NOT NULL,
    status VARCHAR(32) NOT NULL,
    cleared_at DATETIME,
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_kp_floor_status (student_id, course_id, knowledge_point_id)
);

-- 测验-题目关联表
CREATE TABLE IF NOT EXISTS task_question (
    id INT AUTO_INCREMENT PRIMARY KEY,
    task_no INT,
    question_id INT
);

-- 学习行为日志表（模块2）
CREATE TABLE IF NOT EXISTS learning_behavior_log (
    log_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    user_type VARCHAR(16) NOT NULL,
    resource_type VARCHAR(32) NOT NULL,
    resource_id INT,
    task_no INT,
    knowledge_point VARCHAR(128),
    action_type VARCHAR(32) NOT NULL,
    start_time DATETIME,
    duration INT DEFAULT 0,
    completion_status VARCHAR(32),
    result TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_time (user_id, created_at),
    INDEX idx_task (task_no),
    INDEX idx_action (action_type)
);

-- ============================================================
-- 模块4：学生画像与个性化学习
-- ============================================================

-- 学生画像表
-- ============================================================
-- 模块4：学生画像与个性化学习（§7.1，表前缀：profile_/competency_score_/recommendation_/achievement_）
-- 所有新表主键使用 VARCHAR(36) + ASSIGN_UUID，跨模块引用字段使用 VARCHAR(36)
-- 迁移说明：已有 INT AUTO_INCREMENT 表暂不强制迁移，新部署将使用 VARCHAR(36)
-- ============================================================

CREATE TABLE IF NOT EXISTS student_profile (
    id VARCHAR(36) PRIMARY KEY,
    student_no VARCHAR(36) NOT NULL,
    course_code VARCHAR(36) NOT NULL,
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
    last_activity_date DATETIME,
    recent_scores VARCHAR(255) DEFAULT '',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 能力评分表
CREATE TABLE IF NOT EXISTS competency_score (
    id VARCHAR(36) PRIMARY KEY,
    student_no VARCHAR(36) NOT NULL,
    course_code VARCHAR(36) NOT NULL,
    ability_point_id VARCHAR(64),
    ability_point_name VARCHAR(128),
    score INT DEFAULT 50,
    last_updated DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_competency_student_ability (student_no, course_code, ability_point_id)
);

-- 推荐记录表
CREATE TABLE IF NOT EXISTS recommendation (
    id VARCHAR(36) PRIMARY KEY,
    student_no VARCHAR(36) NOT NULL,
    course_code VARCHAR(36) NOT NULL,
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
    id VARCHAR(36) PRIMARY KEY,
    student_no VARCHAR(36) NOT NULL,
    course_code VARCHAR(36) NOT NULL,
    achievement_type VARCHAR(32),
    name VARCHAR(128),
    description VARCHAR(512),
    earned_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    metadata TEXT
);

-- 能力评分变更历史表
CREATE TABLE IF NOT EXISTS competency_score_history (
    id VARCHAR(36) PRIMARY KEY,
    student_no VARCHAR(36) NOT NULL,
    course_code VARCHAR(36) NOT NULL,
    ability_point_id VARCHAR(64),
    old_score INT,
    new_score INT,
    change_reason VARCHAR(128),
    changed_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 成长值变更明细表
CREATE TABLE IF NOT EXISTS growth_history (
    id VARCHAR(36) PRIMARY KEY,
    student_no VARCHAR(36) NOT NULL,
    course_code VARCHAR(36) NOT NULL,
    amount INT,
    type VARCHAR(32),
    source VARCHAR(64),
    source_id VARCHAR(64),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 模块5：学情分析与教学决策
-- ============================================================

-- 班级表
CREATE TABLE IF NOT EXISTS analytics_class (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    course_id VARCHAR(36),
    teacher_id VARCHAR(36),
    semester VARCHAR(32),
    created_at DATETIME,
    updated_at DATETIME
);

-- 班级-学生关联表
CREATE TABLE IF NOT EXISTS analytics_class_student (
    id INT AUTO_INCREMENT PRIMARY KEY,
    class_id VARCHAR(36) NOT NULL,
    student_id VARCHAR(36) NOT NULL,
    enrolled_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_class_student (class_id, student_id)
);

-- 学习风险预警表
CREATE TABLE IF NOT EXISTS analytics_risk_alert (
    id VARCHAR(36) PRIMARY KEY,
    student_id VARCHAR(36) NOT NULL,
    course_id VARCHAR(36),
    risk_type VARCHAR(32) NOT NULL,
    risk_level VARCHAR(16) NOT NULL,
    detail JSON,
    status VARCHAR(16) DEFAULT 'active',
    created_at DATETIME,
    resolved_at DATETIME,
    resolved_by VARCHAR(36),
    UNIQUE KEY uk_active_alert (student_id, risk_type, status)
);

-- 分析报告表
CREATE TABLE IF NOT EXISTS analytics_report (
    id VARCHAR(36) PRIMARY KEY,
    class_id VARCHAR(36) NOT NULL,
    report_type VARCHAR(32) NOT NULL,
    data_json JSON,
    generated_at DATETIME
);

-- 教学建议表
CREATE TABLE IF NOT EXISTS analytics_teaching_suggestion (
    id VARCHAR(36) PRIMARY KEY,
    class_id VARCHAR(36),
    course_id VARCHAR(36),
    suggestion_type VARCHAR(32),
    content TEXT,
    target_type VARCHAR(16),
    target_id VARCHAR(36),
    urgency VARCHAR(16),
    based_on JSON,
    generated_at DATETIME
);
