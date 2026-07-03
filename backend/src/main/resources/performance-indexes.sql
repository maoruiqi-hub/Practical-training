CREATE UNIQUE INDEX IF NOT EXISTS uk_student_username ON student(username);
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

CREATE UNIQUE INDEX IF NOT EXISTS uk_teacher_username ON teacher(username);
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
