-- Repair and complete String-ID migration for KingbaseES.
-- Kingbase may drop the identity sequence when DROP IDENTITY runs, so sequences are created after that step.

ALTER TABLE student ALTER COLUMN student_no DROP IDENTITY IF EXISTS;
CREATE SEQUENCE IF NOT EXISTS student_student_no_seq;
ALTER TABLE student ALTER COLUMN student_no TYPE VARCHAR(64) USING student_no::VARCHAR(64);
ALTER TABLE student ALTER COLUMN student_no SET DEFAULT nextval('student_student_no_seq'::regclass)::text;
SELECT setval('student_student_no_seq'::regclass, COALESCE((SELECT MAX(student_no::INTEGER) FROM student WHERE student_no ~ '^[0-9]+$'), 0) + 1, false);

ALTER TABLE teacher ALTER COLUMN teacher_no DROP IDENTITY IF EXISTS;
CREATE SEQUENCE IF NOT EXISTS teacher_teacher_no_seq;
ALTER TABLE teacher ALTER COLUMN teacher_no TYPE VARCHAR(64) USING teacher_no::VARCHAR(64);
ALTER TABLE teacher ALTER COLUMN teacher_no SET DEFAULT nextval('teacher_teacher_no_seq'::regclass)::text;
SELECT setval('teacher_teacher_no_seq'::regclass, COALESCE((SELECT MAX(teacher_no::INTEGER) FROM teacher WHERE teacher_no ~ '^[0-9]+$'), 0) + 1, false);

ALTER TABLE course ALTER COLUMN course_code DROP IDENTITY IF EXISTS;
CREATE SEQUENCE IF NOT EXISTS course_course_code_seq;
ALTER TABLE course ALTER COLUMN course_code TYPE VARCHAR(64) USING course_code::VARCHAR(64);
ALTER TABLE course ALTER COLUMN course_code SET DEFAULT nextval('course_course_code_seq'::regclass)::text;
SELECT setval('course_course_code_seq'::regclass, COALESCE((SELECT MAX(course_code::INTEGER) FROM course WHERE course_code ~ '^[0-9]+$'), 0) + 1, false);

ALTER TABLE lesson ALTER COLUMN lesson_no DROP IDENTITY IF EXISTS;
CREATE SEQUENCE IF NOT EXISTS lesson_lesson_no_seq;
ALTER TABLE lesson ALTER COLUMN lesson_no TYPE VARCHAR(64) USING lesson_no::VARCHAR(64);
ALTER TABLE lesson ALTER COLUMN lesson_no SET DEFAULT nextval('lesson_lesson_no_seq'::regclass)::text;
SELECT setval('lesson_lesson_no_seq'::regclass, COALESCE((SELECT MAX(lesson_no::INTEGER) FROM lesson WHERE lesson_no ~ '^[0-9]+$'), 0) + 1, false);

ALTER TABLE knowledge_point ALTER COLUMN knowledge_point_id DROP IDENTITY IF EXISTS;
CREATE SEQUENCE IF NOT EXISTS knowledge_point_knowledge_point_id_seq;
ALTER TABLE knowledge_point ALTER COLUMN knowledge_point_id TYPE VARCHAR(64) USING knowledge_point_id::VARCHAR(64);
ALTER TABLE knowledge_point ALTER COLUMN knowledge_point_id SET DEFAULT nextval('knowledge_point_knowledge_point_id_seq'::regclass)::text;
SELECT setval('knowledge_point_knowledge_point_id_seq'::regclass, COALESCE((SELECT MAX(knowledge_point_id::INTEGER) FROM knowledge_point WHERE knowledge_point_id ~ '^[0-9]+$'), 0) + 1, false);

ALTER TABLE knowledge_relation ALTER COLUMN relation_id DROP IDENTITY IF EXISTS;
CREATE SEQUENCE IF NOT EXISTS knowledge_relation_relation_id_seq;
ALTER TABLE knowledge_relation ALTER COLUMN relation_id TYPE VARCHAR(64) USING relation_id::VARCHAR(64);
ALTER TABLE knowledge_relation ALTER COLUMN relation_id SET DEFAULT nextval('knowledge_relation_relation_id_seq'::regclass)::text;
SELECT setval('knowledge_relation_relation_id_seq'::regclass, COALESCE((SELECT MAX(relation_id::INTEGER) FROM knowledge_relation WHERE relation_id ~ '^[0-9]+$'), 0) + 1, false);

