package com.exam.online_exam.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

@Mapper
public interface ExamResultMapper {

    @Select("SELECT s.name AS \"studentName\", c.course_name AS \"courseName\", " +
            "r.total_score AS \"totalScore\", " +
            "CASE WHEN r.is_pass=1 THEN '及格' ELSE '不及格' END AS \"isPass\", " +
            "r.submitted_at AS \"submittedAt\" " +
            "FROM exam_result r " +
            "JOIN student s ON r.student_id = s.student_id " +
            "JOIN exam e ON r.exam_id = e.exam_id " +
            "JOIN course c ON e.course_id = c.course_id " +
            "WHERE r.student_id = #{studentId} AND r.submitted_at IS NOT NULL " +
            "ORDER BY r.submitted_at DESC")
    List<Map<String, Object>> getResultsByStudentId(String studentId);
}
