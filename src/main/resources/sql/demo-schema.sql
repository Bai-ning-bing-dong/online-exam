DROP TABLE IF EXISTS answer_record;
DROP TABLE IF EXISTS exam_result;
DROP TABLE IF EXISTS exam;
DROP TABLE IF EXISTS paper_question;
DROP TABLE IF EXISTS exam_paper;
DROP TABLE IF EXISTS question;
DROP TABLE IF EXISTS course;
DROP TABLE IF EXISTS student;
DROP TABLE IF EXISTS teacher;

CREATE TABLE student (
    student_id VARCHAR(20) PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    class_name VARCHAR(50) NOT NULL,
    gender VARCHAR(10) NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL
);
CREATE TABLE teacher (
    teacher_id VARCHAR(20) PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    department VARCHAR(100),
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL
);
CREATE TABLE course (
    course_id VARCHAR(20) PRIMARY KEY,
    course_name VARCHAR(100) NOT NULL,
    teacher_id VARCHAR(20) NOT NULL,
    FOREIGN KEY (teacher_id) REFERENCES teacher(teacher_id)
);
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
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    FOREIGN KEY (teacher_id) REFERENCES teacher(teacher_id)
);
CREATE TABLE exam_paper (
    paper_id INT AUTO_INCREMENT PRIMARY KEY,
    paper_name VARCHAR(100) NOT NULL,
    total_score DECIMAL(7,2) DEFAULT 0 NOT NULL,
    teacher_id VARCHAR(20) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    FOREIGN KEY (teacher_id) REFERENCES teacher(teacher_id)
);
CREATE TABLE paper_question (
    paper_id INT NOT NULL,
    question_id INT NOT NULL,
    question_order INT NOT NULL,
    PRIMARY KEY (paper_id, question_id),
    UNIQUE (paper_id, question_order),
    FOREIGN KEY (paper_id) REFERENCES exam_paper(paper_id),
    FOREIGN KEY (question_id) REFERENCES question(question_id)
);
CREATE TABLE exam (
    exam_id INT AUTO_INCREMENT PRIMARY KEY,
    course_id VARCHAR(20) NOT NULL,
    paper_id INT NOT NULL,
    start_time DATETIME NOT NULL,
    duration INT NOT NULL,
    status TINYINT DEFAULT 0 NOT NULL,
    FOREIGN KEY (course_id) REFERENCES course(course_id),
    FOREIGN KEY (paper_id) REFERENCES exam_paper(paper_id)
);
CREATE TABLE exam_result (
    student_id VARCHAR(20) NOT NULL,
    exam_id INT NOT NULL,
    total_score DECIMAL(7,2) DEFAULT 0 NOT NULL,
    is_pass TINYINT DEFAULT 0 NOT NULL,
    start_time DATETIME,
    submitted_at DATETIME,
    PRIMARY KEY (student_id, exam_id),
    FOREIGN KEY (student_id) REFERENCES student(student_id),
    FOREIGN KEY (exam_id) REFERENCES exam(exam_id)
);
CREATE TABLE answer_record (
    student_id VARCHAR(20) NOT NULL,
    exam_id INT NOT NULL,
    question_id INT NOT NULL,
    student_answer VARCHAR(10) NOT NULL,
    PRIMARY KEY (student_id, exam_id, question_id),
    FOREIGN KEY (student_id, exam_id) REFERENCES exam_result(student_id, exam_id),
    FOREIGN KEY (question_id) REFERENCES question(question_id)
);
