package com.exam.online_exam.controller;

import com.exam.online_exam.entity.Student;
import com.exam.online_exam.mapper.ExamMapper;
import com.exam.online_exam.service.ExamSubmissionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.MultiValueMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/student")
public class ExamController {

    @Autowired
    private ExamMapper examMapper;
    @Autowired
    private ExamSubmissionService examSubmissionService;

    // 考试列表
    @GetMapping("/exams")
    public String examList(HttpSession session, Model model) {
        if (session.getAttribute("student") == null) return "redirect:/";
        List<Map<String, Object>> exams = examMapper.getActiveExams();
        model.addAttribute("exams", exams);
        return "exam_list";
    }

    // 进入答题页
    @GetMapping("/exam/{examId}")
    public String examDetail(@PathVariable int examId,
                             HttpSession session, Model model) {
        Student student = (Student) session.getAttribute("student");
        if (student == null) return "redirect:/";

        int count = examMapper.checkAlreadySubmitted(student.getStudentId(), examId);
        if (count > 0) {
            // 检查是否已经有成绩（答完了）
            model.addAttribute("msg", "你已经参加过该考试");
            return "exam_done";
        }

        int remainSeconds;
        try {
            remainSeconds = examSubmissionService.startExam(student.getStudentId(), examId);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            model.addAttribute("msg", ex.getMessage());
            return "exam_done";
        }

        // 时间已到，直接跳转成绩页
        if (remainSeconds <= 0) {
            examSubmissionService.finalizeExpired(student.getStudentId(), examId);
            return "redirect:/student/results";
        }

        List<Map<String, Object>> questions = examMapper.getQuestionsByExamId(examId);
        model.addAttribute("questions", questions);
        model.addAttribute("examId", examId);
        model.addAttribute("remainSeconds", remainSeconds);
        return "exam_detail";
    }

    // 提交答卷
    @PostMapping("/exam/submit")
    public String submitExam(@RequestParam int examId,
                             @RequestParam MultiValueMap<String, String> allParams,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        Student student = (Student) session.getAttribute("student");
        if (student == null) return "redirect:/";

        try {
            examSubmissionService.submit(student.getStudentId(), examId, allParams);
            return "redirect:/student/results";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addAttribute("error", ex.getMessage());
            return "redirect:/student/exam/" + examId;
        }
    }
}
