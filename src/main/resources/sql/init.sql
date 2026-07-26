-- MySQL 8.0 初始化脚本。注意：脚本会重建 online_exam 数据库中的业务表。
CREATE DATABASE IF NOT EXISTS online_exam
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE online_exam;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS answer_record;
DROP TABLE IF EXISTS exam_result;
DROP TABLE IF EXISTS exam;
DROP TABLE IF EXISTS paper_question;
DROP TABLE IF EXISTS exam_paper;
DROP TABLE IF EXISTS question;
DROP TABLE IF EXISTS course;
DROP TABLE IF EXISTS student;
DROP TABLE IF EXISTS teacher;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE student (
    student_id VARCHAR(20) PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    class_name VARCHAR(50) NOT NULL,
    gender VARCHAR(10) NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE teacher (
    teacher_id VARCHAR(20) PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    department VARCHAR(100),
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE course (
    course_id VARCHAR(20) PRIMARY KEY,
    course_name VARCHAR(100) NOT NULL,
    teacher_id VARCHAR(20) NOT NULL,
    CONSTRAINT fk_course_teacher FOREIGN KEY (teacher_id)
        REFERENCES teacher (teacher_id) ON UPDATE CASCADE ON DELETE RESTRICT,
    INDEX idx_course_teacher (teacher_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE question (
    question_id INT AUTO_INCREMENT PRIMARY KEY,
    teacher_id VARCHAR(20) NOT NULL,
    question_type TINYINT NOT NULL,
    content VARCHAR(500) NOT NULL,
    option_a VARCHAR(255) NOT NULL,
    option_b VARCHAR(255) NOT NULL,
    option_c VARCHAR(255),
    option_d VARCHAR(255),
    correct_answer VARCHAR(10) NOT NULL,
    score DECIMAL(6,2) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_question_teacher FOREIGN KEY (teacher_id)
        REFERENCES teacher (teacher_id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_question_type CHECK (question_type BETWEEN 1 AND 3),
    CONSTRAINT chk_question_score CHECK (score > 0),
    INDEX idx_question_teacher (teacher_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE exam_paper (
    paper_id INT AUTO_INCREMENT PRIMARY KEY,
    paper_name VARCHAR(100) NOT NULL,
    total_score DECIMAL(7,2) NOT NULL DEFAULT 0,
    teacher_id VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_paper_teacher FOREIGN KEY (teacher_id)
        REFERENCES teacher (teacher_id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_paper_score CHECK (total_score >= 0),
    INDEX idx_paper_teacher (teacher_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE paper_question (
    paper_id INT NOT NULL,
    question_id INT NOT NULL,
    question_order INT NOT NULL,
    PRIMARY KEY (paper_id, question_id),
    UNIQUE KEY uk_paper_order (paper_id, question_order),
    CONSTRAINT fk_pq_paper FOREIGN KEY (paper_id)
        REFERENCES exam_paper (paper_id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_pq_question FOREIGN KEY (question_id)
        REFERENCES question (question_id) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE exam (
    exam_id INT AUTO_INCREMENT PRIMARY KEY,
    course_id VARCHAR(20) NOT NULL,
    paper_id INT NOT NULL,
    start_time DATETIME NOT NULL,
    duration INT NOT NULL,
    status TINYINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_exam_course FOREIGN KEY (course_id)
        REFERENCES course (course_id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_exam_paper FOREIGN KEY (paper_id)
        REFERENCES exam_paper (paper_id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_exam_duration CHECK (duration > 0),
    CONSTRAINT chk_exam_status CHECK (status BETWEEN 0 AND 2),
    INDEX idx_exam_status_time (status, start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE exam_result (
    student_id VARCHAR(20) NOT NULL,
    exam_id INT NOT NULL,
    total_score DECIMAL(7,2) NOT NULL DEFAULT 0,
    is_pass TINYINT NOT NULL DEFAULT 0,
    start_time DATETIME,
    submitted_at DATETIME,
    PRIMARY KEY (student_id, exam_id),
    CONSTRAINT fk_result_student FOREIGN KEY (student_id)
        REFERENCES student (student_id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_result_exam FOREIGN KEY (exam_id)
        REFERENCES exam (exam_id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT chk_result_score CHECK (total_score >= 0),
    INDEX idx_result_exam_submitted (exam_id, submitted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE answer_record (
    student_id VARCHAR(20) NOT NULL,
    exam_id INT NOT NULL,
    question_id INT NOT NULL,
    student_answer VARCHAR(10) NOT NULL,
    PRIMARY KEY (student_id, exam_id, question_id),
    CONSTRAINT fk_answer_result FOREIGN KEY (student_id, exam_id)
        REFERENCES exam_result (student_id, exam_id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_answer_question FOREIGN KEY (question_id)
        REFERENCES question (question_id) ON UPDATE CASCADE ON DELETE RESTRICT,
    INDEX idx_answer_exam (exam_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 演示账号密码均为 123456，数据库保存 PBKDF2 哈希。
INSERT INTO teacher (teacher_id, name, department, password, email) VALUES
('T001', '王老师', '计算机学院',
 'pbkdf2$120000$L5auguMLTo/d2q9y3O+WfA==$ce33TSlICePjxsz/rpieRF1PTzScjdND+Q7r1YGR/XA=',
 'teacher@example.com');

INSERT INTO student (student_id, name, class_name, gender, password, email) VALUES
('S001', '张同学', '计算机2401', '男',
 'pbkdf2$120000$BNVLNs9oGO+XOc1DNgztzA==$EUpYEh82ApMufdiXyArR9Nl6Q2Ugep8LfAzNQYjpD5k=',
 'student@example.com');

INSERT INTO course (course_id, course_name, teacher_id) VALUES
('C001', 'Java Web 应用开发', 'T001');

INSERT INTO question
(question_id, teacher_id, question_type, content, option_a, option_b, option_c, option_d, correct_answer, score) VALUES
(1, 'T001', 1, 'Spring Boot 默认使用哪种配置文件名称？', 'application.properties', 'config.xml', 'spring.ini', 'boot.json', 'A', 10),
(2, 'T001', 2, '下列哪些属于 HTTP 请求方法？', 'GET', 'POST', 'TABLE', 'COMMIT', 'AB', 10),
(3, 'T001', 3, 'MyBatis 可以使用注解编写 SQL。', '正确', '错误', NULL, NULL, 'A', 10),
(4, 'T001', 1, '事务的原子性表示什么？', '操作全部成功或全部失败', '数据永久保存', '查询必须加锁', '每次只能执行一条 SQL', 'A', 10);

INSERT INTO exam_paper (paper_id, paper_name, total_score, teacher_id) VALUES
(1, 'Java Web 基础测试', 40, 'T001');

INSERT INTO paper_question (paper_id, question_id, question_order) VALUES
(1, 1, 1), (1, 2, 2), (1, 3, 3), (1, 4, 4);

INSERT INTO exam (exam_id, course_id, paper_id, start_time, duration, status) VALUES
(1, 'C001', 1, '2026-01-01 09:00:00', 60, 1);
