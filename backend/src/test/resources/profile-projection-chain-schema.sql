CREATE TABLE student (
    student_no VARCHAR(64) PRIMARY KEY,
    name VARCHAR(64),
    password VARCHAR(128)
);

CREATE TABLE question (
    question_id VARCHAR(64) PRIMARY KEY,
    course_code VARCHAR(64),
    lesson_no VARCHAR(32),
    type VARCHAR(16),
    stem TEXT,
    options TEXT,
    answer VARCHAR(512),
    difficulty INTEGER,
    knowledge_point_id VARCHAR(64),
    score INTEGER
);

CREATE TABLE knowledge_mastery (
    mastery_id VARCHAR(36) PRIMARY KEY,
    student_no VARCHAR(64) NOT NULL,
    course_code VARCHAR(64) NOT NULL,
    knowledge_point_id VARCHAR(64) NOT NULL,
    mastery_score INTEGER NOT NULL,
    source_type VARCHAR(32),
    source_id VARCHAR(64),
    updated_at TIMESTAMP NOT NULL
);
CREATE UNIQUE INDEX uk_test_knowledge_mastery
    ON knowledge_mastery(student_no, course_code, knowledge_point_id);

CREATE TABLE course_game_config (
    id VARCHAR(36) PRIMARY KEY,
    course_id VARCHAR(64) NOT NULL,
    game_mode_enabled BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
CREATE UNIQUE INDEX uk_test_course_game_config ON course_game_config(course_id);

CREATE TABLE ability_point (
    ability_point_id VARCHAR(64) PRIMARY KEY,
    course_code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    description TEXT
);

CREATE TABLE knowledge_point (
    knowledge_point_id VARCHAR(64) PRIMARY KEY,
    course_code VARCHAR(64) NOT NULL,
    lesson_no VARCHAR(32),
    name VARCHAR(256) NOT NULL,
    description TEXT,
    chapter VARCHAR(256),
    importance INTEGER,
    generation_method VARCHAR(32)
);

CREATE TABLE ability_knowledge_point (
    id VARCHAR(64) PRIMARY KEY,
    ability_point_id VARCHAR(64) NOT NULL,
    knowledge_point_id VARCHAR(64) NOT NULL
);
CREATE UNIQUE INDEX uk_test_ability_knowledge
    ON ability_knowledge_point(ability_point_id, knowledge_point_id);

CREATE TABLE student_profile (
    id VARCHAR(36) PRIMARY KEY,
    student_no VARCHAR(64) NOT NULL,
    course_code VARCHAR(64) NOT NULL,
    hp INTEGER DEFAULT 100,
    atk INTEGER DEFAULT 50,
    def INTEGER DEFAULT 50,
    exp INTEGER DEFAULT 0,
    level INTEGER DEFAULT 1,
    coins INTEGER DEFAULT 0,
    energy INTEGER DEFAULT 5,
    status VARCHAR(32),
    consecutive_correct INTEGER DEFAULT 0,
    recent_answers VARCHAR(255) DEFAULT '',
    recent_scores VARCHAR(255) DEFAULT '',
    last_activity_date TIMESTAMP,
    updated_at TIMESTAMP
);
CREATE UNIQUE INDEX uk_test_student_profile ON student_profile(student_no, course_code);

CREATE TABLE knowledge_point_floor_status (
    id VARCHAR(64) PRIMARY KEY,
    student_id VARCHAR(64) NOT NULL,
    course_id VARCHAR(64) NOT NULL,
    knowledge_point_id VARCHAR(64) NOT NULL,
    status VARCHAR(32),
    cleared_at TIMESTAMP,
    updated_at TIMESTAMP
);
CREATE UNIQUE INDEX uk_test_floor_status
    ON knowledge_point_floor_status(student_id, course_id, knowledge_point_id);

CREATE TABLE competency_score (
    id VARCHAR(36) PRIMARY KEY,
    student_no VARCHAR(64) NOT NULL,
    course_code VARCHAR(64) NOT NULL,
    ability_point_id VARCHAR(64),
    ability_point_name VARCHAR(128),
    score INTEGER DEFAULT 50,
    last_updated TIMESTAMP
);
CREATE UNIQUE INDEX uk_test_competency_score
    ON competency_score(student_no, course_code, ability_point_id);

CREATE TABLE growth_history (
    id VARCHAR(36) PRIMARY KEY,
    student_no VARCHAR(64) NOT NULL,
    course_code VARCHAR(64) NOT NULL,
    amount INTEGER,
    type VARCHAR(32),
    source VARCHAR(64),
    source_id VARCHAR(64),
    created_at TIMESTAMP
);

CREATE TABLE achievement (
    id VARCHAR(36) PRIMARY KEY,
    student_no VARCHAR(64) NOT NULL,
    course_code VARCHAR(64) NOT NULL,
    achievement_type VARCHAR(32),
    badge_code VARCHAR(64),
    name VARCHAR(128),
    description VARCHAR(512),
    earned_at TIMESTAMP,
    metadata TEXT
);

CREATE TABLE learning_task (
    task_no VARCHAR(64) PRIMARY KEY,
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

CREATE TABLE task_submission (
    submission_id VARCHAR(64) PRIMARY KEY,
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