ALTER TABLE course_resource ALTER COLUMN resource_id DROP IDENTITY IF EXISTS;
CREATE SEQUENCE IF NOT EXISTS course_resource_resource_id_seq;
ALTER TABLE course_resource ALTER COLUMN resource_id TYPE VARCHAR(64) USING resource_id::VARCHAR(64);
ALTER TABLE course_resource ALTER COLUMN resource_id SET DEFAULT nextval('course_resource_resource_id_seq'::regclass)::text;
SELECT setval('course_resource_resource_id_seq'::regclass, COALESCE((SELECT MAX(resource_id::INTEGER) FROM course_resource WHERE resource_id ~ '^[0-9]+$'), 0) + 1, false);

ALTER TABLE ability_point ALTER COLUMN ability_point_id DROP IDENTITY IF EXISTS;
CREATE SEQUENCE IF NOT EXISTS ability_point_ability_point_id_seq;
ALTER TABLE ability_point ALTER COLUMN ability_point_id TYPE VARCHAR(64) USING ability_point_id::VARCHAR(64);
ALTER TABLE ability_point ALTER COLUMN ability_point_id SET DEFAULT nextval('ability_point_ability_point_id_seq'::regclass)::text;
SELECT setval('ability_point_ability_point_id_seq'::regclass, COALESCE((SELECT MAX(ability_point_id::INTEGER) FROM ability_point WHERE ability_point_id ~ '^[0-9]+$'), 0) + 1, false);

ALTER TABLE ability_knowledge_point ALTER COLUMN id DROP IDENTITY IF EXISTS;
CREATE SEQUENCE IF NOT EXISTS ability_knowledge_point_id_seq;
ALTER TABLE ability_knowledge_point ALTER COLUMN id TYPE VARCHAR(64) USING id::VARCHAR(64);
ALTER TABLE ability_knowledge_point ALTER COLUMN id SET DEFAULT nextval('ability_knowledge_point_id_seq'::regclass)::text;
SELECT setval('ability_knowledge_point_id_seq'::regclass, COALESCE((SELECT MAX(id::INTEGER) FROM ability_knowledge_point WHERE id ~ '^[0-9]+$'), 0) + 1, false);

ALTER TABLE knowledge_extraction_candidate ALTER COLUMN candidate_id DROP IDENTITY IF EXISTS;
CREATE SEQUENCE IF NOT EXISTS knowledge_extraction_candidate_candidate_id_seq;
ALTER TABLE knowledge_extraction_candidate ALTER COLUMN candidate_id TYPE VARCHAR(64) USING candidate_id::VARCHAR(64);
ALTER TABLE knowledge_extraction_candidate ALTER COLUMN candidate_id SET DEFAULT nextval('knowledge_extraction_candidate_candidate_id_seq'::regclass)::text;
SELECT setval('knowledge_extraction_candidate_candidate_id_seq'::regclass, COALESCE((SELECT MAX(candidate_id::INTEGER) FROM knowledge_extraction_candidate WHERE candidate_id ~ '^[0-9]+$'), 0) + 1, false);

ALTER TABLE learning_task ALTER COLUMN task_no DROP IDENTITY IF EXISTS;
CREATE SEQUENCE IF NOT EXISTS learning_task_task_no_seq;
ALTER TABLE learning_task ALTER COLUMN task_no TYPE VARCHAR(64) USING task_no::VARCHAR(64);
ALTER TABLE learning_task ALTER COLUMN task_no SET DEFAULT nextval('learning_task_task_no_seq'::regclass)::text;
SELECT setval('learning_task_task_no_seq'::regclass, COALESCE((SELECT MAX(task_no::INTEGER) FROM learning_task WHERE task_no ~ '^[0-9]+$'), 0) + 1, false);

ALTER TABLE task_submission ALTER COLUMN submission_id DROP IDENTITY IF EXISTS;
CREATE SEQUENCE IF NOT EXISTS task_submission_submission_id_seq;
ALTER TABLE task_submission ALTER COLUMN submission_id TYPE VARCHAR(64) USING submission_id::VARCHAR(64);
ALTER TABLE task_submission ALTER COLUMN submission_id SET DEFAULT nextval('task_submission_submission_id_seq'::regclass)::text;
SELECT setval('task_submission_submission_id_seq'::regclass, COALESCE((SELECT MAX(submission_id::INTEGER) FROM task_submission WHERE submission_id ~ '^[0-9]+$'), 0) + 1, false);

