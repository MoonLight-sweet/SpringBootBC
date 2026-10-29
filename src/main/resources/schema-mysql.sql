CREATE TABLE IF NOT EXISTS app_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    email VARCHAR(120) NOT NULL UNIQUE,
    display_name VARCHAR(80),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS password_reset_code (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    code_hash VARCHAR(100) NOT NULL,
    expire_time TIMESTAMP NOT NULL,
    used TINYINT(1) NOT NULL DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    KEY idx_reset_user_time (user_id, create_time),
    CONSTRAINT fk_reset_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS password_reset_attempt (
    reset_code_id BIGINT PRIMARY KEY,
    attempts INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_reset_attempt_code FOREIGN KEY (reset_code_id) REFERENCES password_reset_code(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS wrong_question (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description MEDIUMTEXT,
    language VARCHAR(50) NOT NULL,
    code MEDIUMTEXT NOT NULL,
    error_message MEDIUMTEXT,
    actual_output MEDIUMTEXT,
    expected_output MEDIUMTEXT,
    user_note MEDIUMTEXT,
    status TINYINT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_question_user (user_id),
    KEY idx_question_status (status),
    CONSTRAINT fk_question_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ai_analysis (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    wrong_question_id BIGINT NOT NULL,
    error_type VARCHAR(100) NOT NULL,
    error_location MEDIUMTEXT NOT NULL,
    error_reason MEDIUMTEXT NOT NULL,
    suggestion MEDIUMTEXT NOT NULL,
    correct_code MEDIUMTEXT NOT NULL,
    knowledge_points MEDIUMTEXT NOT NULL,
    learning_advice MEDIUMTEXT NOT NULL,
    model_name VARCHAR(100),
    input_snapshot MEDIUMTEXT,
    status TINYINT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    KEY idx_analysis_question (wrong_question_id),
    CONSTRAINT fk_analysis_question FOREIGN KEY (wrong_question_id) REFERENCES wrong_question(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS knowledge_point (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL UNIQUE,
    description MEDIUMTEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS wrong_question_knowledge (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    wrong_question_id BIGINT NOT NULL,
    knowledge_point_id BIGINT NOT NULL,
    CONSTRAINT uk_question_knowledge UNIQUE (wrong_question_id, knowledge_point_id),
    CONSTRAINT fk_wqk_question FOREIGN KEY (wrong_question_id) REFERENCES wrong_question(id) ON DELETE CASCADE,
    CONSTRAINT fk_wqk_knowledge FOREIGN KEY (knowledge_point_id) REFERENCES knowledge_point(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS analysis_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    wrong_question_id BIGINT NOT NULL,
    model_name VARCHAR(100),
    prompt MEDIUMTEXT,
    response MEDIUMTEXT,
    status TINYINT,
    error_message MEDIUMTEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    KEY idx_record_question (wrong_question_id),
    CONSTRAINT fk_record_question FOREIGN KEY (wrong_question_id) REFERENCES wrong_question(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS question_review (
    question_id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    state VARCHAR(20) NOT NULL DEFAULT 'todo',
    streak INT NOT NULL DEFAULT 0,
    review_count INT NOT NULL DEFAULT 0,
    next_review_at TIMESTAMP NULL,
    last_review_at TIMESTAMP NULL,
    KEY idx_review_due (user_id, state, next_review_at),
    CONSTRAINT fk_review_question FOREIGN KEY (question_id) REFERENCES wrong_question(id) ON DELETE CASCADE,
    CONSTRAINT fk_review_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS review_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    question_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    remembered TINYINT(1) NOT NULL,
    note VARCHAR(500),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_review_record_user (user_id, create_time),
    CONSTRAINT fk_review_record_question FOREIGN KEY (question_id) REFERENCES wrong_question(id) ON DELETE CASCADE,
    CONSTRAINT fk_review_record_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS analysis_feedback (
    analysis_id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    verdict VARCHAR(20) NOT NULL,
    comment VARCHAR(1000),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_feedback_user (user_id, verdict),
    CONSTRAINT fk_feedback_analysis FOREIGN KEY (analysis_id) REFERENCES ai_analysis(id) ON DELETE CASCADE,
    CONSTRAINT fk_feedback_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
