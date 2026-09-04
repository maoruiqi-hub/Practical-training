CREATE SEQUENCE IF NOT EXISTS student_no_seq START WITH 1000 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS teacher_no_seq START WITH 1000 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS course_code_seq START WITH 1000 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS knowledge_point_id_seq START WITH 1000 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS relation_id_seq START WITH 1000 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS course_resource_id_seq START WITH 1000 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS ability_point_id_seq START WITH 1000 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS ability_knowledge_point_id_seq START WITH 1000 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS knowledge_extraction_candidate_id_seq START WITH 1000 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS lesson_no_seq START WITH 1000 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS learning_task_no_seq START WITH 1000 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS task_assignment_id_seq START WITH 1000 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS task_submission_id_seq START WITH 1000 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS submission_ai_review_id_seq START WITH 1000 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS submission_answer_id_seq START WITH 1000 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS exam_id_seq START WITH 1000 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS exam_question_id_seq START WITH 1000 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS question_id_seq START WITH 1000 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS task_question_id_seq START WITH 1000 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS learning_behavior_log_id_seq START WITH 1000 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS analytics_class_student_id_seq START WITH 1000 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS student (
    student_no VARCHAR(64) PRIMARY KEY DEFAULT CAST(nextval('student_no_seq') AS VARCHAR(64)),
    name VARCHAR(64),
    college VARCHAR(128),
    class_name VARCHAR(128),
    course_grades TEXT,
    username VARCHAR(64),
    password VARCHAR(128),
    phone VARCHAR(32)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_student_username ON student(username);

CREATE TABLE IF NOT EXISTS teacher (
    teacher_no VARCHAR(64) PRIMARY KEY DEFAULT CAST(nextval('teacher_no_seq') AS VARCHAR(64)),
    name VARCHAR(64),
    college VARCHAR(128),
    major VARCHAR(128),
    phone VARCHAR(32),
    role VARCHAR(16) DEFAULT 'teacher',
    username VARCHAR(64),
    password VARCHAR(128)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_teacher_username ON teacher(username);

CREATE TABLE IF NOT EXISTS course (
    course_code VARCHAR(64) PRIMARY KEY DEFAULT CAST(nextval('course_code_seq') AS VARCHAR(64)),
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
CREATE INDEX IF NOT EXISTS idx_course_teacher_no ON course(teacher_no);

CREATE TABLE IF NOT EXISTS knowledge_point (
    knowledge_point_id VARCHAR(64) PRIMARY KEY DEFAULT CAST(nextval('knowledge_point_id_seq') AS VARCHAR(64)),
    course_code VARCHAR(64) NOT NULL,
    lesson_no VARCHAR(32),
    name VARCHAR(256) NOT NULL,
    description TEXT,
    chapter VARCHAR(256),
    importance INTEGER,
    generation_method VARCHAR(32) DEFAULT 'manual'
);
CREATE INDEX IF NOT EXISTS idx_knowledge_point_course ON knowledge_point(course_code);
CREATE INDEX IF NOT EXISTS idx_knowledge_point_lesson ON knowledge_point(lesson_no);

CREATE TABLE IF NOT EXISTS knowledge_relation (
    relation_id VARCHAR(64) PRIMARY KEY DEFAULT CAST(nextval('relation_id_seq') AS VARCHAR(64)),
    course_code VARCHAR(64) NOT NULL,
    from_knowledge_point_id VARCHAR(64) NOT NULL,
    to_knowledge_point_id VARCHAR(64) NOT NULL,
    relation_type VARCHAR(32) NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_knowledge_relation
    ON knowledge_relation(from_knowledge_point_id, to_knowledge_point_id, relation_type);
CREATE INDEX IF NOT EXISTS idx_knowledge_relation_course ON knowledge_relation(course_code);

CREATE TABLE IF NOT EXISTS course_resource (
    resource_id VARCHAR(64) PRIMARY KEY DEFAULT CAST(nextval('course_resource_id_seq') AS VARCHAR(64)),
    course_code VARCHAR(64) NOT NULL,
    title VARCHAR(256) NOT NULL,
    resource_type VARCHAR(32) NOT NULL,
    file_url VARCHAR(512) NOT NULL,
    preview_file_url VARCHAR(512),
    preview_status VARCHAR(32) NOT NULL DEFAULT 'not_required',
    preview_error VARCHAR(512),
    original_filename VARCHAR(512),
    chapter VARCHAR(256),
    knowledge_point_id VARCHAR(64),
    file_size BIGINT,
    uploaded_by VARCHAR(64),
    uploaded_at TIMESTAMP NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_course_resource_course ON course_resource(course_code);
CREATE INDEX IF NOT EXISTS idx_course_resource_knowledge_point ON course_resource(knowledge_point_id);

CREATE TABLE IF NOT EXISTS ability_point (
    ability_point_id VARCHAR(64) PRIMARY KEY DEFAULT CAST(nextval('ability_point_id_seq') AS VARCHAR(64)),
    course_code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    description TEXT
);
CREATE INDEX IF NOT EXISTS idx_ability_point_course ON ability_point(course_code);

CREATE TABLE IF NOT EXISTS ability_knowledge_point (
    id VARCHAR(64) PRIMARY KEY DEFAULT CAST(nextval('ability_knowledge_point_id_seq') AS VARCHAR(64)),
    ability_point_id VARCHAR(64) NOT NULL,
    knowledge_point_id VARCHAR(64) NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_ability_knowledge_point
    ON ability_knowledge_point(ability_point_id, knowledge_point_id);

CREATE TABLE IF NOT EXISTS competency_point (
    competency_id VARCHAR(64) PRIMARY KEY,
    course_code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    description TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    sort_order INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT ck_competency_status CHECK (status IN ('active', 'inactive')),
    CONSTRAINT fk_competency_course FOREIGN KEY (course_code) REFERENCES course(course_code)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_competency_point_course_name
    ON competency_point(course_code, name);

CREATE TABLE IF NOT EXISTS ability_point_competency_relation (
    id VARCHAR(64) PRIMARY KEY,
    course_code VARCHAR(64) NOT NULL,
    ability_point_id VARCHAR(64) NOT NULL,
    competency_id VARCHAR(64) NOT NULL,
    relation_status VARCHAR(32) NOT NULL,
    strength DECIMAL(8, 6) NOT NULL DEFAULT 0,
    confidence DECIMAL(8, 6) NOT NULL DEFAULT 0,
    strength_source VARCHAR(32) NOT NULL DEFAULT 'uniform_prior',
    evidence_count INTEGER NOT NULL DEFAULT 0,
    matrix_version VARCHAR(64) NOT NULL DEFAULT 'v1',
    review_note TEXT,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_mapping_relation_status CHECK (relation_status IN ('related', 'unrelated', 'uncertain')),
    CONSTRAINT ck_mapping_strength CHECK (strength >= 0 AND strength <= 1),
    CONSTRAINT ck_mapping_confidence CHECK (confidence >= 0 AND confidence <= 1),
    CONSTRAINT ck_mapping_evidence_count CHECK (evidence_count >= 0),
    CONSTRAINT fk_mapping_ability FOREIGN KEY (ability_point_id) REFERENCES ability_point(ability_point_id),
    CONSTRAINT fk_mapping_competency FOREIGN KEY (competency_id) REFERENCES competency_point(competency_id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_ability_competency_relation
    ON ability_point_competency_relation(course_code, ability_point_id, competency_id, matrix_version);

CREATE TABLE IF NOT EXISTS competency_task_observation (
    id VARCHAR(64) PRIMARY KEY,
    course_code VARCHAR(64) NOT NULL,
    task_no VARCHAR(64) NOT NULL,
    competency_id VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_observation_status CHECK (status IN ('active', 'inactive')),
    CONSTRAINT fk_observation_competency FOREIGN KEY (competency_id) REFERENCES competency_point(competency_id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_competency_task_observation
    ON competency_task_observation(course_code, task_no, competency_id);

CREATE TABLE IF NOT EXISTS ability_competency_matrix_version (
    id VARCHAR(64) PRIMARY KEY,
    course_code VARCHAR(64) NOT NULL,
    version VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    based_on_version VARCHAR(64),
    sample_count INTEGER NOT NULL DEFAULT 0,
    validation_sample_count INTEGER NOT NULL DEFAULT 0,
    algorithm_version VARCHAR(32) NOT NULL DEFAULT 'pearson-v1',
    published_by VARCHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP,
    CONSTRAINT ck_mapping_version_status CHECK (status IN ('draft', 'published', 'archived')),
    CONSTRAINT fk_mapping_version_course FOREIGN KEY (course_code) REFERENCES course(course_code)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_ability_competency_matrix_version
    ON ability_competency_matrix_version(course_code, version);
CREATE UNIQUE INDEX IF NOT EXISTS uk_ability_competency_published_course
    ON ability_competency_matrix_version(course_code)
    WHERE status = 'published';

CREATE TABLE IF NOT EXISTS knowledge_mastery (
    mastery_id VARCHAR(36) PRIMARY KEY,
    student_no VARCHAR(64) NOT NULL,
    course_code VARCHAR(64) NOT NULL,
    knowledge_point_id VARCHAR(64) NOT NULL,
    mastery_score INTEGER NOT NULL,
    source_type VARCHAR(32),
    source_id VARCHAR(64),
    updated_at TIMESTAMP NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_knowledge_mastery
    ON knowledge_mastery(student_no, course_code, knowledge_point_id);

CREATE TABLE IF NOT EXISTS learning_answer_evidence (
    evidence_id VARCHAR(64) PRIMARY KEY,
    student_no VARCHAR(64) NOT NULL,
    course_code VARCHAR(64) NOT NULL,
    question_id VARCHAR(64) NOT NULL,
    knowledge_point_id VARCHAR(64) NOT NULL,
    difficulty INTEGER NOT NULL,
    attempt_no INTEGER NOT NULL,
    first_attempt BOOLEAN NOT NULL,
    correct BOOLEAN NOT NULL,
    answer_content TEXT,
    source_type VARCHAR(32) NOT NULL,
    source_id VARCHAR(64) NOT NULL,
    idempotency_key VARCHAR(160) NOT NULL,
    formula_version VARCHAR(32) NOT NULL,
    answered_at TIMESTAMP NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_learning_evidence_student_key
    ON learning_answer_evidence(student_no, idempotency_key);
CREATE INDEX IF NOT EXISTS idx_learning_evidence_student_question
    ON learning_answer_evidence(student_no, question_id, answered_at);

CREATE TABLE IF NOT EXISTS knowledge_mastery_history (
    history_id VARCHAR(64) PRIMARY KEY,
    evidence_id VARCHAR(64) NOT NULL,
    student_no VARCHAR(64) NOT NULL,
    course_code VARCHAR(64) NOT NULL,
    knowledge_point_id VARCHAR(64) NOT NULL,
    before_score INTEGER NOT NULL,
    after_score INTEGER NOT NULL,
    target_score INTEGER NOT NULL,
    alpha NUMERIC(6,5) NOT NULL,
    formula_version VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_mastery_history_evidence
    ON knowledge_mastery_history(evidence_id);
CREATE INDEX IF NOT EXISTS idx_mastery_history_student_point
    ON knowledge_mastery_history(student_no, course_code, knowledge_point_id, created_at);

CREATE TABLE IF NOT EXISTS student_ability_snapshot (
    snapshot_id VARCHAR(64) PRIMARY KEY,
    evaluation_id VARCHAR(64) NOT NULL,
    student_no VARCHAR(64) NOT NULL,
    course_code VARCHAR(64) NOT NULL,
    run_id VARCHAR(64) NOT NULL,
    node_id VARCHAR(64) NOT NULL,
    phase VARCHAR(16) NOT NULL,
    ability_point_id VARCHAR(64) NOT NULL,
    ability_point_name VARCHAR(128) NOT NULL,
    score INTEGER NOT NULL,
    evidence_knowledge_count INTEGER NOT NULL,
    total_knowledge_count INTEGER NOT NULL,
    knowledge_point_ids_json TEXT NOT NULL,
    weights_json TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_ability_snapshot_evaluation_phase_point
    ON student_ability_snapshot(evaluation_id, phase, ability_point_id);
CREATE INDEX IF NOT EXISTS idx_ability_snapshot_student_node
    ON student_ability_snapshot(student_no, course_code, run_id, node_id, created_at);

CREATE TABLE IF NOT EXISTS knowledge_extraction_candidate (
    candidate_id VARCHAR(64) PRIMARY KEY DEFAULT CAST(nextval('knowledge_extraction_candidate_id_seq') AS VARCHAR(64)),
    course_code VARCHAR(64) NOT NULL,
    resource_id VARCHAR(64) NOT NULL,
    name VARCHAR(256) NOT NULL,
    description TEXT,
    chapter VARCHAR(256),
    importance INTEGER,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_extraction_candidate_course ON knowledge_extraction_candidate(course_code);

CREATE TABLE IF NOT EXISTS lesson (
    lesson_no VARCHAR(64) PRIMARY KEY DEFAULT CAST(nextval('lesson_no_seq') AS VARCHAR(64)),
    course_code VARCHAR(64),
    lesson_title VARCHAR(256),
    resource_type VARCHAR(32),
    resource_url VARCHAR(512),
    description TEXT
);
CREATE INDEX IF NOT EXISTS idx_lesson_course ON lesson(course_code);

CREATE TABLE IF NOT EXISTS learning_task (
    task_no VARCHAR(64) PRIMARY KEY DEFAULT CAST(nextval('learning_task_no_seq') AS VARCHAR(64)),
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
    status VARCHAR(16) DEFAULT 'published',
    allow_late INTEGER DEFAULT 0,
    max_attempts INTEGER DEFAULT 3,
    attachment_formats VARCHAR(256),
    resource_url VARCHAR(512)
);
CREATE INDEX IF NOT EXISTS idx_learning_task_course ON learning_task(course_code);
CREATE INDEX IF NOT EXISTS idx_learning_task_lesson ON learning_task(lesson_no);

CREATE TABLE IF NOT EXISTS task_assignment (
    assignment_id VARCHAR(64) PRIMARY KEY DEFAULT CAST(nextval('task_assignment_id_seq') AS VARCHAR(64)),
    task_no VARCHAR(64) NOT NULL,
    course_code VARCHAR(64) NOT NULL,
    student_no VARCHAR(64) NOT NULL,
    assigned_by VARCHAR(64),
    assigned_at TIMESTAMP NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'assigned',
    note TEXT
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_task_assignment_student_task ON task_assignment(task_no, student_no);
CREATE INDEX IF NOT EXISTS idx_task_assignment_student ON task_assignment(student_no);
CREATE INDEX IF NOT EXISTS idx_task_assignment_task ON task_assignment(task_no);
CREATE INDEX IF NOT EXISTS idx_task_assignment_course ON task_assignment(course_code);

CREATE TABLE IF NOT EXISTS task_submission (
    submission_id VARCHAR(64) PRIMARY KEY DEFAULT CAST(nextval('task_submission_id_seq') AS VARCHAR(64)),
    task_no VARCHAR(64),
    student_no VARCHAR(64),
    attempt_number INTEGER DEFAULT 1,
    content TEXT,
    file_path VARCHAR(512),
    submit_time TIMESTAMP,
    is_overdue INTEGER DEFAULT 0,
    score INTEGER,
    status VARCHAR(32),
    feedback TEXT,
    intervention_reason TEXT,
    intervention_by VARCHAR(64),
    intervention_at TIMESTAMP,
    previous_score INTEGER
);
CREATE INDEX IF NOT EXISTS idx_task_submission_task ON task_submission(task_no);
CREATE INDEX IF NOT EXISTS idx_task_submission_student ON task_submission(student_no);

CREATE TABLE IF NOT EXISTS submission_ai_review (
    review_id VARCHAR(64) PRIMARY KEY DEFAULT CAST(nextval('submission_ai_review_id_seq') AS VARCHAR(64)),
    submission_id VARCHAR(64),
    task_no VARCHAR(64),
    student_no VARCHAR(64),
    ai_score INTEGER,
    confidence DECIMAL(4,3),
    basis VARCHAR(32),
    dimensions TEXT,
    summary TEXT,
    suggestions TEXT,
    risk_level VARCHAR(32),
    status VARCHAR(32),
    create_time TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_ai_review_submission ON submission_ai_review(submission_id);

CREATE TABLE IF NOT EXISTS submission_answer (
    id VARCHAR(64) PRIMARY KEY DEFAULT CAST(nextval('submission_answer_id_seq') AS VARCHAR(64)),
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
CREATE INDEX IF NOT EXISTS idx_submission_answer_submission ON submission_answer(submission_id);
CREATE INDEX IF NOT EXISTS idx_submission_answer_student ON submission_answer(student_no);
CREATE INDEX IF NOT EXISTS idx_submission_answer_task ON submission_answer(task_no);
CREATE UNIQUE INDEX IF NOT EXISTS uk_submission_answer_submission_question
    ON submission_answer(submission_id, question_id);

CREATE TABLE IF NOT EXISTS exam (
    exam_id VARCHAR(64) PRIMARY KEY DEFAULT CAST(nextval('exam_id_seq') AS VARCHAR(64)),
    course_code VARCHAR(64),
    task_no VARCHAR(64),
    title VARCHAR(128),
    generate_type VARCHAR(32),
    target_count INTEGER,
    total_score INTEGER,
    status VARCHAR(32),
    create_time TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_exam_course ON exam(course_code);
CREATE INDEX IF NOT EXISTS idx_exam_task ON exam(task_no);

CREATE TABLE IF NOT EXISTS exam_question (
    id VARCHAR(64) PRIMARY KEY DEFAULT CAST(nextval('exam_question_id_seq') AS VARCHAR(64)),
    exam_id VARCHAR(64),
    question_id VARCHAR(64),
    sort_order INTEGER,
    score_snapshot INTEGER,
    question_type VARCHAR(16),
    knowledge_point_id VARCHAR(64),
    difficulty INTEGER
);
CREATE INDEX IF NOT EXISTS idx_exam_question_exam ON exam_question(exam_id);

CREATE TABLE IF NOT EXISTS question (
    question_id VARCHAR(64) PRIMARY KEY DEFAULT CAST(nextval('question_id_seq') AS VARCHAR(64)),
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
CREATE INDEX IF NOT EXISTS idx_question_course ON question(course_code);
CREATE INDEX IF NOT EXISTS idx_question_lesson ON question(lesson_no);
CREATE INDEX IF NOT EXISTS idx_question_knowledge_point ON question(knowledge_point_id);

CREATE TABLE IF NOT EXISTS course_game_config (
    id VARCHAR(36) PRIMARY KEY,
    course_id VARCHAR(64) NOT NULL,
    game_mode_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_course_game_config_course ON course_game_config(course_id);

CREATE TABLE IF NOT EXISTS knowledge_point_floor_status (
    id VARCHAR(36) PRIMARY KEY,
    student_id VARCHAR(64) NOT NULL,
    course_id VARCHAR(64) NOT NULL,
    knowledge_point_id VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    cleared_at TIMESTAMP,
    updated_at TIMESTAMP NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_kp_floor_status
    ON knowledge_point_floor_status(student_id, course_id, knowledge_point_id);

CREATE TABLE IF NOT EXISTS task_question (
    id VARCHAR(64) PRIMARY KEY DEFAULT CAST(nextval('task_question_id_seq') AS VARCHAR(64)),
    task_no VARCHAR(64),
    question_id VARCHAR(64)
);
CREATE INDEX IF NOT EXISTS idx_task_question_task ON task_question(task_no);
CREATE INDEX IF NOT EXISTS idx_task_question_question ON task_question(question_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_task_question_task_question
    ON task_question(task_no, question_id);

CREATE TABLE IF NOT EXISTS learning_behavior_log (
    log_id VARCHAR(64) PRIMARY KEY DEFAULT CAST(nextval('learning_behavior_log_id_seq') AS VARCHAR(64)),
    user_id VARCHAR(64) NOT NULL,
    user_type VARCHAR(16) NOT NULL,
    resource_type VARCHAR(32) NOT NULL,
    resource_id VARCHAR(64),
    task_no VARCHAR(64),
    knowledge_point VARCHAR(128),
    action_type VARCHAR(32) NOT NULL,
    start_time TIMESTAMP,
    duration INTEGER DEFAULT 0,
    completion_status VARCHAR(32),
    result TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_user_time ON learning_behavior_log(user_id, created_at);
CREATE INDEX IF NOT EXISTS idx_task ON learning_behavior_log(task_no);
CREATE INDEX IF NOT EXISTS idx_action ON learning_behavior_log(action_type);

CREATE TABLE IF NOT EXISTS student_profile (
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
    status VARCHAR(32) DEFAULT 'normal',
    consecutive_correct INTEGER DEFAULT 0,
    recent_answers VARCHAR(255) DEFAULT '',
    recent_scores VARCHAR(255) DEFAULT '',
    last_activity_date TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_student_profile
    ON student_profile(student_no, course_code);

CREATE TABLE IF NOT EXISTS competency_score (
    id VARCHAR(36) PRIMARY KEY,
    student_no VARCHAR(64) NOT NULL,
    course_code VARCHAR(64) NOT NULL,
    ability_point_id VARCHAR(64),
    ability_point_name VARCHAR(128),
    score INTEGER,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_competency_student_ability
    ON competency_score(student_no, course_code, ability_point_id);

CREATE TABLE IF NOT EXISTS recommendation (
    id VARCHAR(36) PRIMARY KEY,
    student_no VARCHAR(64) NOT NULL,
    course_code VARCHAR(64) NOT NULL,
    type VARCHAR(32),
    target_id VARCHAR(64),
    target_name VARCHAR(256),
    reason TEXT,
    priority INTEGER DEFAULT 0,
    feedback VARCHAR(16),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_recommendation_student_course ON recommendation(student_no, course_code);

CREATE TABLE IF NOT EXISTS achievement (
    id VARCHAR(36) PRIMARY KEY,
    student_no VARCHAR(64) NOT NULL,
    course_code VARCHAR(64) NOT NULL,
    achievement_type VARCHAR(32),
    badge_code VARCHAR(64),
    name VARCHAR(128),
    description VARCHAR(512),
    earned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    metadata TEXT
);
CREATE INDEX IF NOT EXISTS idx_achievement_student_course ON achievement(student_no, course_code);
CREATE UNIQUE INDEX IF NOT EXISTS uk_achievement_student_badge
    ON achievement(student_no, course_code, badge_code);

CREATE TABLE IF NOT EXISTS competency_score_history (
    id VARCHAR(36) PRIMARY KEY,
    student_no VARCHAR(64) NOT NULL,
    course_code VARCHAR(64) NOT NULL,
    ability_point_id VARCHAR(64),
    old_score INTEGER,
    new_score INTEGER,
    change_reason VARCHAR(128),
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_competency_history_student_course
    ON competency_score_history(student_no, course_code);

CREATE TABLE IF NOT EXISTS growth_history (
    id VARCHAR(36) PRIMARY KEY,
    student_no VARCHAR(64) NOT NULL,
    course_code VARCHAR(64) NOT NULL,
    amount INTEGER,
    type VARCHAR(32),
    source VARCHAR(64),
    source_id VARCHAR(64),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_growth_history_student_course ON growth_history(student_no, course_code);

CREATE TABLE IF NOT EXISTS profile_projection_ledger (
    id VARCHAR(64) PRIMARY KEY,
    student_no VARCHAR(64) NOT NULL,
    course_code VARCHAR(64) NOT NULL,
    source_type VARCHAR(64) NOT NULL,
    source_id VARCHAR(64) NOT NULL,
    projection_type VARCHAR(64) NOT NULL,
    applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_profile_projection_source
    ON profile_projection_ledger(source_type, source_id, projection_type);
CREATE INDEX IF NOT EXISTS idx_profile_projection_student_course
    ON profile_projection_ledger(student_no, course_code, applied_at);

CREATE TABLE IF NOT EXISTS analytics_class (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    course_id VARCHAR(64),
    teacher_id VARCHAR(64),
    semester VARCHAR(32),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_analytics_class_teacher ON analytics_class(teacher_id);
CREATE INDEX IF NOT EXISTS idx_analytics_class_course ON analytics_class(course_id);

CREATE TABLE IF NOT EXISTS analytics_class_student (
    id VARCHAR(64) PRIMARY KEY DEFAULT CAST(nextval('analytics_class_student_id_seq') AS VARCHAR(64)),
    class_id VARCHAR(36) NOT NULL,
    student_id VARCHAR(64) NOT NULL,
    enrolled_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_class_student ON analytics_class_student(class_id, student_id);

CREATE TABLE IF NOT EXISTS analytics_risk_alert (
    id VARCHAR(36) PRIMARY KEY,
    student_id VARCHAR(64) NOT NULL,
    course_id VARCHAR(64),
    risk_type VARCHAR(32) NOT NULL,
    risk_level VARCHAR(16) NOT NULL,
    detail TEXT,
    status VARCHAR(16) DEFAULT 'active',
    created_at TIMESTAMP,
    resolved_at TIMESTAMP,
    resolved_by VARCHAR(64)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_active_alert ON analytics_risk_alert(student_id, risk_type, status);
CREATE INDEX IF NOT EXISTS idx_risk_alert_student ON analytics_risk_alert(student_id);

CREATE TABLE IF NOT EXISTS analytics_report (
    id VARCHAR(36) PRIMARY KEY,
    class_id VARCHAR(36) NOT NULL,
    report_type VARCHAR(32) NOT NULL,
    data_json TEXT,
    generated_at TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_analytics_report_class ON analytics_report(class_id);

CREATE TABLE IF NOT EXISTS analytics_teaching_suggestion (
    id VARCHAR(36) PRIMARY KEY,
    class_id VARCHAR(36),
    course_id VARCHAR(64),
    suggestion_type VARCHAR(32),
    content TEXT,
    target_type VARCHAR(16),
    target_id VARCHAR(64),
    urgency VARCHAR(16),
    based_on TEXT,
    generated_at TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_teaching_suggestion_class ON analytics_teaching_suggestion(class_id);
CREATE INDEX IF NOT EXISTS idx_teaching_suggestion_course ON analytics_teaching_suggestion(course_id);

CREATE TABLE IF NOT EXISTS student_tower_run (
    run_id VARCHAR(64) PRIMARY KEY,
    student_no VARCHAR(64) NOT NULL,
    course_code VARCHAR(64) NOT NULL,
    version INTEGER DEFAULT 1,
    status VARCHAR(32) DEFAULT 'active',
    route_source VARCHAR(32) DEFAULT 'rule',
    current_node_id VARCHAR(64),
    ai_snapshot_json TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_tower_run_student_course
ON student_tower_run(student_no, course_code, status);

CREATE TABLE IF NOT EXISTS student_tower_node (
    node_id VARCHAR(64) PRIMARY KEY,
    run_id VARCHAR(64) NOT NULL,
    node_order INTEGER NOT NULL,
    row_no INTEGER NOT NULL,
    col_no INTEGER NOT NULL,
    room_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) DEFAULT 'locked',
    knowledge_point_id VARCHAR(64),
    ability_point_id VARCHAR(64),
    parent_node_id VARCHAR(64),
    unlock_after_node_id VARCHAR(64),
    difficulty INTEGER DEFAULT 1,
    ai_reason TEXT,
    payload_json TEXT,
    cleared_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_tower_node_run_order
ON student_tower_node(run_id, node_order);
CREATE INDEX IF NOT EXISTS idx_tower_node_unlock
ON student_tower_node(run_id, unlock_after_node_id);

CREATE TABLE IF NOT EXISTS student_tower_attempt (
    attempt_id VARCHAR(64) PRIMARY KEY,
    run_id VARCHAR(64) NOT NULL,
    node_id VARCHAR(64) NOT NULL,
    student_no VARCHAR(64) NOT NULL,
    course_code VARCHAR(64) NOT NULL,
    room_type VARCHAR(32) NOT NULL,
    result VARCHAR(32) NOT NULL,
    correct_rate NUMERIC(5, 2),
    hp_left INTEGER,
    answer_summary_json TEXT,
    ai_report_json TEXT,
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_tower_attempt_node
ON student_tower_attempt(node_id, finished_at);

CREATE TABLE IF NOT EXISTS tower_run_inventory (
    id VARCHAR(64) PRIMARY KEY,
    run_id VARCHAR(64) NOT NULL,
    student_no VARCHAR(64) NOT NULL,
    item_code VARCHAR(64) NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 0,
    version INTEGER NOT NULL DEFAULT 1,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_tower_inventory_item
ON tower_run_inventory(run_id, item_code);

CREATE TABLE IF NOT EXISTS tower_action_log (
    action_id VARCHAR(64) PRIMARY KEY,
    run_id VARCHAR(64) NOT NULL,
    node_id VARCHAR(64),
    student_no VARCHAR(64) NOT NULL,
    action_type VARCHAR(64) NOT NULL,
    target_id VARCHAR(64),
    result_json TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_tower_action_run
ON tower_action_log(run_id, created_at);

CREATE TABLE IF NOT EXISTS tower_node_option (
    option_id VARCHAR(64) PRIMARY KEY,
    run_id VARCHAR(64) NOT NULL,
    node_id VARCHAR(64) NOT NULL,
    option_kind VARCHAR(32) NOT NULL,
    option_code VARCHAR(64) NOT NULL,
    option_snapshot_json TEXT NOT NULL,
    selected BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    selected_at TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_tower_node_option_code
ON tower_node_option(run_id, node_id, option_code);
CREATE INDEX IF NOT EXISTS idx_tower_node_option_node
ON tower_node_option(run_id, node_id, selected);

CREATE TABLE IF NOT EXISTS student_ability_delta_log (
    id VARCHAR(64) PRIMARY KEY,
    student_no VARCHAR(64) NOT NULL,
    course_code VARCHAR(64) NOT NULL,
    run_id VARCHAR(64),
    node_id VARCHAR(64),
    knowledge_point_id VARCHAR(64),
    ability_point_id VARCHAR(64),
    delta_score INTEGER DEFAULT 0,
    before_score INTEGER,
    after_score INTEGER,
    reason TEXT,
    ai_summary TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_ability_delta_student_course
ON student_ability_delta_log(student_no, course_code, created_at);

CREATE TABLE IF NOT EXISTS student_tower_question_pack (
    pack_id VARCHAR(64) PRIMARY KEY,
    run_id VARCHAR(64) NOT NULL,
    node_id VARCHAR(64) NOT NULL,
    student_no VARCHAR(64) NOT NULL,
    course_code VARCHAR(64) NOT NULL,
    mode VARCHAR(32) NOT NULL,
    question_ids_json TEXT NOT NULL,
    source VARCHAR(32) DEFAULT 'rule',
    strategy_json TEXT,
    ai_reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_tower_question_pack_node
ON student_tower_question_pack(run_id, node_id, mode);

-- Performance indexes for login/register and 200-concurrency student flows.
CREATE INDEX IF NOT EXISTS idx_student_class_name ON student(class_name);

CREATE INDEX IF NOT EXISTS idx_task_assignment_student_course_status
    ON task_assignment(student_no, course_code, status, assigned_at DESC);
CREATE INDEX IF NOT EXISTS idx_task_assignment_task_student_status
    ON task_assignment(task_no, student_no, status);

CREATE INDEX IF NOT EXISTS idx_task_submission_student_task_status_time
    ON task_submission(student_no, task_no, status, submit_time DESC);
CREATE INDEX IF NOT EXISTS idx_task_submission_task_status_time
    ON task_submission(task_no, status, submit_time DESC);
CREATE INDEX IF NOT EXISTS idx_task_submission_task_student_attempt_time
    ON task_submission(task_no, student_no, status, attempt_number DESC, submit_time DESC);

CREATE INDEX IF NOT EXISTS idx_submission_answer_student_filters
    ON submission_answer(student_no, task_no, knowledge_point_id, question_type, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_submission_answer_task_question
    ON submission_answer(task_no, question_id);
CREATE INDEX IF NOT EXISTS idx_submission_ai_review_submission_time
    ON submission_ai_review(submission_id, create_time DESC);

CREATE INDEX IF NOT EXISTS idx_behavior_user_created
    ON learning_behavior_log(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_behavior_task_created
    ON learning_behavior_log(task_no, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_behavior_filter_created
    ON learning_behavior_log(user_type, action_type, resource_type, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_competency_student_course
    ON competency_score(student_no, course_code);
CREATE INDEX IF NOT EXISTS idx_achievement_student_course_type
    ON achievement(student_no, course_code, achievement_type);
CREATE INDEX IF NOT EXISTS idx_student_profile_course_exp
    ON student_profile(course_code, exp DESC);
CREATE INDEX IF NOT EXISTS idx_student_profile_course_coins
    ON student_profile(course_code, coins DESC);

CREATE INDEX IF NOT EXISTS idx_learning_task_course_status_type_lesson
    ON learning_task(course_code, status, task_type, lesson_no, task_no);
CREATE INDEX IF NOT EXISTS idx_question_course_filter
    ON question(course_code, lesson_no, type, difficulty, knowledge_point_id);
CREATE INDEX IF NOT EXISTS idx_course_resource_course_filter
    ON course_resource(course_code, resource_type, knowledge_point_id, uploaded_at DESC);
CREATE INDEX IF NOT EXISTS idx_analytics_class_student_student
    ON analytics_class_student(student_id, class_id);
CREATE INDEX IF NOT EXISTS idx_risk_alert_student_status_type
    ON analytics_risk_alert(student_id, status, risk_type, created_at DESC);

CREATE UNIQUE INDEX IF NOT EXISTS uk_tower_active_run
    ON student_tower_run(student_no, course_code)
    WHERE status = 'active';
CREATE UNIQUE INDEX IF NOT EXISTS uk_tower_pack_run_node_mode
    ON student_tower_question_pack(run_id, node_id, mode);
CREATE INDEX IF NOT EXISTS idx_tower_attempt_student_course_time
    ON student_tower_attempt(student_no, course_code, finished_at DESC);
CREATE INDEX IF NOT EXISTS idx_tower_attempt_run_node
    ON student_tower_attempt(run_id, node_id, finished_at DESC);
CREATE INDEX IF NOT EXISTS idx_ability_delta_student_course_run_node
    ON student_ability_delta_log(student_no, course_code, run_id, node_id, created_at DESC);
