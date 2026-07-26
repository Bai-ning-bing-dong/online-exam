package com.exam.online_exam.controller;

import com.exam.online_exam.entity.Student;
import com.exam.online_exam.mapper.StudentMapper;
import com.exam.online_exam.mapper.ExamResultMapper;
import com.exam.online_exam.util.PasswordUtil;
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
        Student student = studentMapper.findById(studentId.trim());
        if (student != null && passwordMatches(password, student.getPassword())) {
            if (!PasswordUtil.isEncoded(student.getPassword())) {
                studentMapper.updatePassword(student.getStudentId(), PasswordUtil.hash(password));
            }
            session.setAttribute("student", student);
            session.removeAttribute("teacher");
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
        studentId = studentId == null ? "" : studentId.trim();
        name = name == null ? "" : name.trim();
        className = className == null ? "" : className.trim();
        if (studentId.isEmpty() || name.isEmpty() || className.isEmpty()) {
            model.addAttribute("error", "学号、姓名和班级不能为空");
            return "register";
        }
        if (password == null || password.length() < 6) {
            model.addAttribute("error", "密码长度不能少于6位");
            return "register";
        }
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
        studentMapper.register(studentId, name, className, gender,
                PasswordUtil.hash(password), email == null ? null : email.trim());
        return "redirect:/?registered=1";
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    private boolean passwordMatches(String rawPassword, String storedPassword) {
        return PasswordUtil.isEncoded(storedPassword)
                ? PasswordUtil.matches(rawPassword, storedPassword)
                : storedPassword != null && storedPassword.equals(rawPassword);
    }
}
