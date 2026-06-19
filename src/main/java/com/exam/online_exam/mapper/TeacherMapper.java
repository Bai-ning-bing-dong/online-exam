package com.exam.online_exam.mapper;

import com.exam.online_exam.entity.Teacher;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;

@Mapper
public interface TeacherMapper {

    @Select("SELECT * FROM teacher WHERE teacher_id=#{teacherId} AND password=#{password}")
    Teacher login(@Param("teacherId") String teacherId,
                  @Param("password") String password);

    @Select("SELECT q.question_id AS questionId, q.content, " +
            "CASE q.question_type WHEN 1 THEN '单选' WHEN 2 THEN '多选' WHEN 3 THEN '判断' END AS typeName, " +
            "q.correct_answer AS correctAnswer, q.score " +
            "FROM question q WHERE q.teacher_id=#{teacherId}")
    List<Map<String, Object>> getQuestionsByTeacher(String teacherId);

    @Select("SELECT e.exam_id AS examId, c.course_name AS courseName, " +
            "COUNT(r.student_id) AS totalCount, " +
            "MAX(r.total_score) AS maxScore, " +
            "MIN(r.total_score) AS minScore, " +
            "ROUND(AVG(r.total_score), 2) AS avgScore, " +
            "SUM(r.is_pass) AS passCount " +
            "FROM exam_result r " +
            "JOIN exam e ON r.exam_id = e.exam_id " +
            "JOIN course c ON e.course_id = c.course_id " +
            "JOIN exam_paper ep ON e.paper_id = ep.paper_id " +
            "WHERE ep.teacher_id = #{teacherId} " +
            "GROUP BY e.exam_id, c.course_name")
    List<Map<String, Object>> getExamStats(String teacherId);

    @Select("SELECT s.student_id AS studentId, s.name AS studentName, " +
            "s.class_name AS className, r.total_score AS totalScore, " +
            "CASE WHEN r.is_pass=1 THEN '及格' ELSE '不及格' END AS isPass, " +
            "r.submitted_at AS submittedAt " +
            "FROM exam_result r " +
            "JOIN student s ON r.student_id = s.student_id " +
            "WHERE r.exam_id = #{examId} " +
            "ORDER BY r.total_score DESC")
    List<Map<String, Object>> getStudentResultsByExam(int examId);

    // 查询教师的所有试卷
    @Select("SELECT paper_id AS paperId, paper_name AS paperName, " +
            "total_score AS totalScore, created_at AS createdAt " +
            "FROM exam_paper WHERE teacher_id = #{teacherId}")
    List<Map<String, Object>> getPapersByTeacher(String teacherId);

    // 查询试卷里已有的题目
    @Select("SELECT q.question_id AS questionId, q.content, " +
            "CASE q.question_type WHEN 1 THEN '单选' WHEN 2 THEN '多选' WHEN 3 THEN '判断' END AS typeName, " +
            "q.correct_answer AS correctAnswer, q.score, pq.question_order AS questionOrder " +
            "FROM paper_question pq " +
            "JOIN question q ON pq.question_id = q.question_id " +
            "WHERE pq.paper_id = #{paperId} ORDER BY pq.question_order")
    List<Map<String, Object>> getQuestionsByPaper(int paperId);

    // 查询题库中不在该试卷里的题目
    @Select("SELECT q.question_id AS questionId, q.content, " +
            "CASE q.question_type WHEN 1 THEN '单选' WHEN 2 THEN '多选' WHEN 3 THEN '判断' END AS typeName, " +
            "q.score " +
            "FROM question q " +
            "WHERE q.teacher_id = #{teacherId} " +
            "AND q.question_id NOT IN " +
            "(SELECT question_id FROM paper_question WHERE paper_id = #{paperId})")
    List<Map<String, Object>> getAvailableQuestions(@Param("teacherId") String teacherId,
                                                    @Param("paperId") int paperId);

    // 查询教师的所有考试安排
    @Select("SELECT e.exam_id AS examId, c.course_name AS courseName, " +
            "ep.paper_name AS paperName, e.start_time AS startTime, " +
            "e.duration AS duration, " +
            "CASE e.status WHEN 0 THEN '未开始' WHEN 1 THEN '进行中' WHEN 2 THEN '已结束' END AS statusName, " +
            "e.status AS status " +
            "FROM exam e " +
            "JOIN course c ON e.course_id = c.course_id " +
            "JOIN exam_paper ep ON e.paper_id = ep.paper_id " +
            "WHERE ep.teacher_id = #{teacherId} " +
            "ORDER BY e.exam_id DESC")
    List<Map<String, Object>> getExamsByTeacher(String teacherId);

