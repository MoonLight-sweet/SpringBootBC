CREATE TABLE IF NOT EXISTS app_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    email VARCHAR(120) NOT NULL UNIQUE,
    display_name VARCHAR(80),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS wrong_question (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    language VARCHAR(50) NOT NULL,
    code TEXT NOT NULL,
    error_message TEXT,
    actual_output TEXT,
    expected_output TEXT,
    user_note TEXT,
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
    error_location TEXT NOT NULL,
    error_reason TEXT NOT NULL,
    suggestion TEXT NOT NULL,
    correct_code TEXT NOT NULL,
    knowledge_points TEXT NOT NULL,
    learning_advice TEXT NOT NULL,
    model_name VARCHAR(100),
    status TINYINT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    KEY idx_analysis_question (wrong_question_id),
    CONSTRAINT fk_analysis_question FOREIGN KEY (wrong_question_id) REFERENCES wrong_question(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS knowledge_point (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
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
    prompt TEXT,
    response TEXT,
    status TINYINT,
    error_message TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    KEY idx_record_question (wrong_question_id),
    CONSTRAINT fk_record_question FOREIGN KEY (wrong_question_id) REFERENCES wrong_question(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
