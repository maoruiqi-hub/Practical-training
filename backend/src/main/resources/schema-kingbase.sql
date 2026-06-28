-- KingbaseES V9 schema for the entity branch.
-- This file is aligned to the current Java entity classes and avoids the older MySQL-only schema.sql.

CREATE SEQUENCE IF NOT EXISTS student_student_no_seq;
CREATE SEQUENCE IF NOT EXISTS teacher_teacher_no_seq;
CREATE SEQUENCE IF NOT EXISTS course_course_code_seq;
CREATE SEQUENCE IF NOT EXISTS lesson_lesson_no_seq;
CREATE SEQUENCE IF NOT EXISTS knowledge_point_knowledge_point_id_seq;
CREATE SEQUENCE IF NOT EXISTS knowledge_relation_relation_id_seq;
CREATE SEQUENCE IF NOT EXISTS course_resource_resource_id_seq;
CREATE SEQUENCE IF NOT EXISTS ability_point_ability_point_id_seq;
CREATE SEQUENCE IF NOT EXISTS ability_knowledge_point_id_seq;
CREATE SEQUENCE IF NOT EXISTS knowledge_extraction_candidate_candidate_id_seq;
CREATE SEQUENCE IF NOT EXISTS learning_task_task_no_seq;
CREATE SEQUENCE IF NOT EXISTS task_submission_submission_id_seq;
CREATE SEQUENCE IF NOT EXISTS submission_ai_review_review_id_seq;
CREATE SEQUENCE IF NOT EXISTS submission_answer_id_seq;
CREATE SEQUENCE IF NOT EXISTS exam_exam_id_seq;
CREATE SEQUENCE IF NOT EXISTS exam_question_id_seq;
CREATE SEQUENCE IF NOT EXISTS question_question_id_seq;
CREATE SEQUENCE IF NOT EXISTS task_question_id_seq;
CREATE SEQUENCE IF NOT EXISTS learning_behavior_log_log_id_seq;

CREATE TABLE IF NOT EXISTS student (
    student_no VARCHAR(64) DEFAULT nextval('student_student_no_seq')::text PRIMARY KEY,
    name VARCHAR(64),
    college VARCHAR(128),
    class_name VARCHAR(128),
    course_grades TEXT,
    username VARCHAR(64) UNIQUE,
    password VARCHAR(128),
    phone VARCHAR(32)
);

CREATE TABLE IF NOT EXISTS teacher (
    teacher_no VARCHAR(64) DEFAULT nextval('teacher_teacher_no_seq')::text PRIMARY KEY,
    name VARCHAR(64),
    college VARCHAR(128),
    major VARCHAR(128),
    phone VARCHAR(32),
    role VARCHAR(16),
    username VARCHAR(64) UNIQUE,
    password VARCHAR(128)
);

CREATE TABLE IF NOT EXISTS course (
    course_code VARCHAR(64) DEFAULT nextval('course_course_code_seq')::text PRIMARY KEY,
    course_name VARCHAR(128),
    teacher VARCHAR(64),
    teacher_no VARCHAR(64),
    credits INTEGER,
    hours INTEGER,
    cover_url VARCHAR(512),
    description TEXT,
    applicable_major VARCHAR(256),
    course_objectives TEXT
);

CREATE TABLE IF NOT EXISTS lesson (
    lesson_no VARCHAR(64) DEFAULT nextval('lesson_lesson_no_seq')::text PRIMARY KEY,
    course_code VARCHAR(64),
    lesson_title VARCHAR(256),
    resource_type VARCHAR(32),
    resource_url VARCHAR(512),
    description TEXT
);

CREATE TABLE IF NOT EXISTS knowledge_point (
    knowledge_point_id VARCHAR(64) DEFAULT nextval('knowledge_point_knowledge_point_id_seq')::text PRIMARY KEY,
    course_code VARCHAR(64),
    lesson_no VARCHAR(64),
    name VARCHAR(256),
    description TEXT,
    chapter VARCHAR(256),
    importance INTEGER,
    generation_method VARCHAR(32)
);

CREATE TABLE IF NOT EXISTS knowledge_relation (
    relation_id VARCHAR(64) DEFAULT nextval('knowledge_relation_relation_id_seq')::text PRIMARY KEY,
    course_code VARCHAR(64),
    from_knowledge_point_id VARCHAR(64),
    to_knowledge_point_id VARCHAR(64),
    relation_type VARCHAR(32)
);