ALTER TABLE submission_ai_review ALTER COLUMN review_id DROP IDENTITY IF EXISTS;
CREATE SEQUENCE IF NOT EXISTS submission_ai_review_review_id_seq;
ALTER TABLE submission_ai_review ALTER COLUMN review_id TYPE VARCHAR(64) USING review_id::VARCHAR(64);
ALTER TABLE submission_ai_review ALTER COLUMN review_id SET DEFAULT nextval('submission_ai_review_review_id_seq'::regclass)::text;
SELECT setval('submission_ai_review_review_id_seq'::regclass, COALESCE((SELECT MAX(review_id::INTEGER) FROM submission_ai_review WHERE review_id ~ '^[0-9]+$'), 0) + 1, false);

ALTER TABLE submission_answer ALTER COLUMN id DROP IDENTITY IF EXISTS;
CREATE SEQUENCE IF NOT EXISTS submission_answer_id_seq;
ALTER TABLE submission_answer ALTER COLUMN id TYPE VARCHAR(64) USING id::VARCHAR(64);
ALTER TABLE submission_answer ALTER COLUMN id SET DEFAULT nextval('submission_answer_id_seq'::regclass)::text;
SELECT setval('submission_answer_id_seq'::regclass, COALESCE((SELECT MAX(id::INTEGER) FROM submission_answer WHERE id ~ '^[0-9]+$'), 0) + 1, false);

ALTER TABLE exam ALTER COLUMN exam_id DROP IDENTITY IF EXISTS;
CREATE SEQUENCE IF NOT EXISTS exam_exam_id_seq;
ALTER TABLE exam ALTER COLUMN exam_id TYPE VARCHAR(64) USING exam_id::VARCHAR(64);
ALTER TABLE exam ALTER COLUMN exam_id SET DEFAULT nextval('exam_exam_id_seq'::regclass)::text;
SELECT setval('exam_exam_id_seq'::regclass, COALESCE((SELECT MAX(exam_id::INTEGER) FROM exam WHERE exam_id ~ '^[0-9]+$'), 0) + 1, false);

ALTER TABLE exam_question ALTER COLUMN id DROP IDENTITY IF EXISTS;
CREATE SEQUENCE IF NOT EXISTS exam_question_id_seq;
ALTER TABLE exam_question ALTER COLUMN id TYPE VARCHAR(64) USING id::VARCHAR(64);
ALTER TABLE exam_question ALTER COLUMN id SET DEFAULT nextval('exam_question_id_seq'::regclass)::text;
SELECT setval('exam_question_id_seq'::regclass, COALESCE((SELECT MAX(id::INTEGER) FROM exam_question WHERE id ~ '^[0-9]+$'), 0) + 1, false);

ALTER TABLE question ALTER COLUMN question_id DROP IDENTITY IF EXISTS;
CREATE SEQUENCE IF NOT EXISTS question_question_id_seq;
ALTER TABLE question ALTER COLUMN question_id TYPE VARCHAR(64) USING question_id::VARCHAR(64);
ALTER TABLE question ALTER COLUMN question_id SET DEFAULT nextval('question_question_id_seq'::regclass)::text;
SELECT setval('question_question_id_seq'::regclass, COALESCE((SELECT MAX(question_id::INTEGER) FROM question WHERE question_id ~ '^[0-9]+$'), 0) + 1, false);

ALTER TABLE task_question ALTER COLUMN id DROP IDENTITY IF EXISTS;
CREATE SEQUENCE IF NOT EXISTS task_question_id_seq;
ALTER TABLE task_question ALTER COLUMN id TYPE VARCHAR(64) USING id::VARCHAR(64);
ALTER TABLE task_question ALTER COLUMN id SET DEFAULT nextval('task_question_id_seq'::regclass)::text;
SELECT setval('task_question_id_seq'::regclass, COALESCE((SELECT MAX(id::INTEGER) FROM task_question WHERE id ~ '^[0-9]+$'), 0) + 1, false);

ALTER TABLE learning_behavior_log ALTER COLUMN log_id DROP IDENTITY IF EXISTS;
CREATE SEQUENCE IF NOT EXISTS learning_behavior_log_log_id_seq;
ALTER TABLE learning_behavior_log ALTER COLUMN log_id TYPE VARCHAR(64) USING log_id::VARCHAR(64);
ALTER TABLE learning_behavior_log ALTER COLUMN log_id SET DEFAULT nextval('learning_behavior_log_log_id_seq'::regclass)::text;
SELECT setval('learning_behavior_log_log_id_seq'::regclass, COALESCE((SELECT MAX(log_id::INTEGER) FROM learning_behavior_log WHERE log_id ~ '^[0-9]+$'), 0) + 1, false);
