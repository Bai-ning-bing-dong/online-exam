package com.exam.online_exam.controller;

import com.exam.online_exam.entity.Student;
import com.exam.online_exam.mapper.StudentMapper;
import com.exam.online_exam.mapper.ExamResultMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import java.util.List;
import java.util.Map;

@Controller
public class LoginController {

    @Autowired
    private StudentMapper studentMapper;
    @Autowired
    private ExamResultMapper examResultMapper;

    // 访问登录页
    @GetMapping("/")
    public String index() {
        return "login";
    }

    // 处理登录
    @PostMapping("/login")
    public String login(@RequestParam String studentId,
                        @RequestParam String password,
                        HttpSession session) {
        Student student = studentMapper.login(studentId, password);
        if (student != null) {
            session.setAttribute("student", student);
            return "redirect:/student/index";
        }
        return "redirect:/?error=1";
    }

    @GetMapping("/student/index")
    public String studentIndex(HttpSession session) {
        if (session.getAttribute("student") == null) {
            return "redirect:/";
        }
        return "student_index";
    }

    @GetMapping("/student/results")
    public String results(HttpSession session, Model model) {
        Student student = (Student) session.getAttribute("student");
        if (student == null) return "redirect:/";
        List<Map<String, Object>> results =
                examResultMapper.getResultsByStudentId(student.getStudentId());
        model.addAttribute("results", results);
        return "student_results";
    }

    // 跳转注册页
    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    // 处理注册
    @PostMapping("/register")
    public String register(@RequestParam String studentId,
                           @RequestParam String name,
                           @RequestParam String className,
                           @RequestParam String gender,
                           @RequestParam String password,
                           @RequestParam String confirmPassword,
                           @RequestParam(required = false) String email,
                           Model model) {
        // 两次密码不一致
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "两次密码输入不一致");
            return "register";
        }
        // 学号已存在
        if (studentMapper.checkStudentExists(studentId) > 0) {
            model.addAttribute("error", "该学号已被注册");
            return "register";
        }
        studentMapper.register(studentId, name, className, gender, password, email);
        return "redirect:/?registered=1";
    }
}