CREATE TABLE IF NOT EXISTS course_resource (
    resource_id VARCHAR(64) DEFAULT nextval('course_resource_resource_id_seq')::text PRIMARY KEY,
    course_code VARCHAR(64),
    title VARCHAR(256),
    resource_type VARCHAR(32),
    file_url VARCHAR(512),
    preview_file_url VARCHAR(512),
    preview_status VARCHAR(32),
    preview_error VARCHAR(512),
    original_filename VARCHAR(512),
    chapter VARCHAR(256),
    knowledge_point_id VARCHAR(64),
    file_size BIGINT,
    uploaded_by VARCHAR(64),
    uploaded_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ability_point (
    ability_point_id VARCHAR(64) DEFAULT nextval('ability_point_ability_point_id_seq')::text PRIMARY KEY,
    course_code VARCHAR(64),
    name VARCHAR(128),
    description TEXT
);

CREATE TABLE IF NOT EXISTS ability_knowledge_point (
    id VARCHAR(64) DEFAULT nextval('ability_knowledge_point_id_seq')::text PRIMARY KEY,
    ability_point_id VARCHAR(64),
    knowledge_point_id VARCHAR(64)
);

CREATE TABLE IF NOT EXISTS knowledge_mastery (
    mastery_id VARCHAR(36) PRIMARY KEY,
    student_no VARCHAR(64),
    course_code VARCHAR(64),
    knowledge_point_id VARCHAR(64),
    mastery_score INTEGER,
    source_type VARCHAR(32),
    source_id VARCHAR(64),
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS knowledge_extraction_candidate (
    candidate_id VARCHAR(64) DEFAULT nextval('knowledge_extraction_candidate_candidate_id_seq')::text PRIMARY KEY,
    course_code VARCHAR(64),
    resource_id VARCHAR(64),
    name VARCHAR(256),
    description TEXT,
    chapter VARCHAR(256),
    importance INTEGER,
    status VARCHAR(32),
    created_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS learning_task (
    task_no VARCHAR(64) DEFAULT nextval('learning_task_task_no_seq')::text PRIMARY KEY,
    course_code VARCHAR(64),
    task_name VARCHAR(256),
    lesson_no VARCHAR(64),
    knowledge_points TEXT,
    task_type VARCHAR(64),
    description TEXT,
    deadline TIMESTAMP,
    submit_method VARCHAR(64),
    score INTEGER,
    grading_rule TEXT,
    status VARCHAR(16),
    allow_late INTEGER,
    max_attempts INTEGER,
    attachment_formats VARCHAR(256),
    resource_url VARCHAR(512)
);

CREATE TABLE IF NOT EXISTS task_submission (
    submission_id VARCHAR(64) DEFAULT nextval('task_submission_submission_id_seq')::text PRIMARY KEY,
    task_no VARCHAR(64),
    student_no VARCHAR(64),
    attempt_number INTEGER,
    content TEXT,
    file_path VARCHAR(512),
    submit_time TIMESTAMP,
    is_overdue INTEGER,
    score INTEGER,
    status VARCHAR(32),
    feedback TEXT
);

CREATE TABLE IF NOT EXISTS submission_ai_review (
    review_id VARCHAR(64) DEFAULT nextval('submission_ai_review_review_id_seq')::text PRIMARY KEY,
    submission_id VARCHAR(64),
    task_no VARCHAR(64),
    student_no VARCHAR(64),
    ai_score INTEGER,
    dimensions TEXT,
    summary TEXT,
    suggestions TEXT,
    risk_level VARCHAR(32),
    status VARCHAR(32),
    create_time TIMESTAMP
);

CREATE TABLE IF NOT EXISTS submission_answer (
    id VARCHAR(64) DEFAULT nextval('submission_answer_id_seq')::text PRIMARY KEY,
    submission_id VARCHAR(64),
    task_no VARCHAR(64),
    student_no VARCHAR(64),
    question_id VARCHAR(64),
    question_stem TEXT,
    question_type VARCHAR(16),
    knowledge_point_id VARCHAR(64),
    student_answer TEXT,
    correct_answer TEXT,
    correct BOOLEAN,
    score INTEGER,
    max_score INTEGER,
    auto_gradable BOOLEAN,
    create_time TIMESTAMP
);

CREATE TABLE IF NOT EXISTS exam (
    exam_id VARCHAR(64) DEFAULT nextval('exam_exam_id_seq')::text PRIMARY KEY,
    course_code VARCHAR(64),
    task_no VARCHAR(64),
    title VARCHAR(128),
    generate_type VARCHAR(32),
    target_count INTEGER,
    total_score INTEGER,
    status VARCHAR(32),
    create_time TIMESTAMP
);

CREATE TABLE IF NOT EXISTS exam_question (
    id VARCHAR(64) DEFAULT nextval('exam_question_id_seq')::text PRIMARY KEY,
    exam_id VARCHAR(64),
    question_id VARCHAR(64),
    sort_order INTEGER,
    score_snapshot INTEGER,
    question_type VARCHAR(16),
    knowledge_point_id VARCHAR(64),
    difficulty INTEGER
);

CREATE TABLE IF NOT EXISTS question (
    question_id VARCHAR(64) DEFAULT nextval('question_question_id_seq')::text PRIMARY KEY,
    course_code VARCHAR(64),
    lesson_no VARCHAR(64),
    type VARCHAR(16),
    stem TEXT,
    options TEXT,
    answer VARCHAR(512),
    difficulty INTEGER,
    knowledge_point_id VARCHAR(64),
    score INTEGER
);

CREATE TABLE IF NOT EXISTS course_game_config (
    id VARCHAR(36) PRIMARY KEY,
    course_id VARCHAR(36) UNIQUE,
    game_mode_enabled BOOLEAN,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS knowledge_point_floor_status (
    id VARCHAR(36) PRIMARY KEY,
    student_id VARCHAR(36),
    course_id VARCHAR(36),
    knowledge_point_id VARCHAR(36),
    status VARCHAR(32),
    cleared_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS task_question (
    id VARCHAR(64) DEFAULT nextval('task_question_id_seq')::text PRIMARY KEY,
    task_no VARCHAR(64),
    question_id VARCHAR(64)
);

CREATE TABLE IF NOT EXISTS learning_behavior_log (
    log_id VARCHAR(64) DEFAULT nextval('learning_behavior_log_log_id_seq')::text PRIMARY KEY,
    user_id VARCHAR(64),
    user_type VARCHAR(16),
    resource_type VARCHAR(32),
    resource_id VARCHAR(64),
    task_no VARCHAR(64),
    knowledge_point VARCHAR(128),
    action_type VARCHAR(32),
    start_time TIMESTAMP,
    duration INTEGER,
    completion_status VARCHAR(32),
    result TEXT,
    created_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS student_profile (
    id VARCHAR(36) PRIMARY KEY,
    student_no INTEGER,
    course_code INTEGER,
    hp INTEGER,
    atk INTEGER,
    def INTEGER,
    exp INTEGER,
    level INTEGER,
    coins INTEGER,
    energy INTEGER,
    status VARCHAR(32),
    consecutive_correct INTEGER,
    recent_answers VARCHAR(255),
    recent_scores VARCHAR(255),
    last_activity_date TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS competency_score (
    id VARCHAR(36) PRIMARY KEY,
    student_no INTEGER,
    course_code INTEGER,
    ability_point_id VARCHAR(64),
    ability_point_name VARCHAR(128),
    score INTEGER,
    last_updated TIMESTAMP
);

CREATE TABLE IF NOT EXISTS recommendation (
    id VARCHAR(36) PRIMARY KEY,
    student_no INTEGER,
    course_code INTEGER,
    type VARCHAR(32),
    target_id VARCHAR(64),
    target_name VARCHAR(256),
    reason TEXT,
    priority INTEGER,
    feedback VARCHAR(16),
    created_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS achievement (
    id VARCHAR(36) PRIMARY KEY,
    student_no INTEGER,
    course_code INTEGER,
    achievement_type VARCHAR(32),
    name VARCHAR(128),
    description VARCHAR(512),
    earned_at TIMESTAMP,
    metadata TEXT
);

CREATE TABLE IF NOT EXISTS competency_score_history (
    id VARCHAR(36) PRIMARY KEY,
    student_no INTEGER,
    course_code INTEGER,
    ability_point_id VARCHAR(64),
    old_score INTEGER,
    new_score INTEGER,
    change_reason VARCHAR(128),
    changed_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS growth_history (
    id VARCHAR(36) PRIMARY KEY,
    student_no INTEGER,
    course_code INTEGER,
    amount INTEGER,
    type VARCHAR(32),
    source VARCHAR(64),
    source_id VARCHAR(64),
    created_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS analytics_class (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(128),
    course_id VARCHAR(36),
    teacher_id VARCHAR(36),
    semester VARCHAR(32),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS analytics_class_student (
    class_id VARCHAR(36) NOT NULL,
    student_id VARCHAR(36) NOT NULL,
    enrolled_at TIMESTAMP,
    PRIMARY KEY (class_id, student_id)
);

CREATE TABLE IF NOT EXISTS analytics_risk_alert (
    id VARCHAR(36) PRIMARY KEY,
    student_id VARCHAR(36),
    course_id VARCHAR(36),
    risk_type VARCHAR(32),
    risk_level VARCHAR(16),
    detail TEXT,
    status VARCHAR(16),
    created_at TIMESTAMP,
    resolved_at TIMESTAMP,
    resolved_by VARCHAR(36)
);
