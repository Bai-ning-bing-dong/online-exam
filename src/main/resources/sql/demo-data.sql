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
