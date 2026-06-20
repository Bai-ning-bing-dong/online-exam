package com.exam.online_exam.controller;

import com.exam.online_exam.entity.Student;
import com.exam.online_exam.mapper.ExamMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/student")
public class ExamController {

    @Autowired
    private ExamMapper examMapper;

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

        // 记录开始时间（已记录过则不更新）
        examMapper.recordStartTime(student.getStudentId(), examId);

        // 计算剩余秒数
        int duration = examMapper.getExamDuration(examId);
        Integer elapsed = examMapper.getElapsedSeconds(student.getStudentId(), examId);
        int remainSeconds = duration * 60 - (elapsed == null ? 0 : elapsed);

        // 时间已到，直接跳转成绩页
        if (remainSeconds <= 0) {
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
                             @RequestParam Map<String, String> allParams,
                             HttpSession session) {
        Student student = (Student) session.getAttribute("student");
        if (student == null) return "redirect:/";

        // 遍历所有答案参数（格式：answer_题目ID）
        for (Map.Entry<String, String> entry : allParams.entrySet()) {
            if (entry.getKey().startsWith("answer_")) {
                int questionId = Integer.parseInt(entry.getKey().replace("answer_", ""));
                examMapper.submitAnswer(student.getStudentId(), examId,
                        questionId, entry.getValue());
            }
        }
        examMapper.markSubmitted(student.getStudentId(), examId);
        return "redirect:/student/results";
    }
}