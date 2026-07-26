package com.exam.online_exam.mapper;

import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;

@Mapper
public interface ExamMapper {

    // 查询所有进行中的考试
    @Select("SELECT e.exam_id AS \"examId\", c.course_name AS \"courseName\", " +
            "e.start_time AS \"startTime\", e.duration AS duration " +
            "FROM exam e JOIN course c ON e.course_id = c.course_id " +
            "WHERE e.status = 1 AND e.start_time <= NOW() ORDER BY e.start_time")
    List<Map<String, Object>> getActiveExams();

    // 查询某场考试的所有题目
    @Select("SELECT q.question_id AS \"questionId\", q.content, q.question_type AS \"questionType\", " +
            "q.option_a AS \"optionA\", q.option_b AS \"optionB\", " +
            "q.option_c AS \"optionC\", q.option_d AS \"optionD\", q.score " +
            "FROM paper_question pq " +
            "JOIN question q ON pq.question_id = q.question_id " +
            "JOIN exam e ON e.paper_id = pq.paper_id " +
            "WHERE e.exam_id = #{examId} " +
            "ORDER BY pq.question_order")
    List<Map<String, Object>> getQuestionsByExamId(int examId);

    // 提交一道题的答案
    @Insert("INSERT INTO answer_record(student_id, exam_id, question_id, student_answer) " +
            "VALUES(#{studentId}, #{examId}, #{questionId}, #{answer}) " +
            "ON DUPLICATE KEY UPDATE student_answer = #{answer}")
    void submitAnswer(@Param("studentId") String studentId,
                      @Param("examId") int examId,
                      @Param("questionId") int questionId,
                      @Param("answer") String answer);

    // 检查是否已经参加过该考试
    @Select("SELECT COUNT(*) FROM exam_result " +
            "WHERE student_id=#{studentId} AND exam_id=#{examId} " +
            "AND submitted_at IS NOT NULL")
    int checkAlreadySubmitted(@Param("studentId") String studentId,
                              @Param("examId") int examId);

    // 查询考试时长
    @Select("SELECT duration FROM exam WHERE exam_id=#{examId}")
    int getExamDuration(int examId);

    // 记录开始答题时间
    @Insert("INSERT INTO exam_result(student_id, exam_id, total_score, is_pass, start_time) " +
            "VALUES(#{studentId}, #{examId}, 0, 0, NOW()) " +
            "ON DUPLICATE KEY UPDATE " +
            "start_time = COALESCE(start_time, NOW())")
    void recordStartTime(@Param("studentId") String studentId,
                         @Param("examId") int examId);

    // 查询已经过了多少秒
    @Select("SELECT TIMESTAMPDIFF(SECOND, start_time, NOW()) " +
            "FROM exam_result WHERE student_id=#{studentId} AND exam_id=#{examId}")
    Integer getElapsedSeconds(@Param("studentId") String studentId,
                              @Param("examId") int examId);

    @Select("SELECT e.status AS status, e.duration AS duration, " +
            "CASE WHEN e.start_time <= NOW() THEN 1 ELSE 0 END AS \"scheduledStarted\", " +
            "r.start_time AS \"startTime\", r.submitted_at AS \"submittedAt\", " +
            "TIMESTAMPDIFF(SECOND, r.start_time, NOW()) AS \"elapsedSeconds\" " +
            "FROM exam e LEFT JOIN exam_result r " +
            "ON r.exam_id=e.exam_id AND r.student_id=#{studentId} " +
            "WHERE e.exam_id=#{examId}")
    Map<String, Object> getExamSession(@Param("studentId") String studentId,
                                       @Param("examId") int examId);

    @Select("SELECT q.question_id FROM paper_question pq " +
            "JOIN question q ON q.question_id=pq.question_id " +
            "JOIN exam e ON e.paper_id=pq.paper_id " +
            "WHERE e.exam_id=#{examId}")
    List<Integer> getQuestionIdsByExamId(@Param("examId") int examId);

    @Update("UPDATE exam_result r SET total_score=(" +
            "SELECT COALESCE(SUM(q.score), 0) FROM answer_record a " +
            "JOIN question q ON q.question_id=a.question_id " +
            "WHERE a.student_id=#{studentId} AND a.exam_id=#{examId} " +
            "AND REPLACE(REPLACE(UPPER(a.student_answer), ',', ''), ' ', '') = " +
            "REPLACE(REPLACE(UPPER(q.correct_answer), ',', ''), ' ', '')" +
            ") WHERE r.student_id=#{studentId} AND r.exam_id=#{examId}")
    int calculateScore(@Param("studentId") String studentId,
                       @Param("examId") int examId);

    @Update("UPDATE exam_result r SET r.is_pass=CASE WHEN r.total_score >= (" +
            "SELECT p.total_score * 0.6 FROM exam e " +
            "JOIN exam_paper p ON p.paper_id=e.paper_id WHERE e.exam_id=#{examId}" +
            ") THEN 1 ELSE 0 END, " +
            "r.submitted_at=NOW() " +
            "WHERE r.student_id=#{studentId} AND r.exam_id=#{examId} AND r.submitted_at IS NULL")
    int finalizeSubmission(@Param("studentId") String studentId,
                           @Param("examId") int examId);
}