    // 查询该教师的所有课程（用于创建考试时选课程）
    @Select("SELECT course_id AS courseId, course_name AS courseName " +
            "FROM course WHERE teacher_id = #{teacherId}")
    List<Map<String, Object>> getCoursesByTeacher(String teacherId);

    // 查询单道题目详情（用于回填修改表单）
    @Select("SELECT * FROM question WHERE question_id=#{questionId}")
    Map<String, Object> getQuestionById(int questionId);

    // 创建试卷
    @Insert("INSERT INTO exam_paper(paper_name, total_score, teacher_id) " +
            "VALUES(#{paperName}, 0, #{teacherId})")
    void createPaper(@Param("paperName") String paperName,
                     @Param("teacherId") String teacherId);

    // 创建考试安排
    @Insert("INSERT INTO exam(course_id, paper_id, start_time, duration, status) " +
            "VALUES(#{courseId}, #{paperId}, #{startTime}, #{duration}, 0)")
    void createExam(@Param("courseId") String courseId,
                    @Param("paperId") int paperId,
                    @Param("startTime") String startTime,
                    @Param("duration") int duration);


    // 把题目加入试卷
    @Insert("INSERT INTO paper_question(paper_id, question_id, question_order) " +
            "VALUES(#{paperId}, #{questionId}, " +
            "(SELECT IFNULL(MAX(question_order), 0)+1 FROM paper_question pq2 WHERE pq2.paper_id = #{paperId}))")
    void addQuestionToPaper(@Param("paperId") int paperId,
                            @Param("questionId") int questionId);

    // 更新试卷总分
    @Update("UPDATE exam_paper SET total_score = " +
            "(SELECT IFNULL(SUM(score), 0) FROM paper_question pq " +
            "JOIN question q ON pq.question_id = q.question_id " +
            "WHERE pq.paper_id = #{paperId}) " +
            "WHERE paper_id = #{paperId}")
    void updatePaperTotalScore(int paperId);

    // 修改考试状态
    @Update("UPDATE exam SET status=#{status} WHERE exam_id=#{examId}")
    void updateExamStatus(@Param("examId") int examId,
                          @Param("status") int status);

    @Update("UPDATE question SET content=#{content}, question_type=#{questionType}, " +
            "option_a=#{optionA}, option_b=#{optionB}, option_c=#{optionC}, option_d=#{optionD}, " +
            "correct_answer=#{correctAnswer}, score=#{score} " +
            "WHERE question_id=#{questionId}")
    void updateQuestion(@Param("questionId") int questionId,
                        @Param("content") String content,
                        @Param("questionType") int questionType,
                        @Param("optionA") String optionA,
                        @Param("optionB") String optionB,
                        @Param("optionC") String optionC,
                        @Param("optionD") String optionD,
                        @Param("correctAnswer") String correctAnswer,
                        @Param("score") double score);


    // 从试卷移除题目
    @Delete("DELETE FROM paper_question WHERE paper_id=#{paperId} AND question_id=#{questionId}")
    void removeQuestionFromPaper(@Param("paperId") int paperId,
                                 @Param("questionId") int questionId);

    // 删除题目（先删关联表再删题目）
    @Delete("DELETE FROM paper_question WHERE question_id=#{questionId}")
    void deletePaperQuestion(int questionId);

    @Delete("DELETE FROM question WHERE question_id=#{questionId}")
    void deleteQuestion(int questionId);

    @Insert("INSERT INTO question(teacher_id, question_type, content, " +
            "option_a, option_b, option_c, option_d, correct_answer, score) " +
            "VALUES(#{teacherId}, #{questionType}, #{content}, " +
            "#{optionA}, #{optionB}, #{optionC}, #{optionD}, #{correctAnswer}, #{score})")

    void addQuestion(@Param("teacherId") String teacherId,
                     @Param("questionType") int questionType,
                     @Param("content") String content,
                     @Param("optionA") String optionA,
                     @Param("optionB") String optionB,
                     @Param("optionC") String optionC,
                     @Param("optionD") String optionD,
                     @Param("correctAnswer") String correctAnswer,
                     @Param("score") double score);
}