package com.exam.online_exam.mapper;

import com.exam.online_exam.entity.Student;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface StudentMapper {
    @Select("SELECT * FROM student WHERE student_id=#{studentId}")
    Student findById(@Param("studentId") String studentId);

    @Update("UPDATE student SET password=#{password} WHERE student_id=#{studentId}")
    int updatePassword(@Param("studentId") String studentId,
                       @Param("password") String password);

    // 检查学号是否已存在
    @Select("SELECT COUNT(*) FROM student WHERE student_id=#{studentId}")
    int checkStudentExists(@Param("studentId") String studentId);

    // 注册新学生
    @Insert("INSERT INTO student(student_id, name, class_name, gender, password, email) " +
            "VALUES(#{studentId}, #{name}, #{className}, #{gender}, #{password}, #{email})")
    void register(@Param("studentId") String studentId,
                  @Param("name") String name,
                  @Param("className") String className,
                  @Param("gender") String gender,
                  @Param("password") String password,
                  @Param("email") String email);
}
