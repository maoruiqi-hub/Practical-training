CREATE TABLE IF NOT EXISTS competency_point (
    competency_id VARCHAR(64) PRIMARY KEY,
    course_code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    description TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    sort_order INTEGER NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_competency_point_course_name
    ON competency_point(course_code, name);
CREATE INDEX IF NOT EXISTS idx_competency_point_course
    ON competency_point(course_code);

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
    CONSTRAINT fk_mapping_competency FOREIGN KEY (competency_id) REFERENCES competency_point(competency_id),
    CONSTRAINT fk_mapping_relation_course FOREIGN KEY (course_code) REFERENCES course(course_code)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_ability_competency_relation
    ON ability_point_competency_relation(course_code, ability_point_id, competency_id, matrix_version);
CREATE INDEX IF NOT EXISTS idx_ability_competency_course
    ON ability_point_competency_relation(course_code, matrix_version);

CREATE TABLE IF NOT EXISTS competency_task_observation (
    id VARCHAR(64) PRIMARY KEY,
    course_code VARCHAR(64) NOT NULL,
    task_no VARCHAR(64) NOT NULL,
    competency_id VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_observation_status CHECK (status IN ('active', 'inactive')),
    CONSTRAINT fk_observation_task FOREIGN KEY (task_no) REFERENCES learning_task(task_no),
    CONSTRAINT fk_observation_competency FOREIGN KEY (competency_id) REFERENCES competency_point(competency_id),
    CONSTRAINT fk_observation_course FOREIGN KEY (course_code) REFERENCES course(course_code)
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
    CONSTRAINT ck_mapping_version_samples CHECK (sample_count >= 0 AND validation_sample_count >= 0),
    CONSTRAINT fk_mapping_version_course FOREIGN KEY (course_code) REFERENCES course(course_code)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_ability_competency_matrix_version
    ON ability_competency_matrix_version(course_code, version);
CREATE UNIQUE INDEX IF NOT EXISTS uk_ability_competency_published_course
    ON ability_competency_matrix_version(course_code)
    WHERE status = 'published';
