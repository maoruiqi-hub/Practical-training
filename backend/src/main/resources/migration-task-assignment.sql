CREATE SEQUENCE IF NOT EXISTS task_assignment_id_seq START WITH 1000 INCREMENT BY 1;

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

DROP INDEX IF EXISTS idx_learning_task_target_student;
ALTER TABLE learning_task DROP COLUMN IF EXISTS target_student_no;

-- Backfill personalized assignments from legacy submission records.
-- Old data allowed students to submit public tasks without an assignment row.
-- The new model requires task_assignment as the source of truth for progress.
INSERT INTO task_assignment (
    task_no,
    course_code,
    student_no,
    assigned_by,
    assigned_at,
    status,
    note
)
SELECT
    s.task_no,
    t.course_code,
    s.student_no,
    NULL AS assigned_by,
    COALESCE(MIN(s.submit_time), CURRENT_TIMESTAMP) AS assigned_at,
    CASE
        WHEN SUM(CASE WHEN s.status = 'graded' THEN 1 ELSE 0 END) > 0 THEN 'completed'
        ELSE 'submitted'
    END AS status,
    '由历史提交记录迁移生成' AS note
FROM task_submission s
JOIN learning_task t ON t.task_no = s.task_no
WHERE s.task_no IS NOT NULL
  AND s.student_no IS NOT NULL
  AND COALESCE(s.status, '') <> 'superseded'
  AND NOT EXISTS (
      SELECT 1
      FROM task_assignment a
      WHERE a.task_no = s.task_no
        AND a.student_no = s.student_no
  )
GROUP BY s.task_no, t.course_code, s.student_no;
