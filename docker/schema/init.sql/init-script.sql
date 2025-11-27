-- =============================================
-- JungleBook Database Initialization Script
-- =============================================

-- 데이터베이스 생성 (이미 docker-compose에서 생성됨)
-- CREATE DATABASE IF NOT EXISTS junglebook CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- USE junglebook;

-- =============================================
-- 1. 회원 관련 테이블
-- =============================================

-- 회원 테이블
CREATE TABLE IF NOT EXISTS member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    birth VARCHAR(255) NOT NULL,
    phone_number VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    sex ENUM('M', 'F') NOT NULL,
    ideology ENUM('C', 'L', 'M', 'N') NOT NULL,
    login_id VARCHAR(255) UNIQUE NOT NULL,
    nickname VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    profile_image VARCHAR(255),
    delete_yn INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    social_provider ENUM('KAKAO', 'NAVER'),
    social_provider_id VARCHAR(255),
    member_type ENUM('REGULAR', 'SOCIAL') NOT NULL DEFAULT 'REGULAR',
    
    INDEX idx_email (email),
    INDEX idx_login_id (login_id),
    INDEX idx_nickname (nickname),
    INDEX idx_social_provider (social_provider, social_provider_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 회원 캠프 히스토리 테이블
CREATE TABLE IF NOT EXISTS member_camp_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    camp INT NOT NULL COMMENT '0=C(보수), 1=L(진보), 2=M(중도), 3=N(없음)',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    
    FOREIGN KEY (member_id) REFERENCES member(id) ON DELETE CASCADE,
    INDEX idx_member_id (member_id),
    INDEX idx_camp (camp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- 2. 게시판 관련 테이블
-- =============================================

-- 게시판 테이블
CREATE TABLE IF NOT EXISTS board (
    id INT AUTO_INCREMENT PRIMARY KEY,
    use_yn BOOLEAN NOT NULL DEFAULT TRUE,
    type INT,
    name VARCHAR(100) NOT NULL,
    created_dt DATETIME NOT NULL,
    updated_dt DATETIME NOT NULL,
    
    INDEX idx_use_yn (use_yn)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 게시글 테이블
CREATE TABLE IF NOT EXISTS post (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    board_id INT NOT NULL,
    seq_no BIGINT,
    user_id BIGINT,
    notice_yn BOOLEAN NOT NULL DEFAULT FALSE,
    use_yn BOOLEAN NOT NULL DEFAULT TRUE,
    file_yn BOOLEAN NOT NULL DEFAULT FALSE,
    nickname VARCHAR(50),
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    content_html TEXT,
    view_cnt INT NOT NULL DEFAULT 0,
    like_cnt INT NOT NULL DEFAULT 0,
    dislike_cnt INT NOT NULL DEFAULT 0,
    reply_cnt INT NOT NULL DEFAULT 0,
    created_dt DATETIME NOT NULL,
    updated_dt DATETIME NOT NULL,
    
    FOREIGN KEY (board_id) REFERENCES board(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES member(id) ON DELETE SET NULL,
    INDEX idx_board_id (board_id),
    INDEX idx_user_id (user_id),
    INDEX idx_notice_yn (notice_yn),
    INDEX idx_use_yn (use_yn),
    INDEX idx_created_dt (created_dt),
    INDEX idx_view_cnt (view_cnt),
    INDEX idx_like_cnt (like_cnt)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 게시글 댓글 테이블
CREATE TABLE IF NOT EXISTS post_reply (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    board_id INT,
    post_id BIGINT NOT NULL,
    pid BIGINT,
    user_id BIGINT,
    use_yn BOOLEAN NOT NULL DEFAULT TRUE,
    file_yn BOOLEAN NOT NULL DEFAULT FALSE,
    nickname VARCHAR(50),
    content TEXT NOT NULL,
    content_html TEXT,
    like_cnt INT NOT NULL DEFAULT 0,
    dislike_cnt INT NOT NULL DEFAULT 0,
    created_dt DATETIME NOT NULL,
    updated_dt DATETIME NOT NULL,
    
    FOREIGN KEY (board_id) REFERENCES board(id) ON DELETE CASCADE,
    FOREIGN KEY (post_id) REFERENCES post(id) ON DELETE CASCADE,
    FOREIGN KEY (pid) REFERENCES post_reply(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES member(id) ON DELETE SET NULL,
    INDEX idx_post_id (post_id),
    INDEX idx_pid (pid),
    INDEX idx_user_id (user_id),
    INDEX idx_use_yn (use_yn),
    INDEX idx_created_dt (created_dt)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 게시글 파일 테이블
CREATE TABLE IF NOT EXISTS post_file (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    attach_yn BOOLEAN NOT NULL DEFAULT FALSE,
    ref_type INT,
    ref_id BIGINT,
    user_id BIGINT,
    file_type VARCHAR(50),
    file_size VARCHAR(20),
    file_name VARCHAR(255),
    url VARCHAR(500),
    
    FOREIGN KEY (user_id) REFERENCES member(id) ON DELETE SET NULL,
    INDEX idx_ref_type_ref_id (ref_type, ref_id),
    INDEX idx_user_id (user_id),
    INDEX idx_attach_yn (attach_yn)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 게시글 카운트 히스토리 테이블
CREATE TABLE IF NOT EXISTS post_count_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ref_type ENUM('POST', 'REPLY') NOT NULL,
    ref_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    type ENUM('VIEW', 'LIKE', 'DISLIKE') NOT NULL,
    created_at DATETIME NOT NULL,
    
    FOREIGN KEY (user_id) REFERENCES member(id) ON DELETE CASCADE,
    INDEX idx_ref_type_ref_id (ref_type, ref_id),
    INDEX idx_user_id (user_id),
    INDEX idx_type (type),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- 3. 토론 관련 테이블
-- =============================================

-- 토론 주제 테이블
CREATE TABLE IF NOT EXISTS debate_topic (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    description_html TEXT,
    category ENUM('POLITICS', 'ECONOMY', 'SOCIETY', 'CULTURE', 'IT_SCIENCE', 'FOREIGN_AFFAIRS') NOT NULL,
    status ENUM('PREPARING', 'DEBATING', 'CLOSED', 'VOTING') NOT NULL,
    creator_id BIGINT,
    hot_yn BOOLEAN NOT NULL DEFAULT FALSE,
    active_yn BOOLEAN NOT NULL DEFAULT TRUE,
    start_date DATE,
    end_date DATE,
    argument_count INT NOT NULL DEFAULT 0,
    view_count INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    
    FOREIGN KEY (creator_id) REFERENCES member(id) ON DELETE SET NULL,
    INDEX idx_category (category),
    INDEX idx_status (status),
    INDEX idx_creator_id (creator_id),
    INDEX idx_hot_yn (hot_yn),
    INDEX idx_active_yn (active_yn),
    INDEX idx_created_at (created_at),
    INDEX idx_view_count (view_count),
    INDEX idx_argument_count (argument_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 토론 논증 테이블
CREATE TABLE IF NOT EXISTS debate_argument (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    topic_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    stance ENUM('PRO', 'CON', 'NEUTRAL') NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    content_html TEXT,
    author_nickname VARCHAR(50) NOT NULL,
    active_yn BOOLEAN NOT NULL DEFAULT TRUE,
    notice_yn BOOLEAN NOT NULL DEFAULT FALSE,
    file_yn BOOLEAN NOT NULL DEFAULT FALSE,
    view_count INT NOT NULL DEFAULT 0,
    support_count INT NOT NULL DEFAULT 0,
    oppose_count INT NOT NULL DEFAULT 0,
    reply_count INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    
    FOREIGN KEY (topic_id) REFERENCES debate_topic(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES member(id) ON DELETE CASCADE,
    INDEX idx_topic_created (topic_id, created_at),
    INDEX idx_stance_created (stance, created_at),
    INDEX idx_user_id (user_id),
    INDEX idx_active_yn (active_yn),
    INDEX idx_view_count (view_count),
    INDEX idx_support_count (support_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 토론 댓글 테이블
CREATE TABLE IF NOT EXISTS debate_reply (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    argument_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    parent_id BIGINT,
    depth INT NOT NULL DEFAULT 0,
    content TEXT NOT NULL,
    content_html TEXT,
    author_nickname VARCHAR(50) NOT NULL,
    active_yn BOOLEAN NOT NULL DEFAULT TRUE,
    file_yn BOOLEAN NOT NULL DEFAULT FALSE,
    support_count INT NOT NULL DEFAULT 0,
    oppose_count INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    
    FOREIGN KEY (argument_id) REFERENCES debate_argument(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES member(id) ON DELETE CASCADE,
    FOREIGN KEY (parent_id) REFERENCES debate_reply(id) ON DELETE CASCADE,
    INDEX idx_argument_created (argument_id, created_at),
    INDEX idx_parent_created (parent_id, created_at),
    INDEX idx_user_id (user_id),
    INDEX idx_active_yn (active_yn),
    INDEX idx_depth (depth)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 토론 투표 테이블
CREATE TABLE IF NOT EXISTS debate_vote (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    argument_id BIGINT,
    reply_id BIGINT,
    vote_type ENUM('UPVOTE', 'DOWNVOTE') NOT NULL,
    created_at DATETIME NOT NULL,
    
    FOREIGN KEY (member_id) REFERENCES member(id) ON DELETE CASCADE,
    FOREIGN KEY (argument_id) REFERENCES debate_argument(id) ON DELETE CASCADE,
    FOREIGN KEY (reply_id) REFERENCES debate_reply(id) ON DELETE CASCADE,
    UNIQUE KEY uk_vote_member_argument (member_id, argument_id, vote_type),
    UNIQUE KEY uk_vote_member_reply (member_id, reply_id, vote_type),
    INDEX idx_member_id (member_id),
    INDEX idx_argument_id (argument_id),
    INDEX idx_reply_id (reply_id),
    INDEX idx_vote_type (vote_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 토론 파일 테이블
CREATE TABLE IF NOT EXISTS debate_file (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reference_type ENUM('ARGUMENT', 'REPLY') NOT NULL,
    reference_id BIGINT NOT NULL,
    uploader_id BIGINT NOT NULL,
    original_name VARCHAR(255),
    file_size BIGINT NOT NULL DEFAULT 0,
    file_type VARCHAR(100),
    file_url VARCHAR(500) NOT NULL,
    active_yn BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    
    FOREIGN KEY (uploader_id) REFERENCES member(id) ON DELETE CASCADE,
    INDEX idx_reference (reference_type, reference_id),
    INDEX idx_uploader_id (uploader_id),
    INDEX idx_active_yn (active_yn),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- 4. 국회 관련 테이블
-- =============================================

-- 국회 법안 테이블
CREATE TABLE IF NOT EXISTS assembly_bill (
    bill_id VARCHAR(50) PRIMARY KEY,
    bill_no VARCHAR(100),
    bill_name VARCHAR(500),
    bill_kind VARCHAR(50),
    committee_name VARCHAR(100),
    proposer VARCHAR(200),
    propose_dt VARCHAR(10),
    vote_tcnt INT NOT NULL DEFAULT 0,
    yes_tcnt INT NOT NULL DEFAULT 0,
    no_tcnt INT NOT NULL DEFAULT 0,
    blank_tcnt INT NOT NULL DEFAULT 0,
    proc_result VARCHAR(50),
    link VARCHAR(1000),
    age INT NOT NULL DEFAULT 0,
    
    INDEX idx_bill_no (bill_no),
    INDEX idx_bill_name (bill_name),
    INDEX idx_committee_name (committee_name),
    INDEX idx_propose_dt (propose_dt),
    INDEX idx_age (age)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 국회 투표 테이블
CREATE TABLE IF NOT EXISTS assembly_vote (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bill_id VARCHAR(50) NOT NULL,
    memo_no VARCHAR(20),
    ko_name VARCHAR(50),
    cn_name VARCHAR(50),
    poly_cd VARCHAR(10),
    poly_nm VARCHAR(50),
    vote_date VARCHAR(10),
    vote_mod VARCHAR(10),
    poly_cnt INT NOT NULL DEFAULT 0,
    
    FOREIGN KEY (bill_id) REFERENCES assembly_bill(bill_id) ON DELETE CASCADE,
    INDEX idx_bill_id (bill_id),
    INDEX idx_ko_name (ko_name),
    INDEX idx_poly_cd (poly_cd),
    INDEX idx_vote_date (vote_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- 5. 신고 관련 테이블
-- =============================================

-- 신고 테이블
CREATE TABLE IF NOT EXISTS report (
    report_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ref_type INT,
    target_id BIGINT NOT NULL,
    type INT,
    message VARCHAR(1000),
    user_id BIGINT,
    created_dt DATETIME NOT NULL,
    
    FOREIGN KEY (user_id) REFERENCES member(id) ON DELETE SET NULL,
    INDEX idx_ref_type (ref_type),
    INDEX idx_target_id (target_id),
    INDEX idx_user_id (user_id),
    INDEX idx_created_dt (created_dt)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- 6. 초기 데이터 삽입
-- =============================================

-- 기본 게시판 데이터
INSERT INTO board (id, use_yn, type, name, created_dt, updated_dt) VALUES
(1, TRUE, 1, '공지사항', NOW(), NOW()),
(2, TRUE, 2, '자유게시판', NOW(), NOW()),
(3, TRUE, 3, '토론게시판', NOW(), NOW()),
(4, TRUE, 4, '정치게시판', NOW(), NOW()),
(5, TRUE, 5, '경제게시판', NOW(), NOW())
ON DUPLICATE KEY UPDATE
use_yn = VALUES(use_yn),
type = VALUES(type),
name = VALUES(name),
updated_dt = NOW();

-- =============================================
-- 7. 뷰 생성 (선택사항)
-- =============================================

-- 인기 토론 주제 뷰
CREATE OR REPLACE VIEW popular_debate_topics AS
SELECT 
    dt.id,
    dt.title,
    dt.category,
    dt.status,
    dt.hot_yn,
    dt.argument_count,
    dt.view_count,
    dt.created_at,
    m.nickname as creator_nickname
FROM debate_topic dt
LEFT JOIN member m ON dt.creator_id = m.id
WHERE dt.active_yn = TRUE
ORDER BY (dt.argument_count * 2 + dt.view_count) DESC;

-- 최근 활동 통계 뷰
CREATE OR REPLACE VIEW recent_activity_stats AS
SELECT 
    'POST' as type,
    COUNT(*) as count,
    DATE(created_dt) as activity_date
FROM post 
WHERE created_dt >= DATE_SUB(NOW(), INTERVAL 7 DAY)
GROUP BY DATE(created_dt)

UNION ALL

SELECT 
    'DEBATE_TOPIC' as type,
    COUNT(*) as count,
    DATE(created_at) as activity_date
FROM debate_topic 
WHERE created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)
GROUP BY DATE(created_at)

UNION ALL

SELECT 
    'DEBATE_ARGUMENT' as type,
    COUNT(*) as count,
    DATE(created_at) as activity_date
FROM debate_argument 
WHERE created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)
GROUP BY DATE(created_at);

-- =============================================
-- 8. 트리거 생성 (선택사항)
-- =============================================

-- 토론 주제의 논증 수 업데이트 트리거
DELIMITER //
CREATE TRIGGER tr_debate_argument_insert
AFTER INSERT ON debate_argument
FOR EACH ROW
BEGIN
    UPDATE debate_topic 
    SET argument_count = argument_count + 1 
    WHERE id = NEW.topic_id;
END//

CREATE TRIGGER tr_debate_argument_delete
AFTER UPDATE ON debate_argument
FOR EACH ROW
BEGIN
    IF NEW.active_yn = FALSE AND OLD.active_yn = TRUE THEN
        UPDATE debate_topic 
        SET argument_count = argument_count - 1 
        WHERE id = NEW.topic_id;
    END IF;
END//
DELIMITER ;

-- =============================================
-- 스크립트 완료
-- =============================================

-- 데이터베이스 설정 확인
SELECT 'Database initialization completed successfully!' as status;






