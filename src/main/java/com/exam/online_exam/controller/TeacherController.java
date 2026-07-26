package com.exam.online_exam.controller;

import com.exam.online_exam.entity.Teacher;
import com.exam.online_exam.mapper.TeacherMapper;
import com.exam.online_exam.util.AnswerUtils;
import com.exam.online_exam.util.PasswordUtil;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/teacher")
public class TeacherController {

    @Autowired
    private TeacherMapper teacherMapper;

    // 教师登录
    @PostMapping("/login")
    public String login(@RequestParam String teacherId,
                        @RequestParam String password,
                        HttpSession session) {
        Teacher teacher = teacherMapper.findById(teacherId.trim());
        if (teacher != null && passwordMatches(password, teacher.getPassword())) {
            if (!PasswordUtil.isEncoded(teacher.getPassword())) {
                teacherMapper.updatePassword(teacher.getTeacherId(), PasswordUtil.hash(password));
            }
            session.setAttribute("teacher", teacher);
            session.removeAttribute("student");
            return "redirect:/teacher/index";
        }
        return "redirect:/?error=1";
    }

    // 教师首页
    @GetMapping("/index")
    public String index(HttpSession session) {
        if (session.getAttribute("teacher") == null) return "redirect:/";
        return "teacher_index";
    }

    // 题目管理页
    @GetMapping("/questions")
    public String questions(HttpSession session, Model model) {
        Teacher teacher = (Teacher) session.getAttribute("teacher");
        if (teacher == null) return "redirect:/";
        List<Map<String, Object>> questions =
                teacherMapper.getQuestionsByTeacher(teacher.getTeacherId());
        model.addAttribute("questions", questions);
        return "teacher_questions";
    }

    // 添加题目
    @PostMapping("/question/add")
    public String addQuestion(@RequestParam String content,
                              @RequestParam int questionType,
                              @RequestParam String optionA,
                              @RequestParam String optionB,
                              @RequestParam(required = false) String optionC,
                              @RequestParam(required = false) String optionD,
                              @RequestParam String correctAnswer,
                              @RequestParam double score,
                              HttpSession session) {
        Teacher teacher = (Teacher) session.getAttribute("teacher");
        if (teacher == null) return "redirect:/";
        validateQuestion(content, questionType, correctAnswer, score);
        teacherMapper.addQuestion(teacher.getTeacherId(), questionType,
                content.trim(), optionA, optionB, optionC, optionD,
                AnswerUtils.normalize(correctAnswer), score);
        return "redirect:/teacher/questions";
    }

    @GetMapping("/results")
    public String results(HttpSession session, Model model) {
        Teacher teacher = (Teacher) session.getAttribute("teacher");
        if (teacher == null) return "redirect:/";
        List<Map<String, Object>> stats =
                teacherMapper.getExamStats(teacher.getTeacherId());
        model.addAttribute("stats", stats);
        return "teacher_results";
    }

    @GetMapping("/results/{examId}")
    public String examDetail(@PathVariable int examId,
                             HttpSession session, Model model) {
        Teacher teacher = (Teacher) session.getAttribute("teacher");
        if (teacher == null) return "redirect:/";
        requireOwnedExam(teacher, examId);
        List<Map<String, Object>> results =
                teacherMapper.getStudentResultsByExam(examId);
        model.addAttribute("results", results);
        model.addAttribute("examId", examId);
        return "teacher_exam_detail";
    }

    // 试卷列表
    @GetMapping("/papers")
    public String papers(HttpSession session, Model model) {
        Teacher teacher = (Teacher) session.getAttribute("teacher");
        if (teacher == null) return "redirect:/";
        model.addAttribute("papers", teacherMapper.getPapersByTeacher(teacher.getTeacherId()));
        return "teacher_papers";
    }

    // 创建试卷
    @PostMapping("/paper/create")
    public String createPaper(@RequestParam String paperName, HttpSession session) {
        Teacher teacher = (Teacher) session.getAttribute("teacher");
        if (teacher == null) return "redirect:/";
        if (paperName == null || paperName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "试卷名称不能为空");
        }
        teacherMapper.createPaper(paperName.trim(), teacher.getTeacherId());
        return "redirect:/teacher/papers";
    }

    // 试卷详情（组卷页面）
    @GetMapping("/paper/{paperId}")
    public String paperDetail(@PathVariable int paperId,
                              HttpSession session, Model model) {
        Teacher teacher = (Teacher) session.getAttribute("teacher");
        if (teacher == null) return "redirect:/";
        requireOwnedPaper(teacher, paperId);
        model.addAttribute("paperId", paperId);
        model.addAttribute("questions", teacherMapper.getQuestionsByPaper(paperId));
        model.addAttribute("available", teacherMapper.getAvailableQuestions(
                teacher.getTeacherId(), paperId));
        return "teacher_paper_detail";
    }

    // 添加题目到试卷
    @PostMapping("/paper/{paperId}/add/{questionId}")
    public String addQuestion(@PathVariable int paperId,
                              @PathVariable int questionId,
                              HttpSession session) {
        Teacher teacher = (Teacher) session.getAttribute("teacher");
        if (teacher == null) return "redirect:/";
        requireOwnedPaper(teacher, paperId);
        requireOwnedQuestion(teacher, questionId);
        teacherMapper.addQuestionToPaper(paperId, questionId);
        teacherMapper.updatePaperTotalScore(paperId);
        return "redirect:/teacher/paper/" + paperId;
    }

    // 从试卷移除题目
    @PostMapping("/paper/{paperId}/remove/{questionId}")
    public String removeQuestion(@PathVariable int paperId,
                                 @PathVariable int questionId,
                                 HttpSession session) {
        Teacher teacher = (Teacher) session.getAttribute("teacher");
        if (teacher == null) return "redirect:/";
        requireOwnedPaper(teacher, paperId);
        requireOwnedQuestion(teacher, questionId);
        teacherMapper.removeQuestionFromPaper(paperId, questionId);
        teacherMapper.updatePaperTotalScore(paperId);
        return "redirect:/teacher/paper/" + paperId;
    }

    // 考试管理页面
    @GetMapping("/exams")
    public String exams(HttpSession session, Model model) {
        Teacher teacher = (Teacher) session.getAttribute("teacher");
        if (teacher == null) return "redirect:/";
        model.addAttribute("exams", teacherMapper.getExamsByTeacher(teacher.getTeacherId()));
        model.addAttribute("courses", teacherMapper.getCoursesByTeacher(teacher.getTeacherId()));
        model.addAttribute("papers", teacherMapper.getPapersByTeacher(teacher.getTeacherId()));
        return "teacher_exams";
    }

    // 创建考试
    @PostMapping("/exam/create")
    public String createExam(@RequestParam String courseId,
                             @RequestParam int paperId,
                             @RequestParam String startTime,
                             @RequestParam int duration,
                             HttpSession session) {
        Teacher teacher = (Teacher) session.getAttribute("teacher");
        if (teacher == null) return "redirect:/";
        requireOwnedPaper(teacher, paperId);
        if (teacherMapper.ownsCourse(teacher.getTeacherId(), courseId) == 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权使用该课程");
        }
        if (duration <= 0 || startTime == null || startTime.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "考试时间和时长不合法");
        }
        teacherMapper.createExam(courseId, paperId, startTime, duration);
        return "redirect:/teacher/exams";
    }

    // 开启考试
    @PostMapping("/exam/{examId}/start")
    public String startExam(@PathVariable int examId, HttpSession session) {
        Teacher teacher = (Teacher) session.getAttribute("teacher");
        if (teacher == null) return "redirect:/";
        requireOwnedExam(teacher, examId);
        teacherMapper.updateExamStatus(examId, 1);
        return "redirect:/teacher/exams";
    }

    // 结束考试
    @PostMapping("/exam/{examId}/end")
    public String endExam(@PathVariable int examId, HttpSession session) {
        Teacher teacher = (Teacher) session.getAttribute("teacher");
        if (teacher == null) return "redirect:/";
        requireOwnedExam(teacher, examId);
        teacherMapper.updateExamStatus(examId, 2);
        return "redirect:/teacher/exams";
    }

    // 跳转修改页面
    @GetMapping("/question/{questionId}/edit")
    public String editQuestion(@PathVariable int questionId,
                               HttpSession session, Model model) {
        Teacher teacher = (Teacher) session.getAttribute("teacher");
        if (teacher == null) return "redirect:/";
        requireOwnedQuestion(teacher, questionId);
        model.addAttribute("q", teacherMapper.getQuestionById(questionId));
        return "teacher_question_edit";
    }

    // 提交修改
    @PostMapping("/question/{questionId}/update")
    public String updateQuestion(@PathVariable int questionId,
                                 @RequestParam String content,
                                 @RequestParam int questionType,
                                 @RequestParam String optionA,
                                 @RequestParam String optionB,
                                 @RequestParam(required = false) String optionC,
                                 @RequestParam(required = false) String optionD,
                                 @RequestParam String correctAnswer,
                                 @RequestParam double score,
                                 HttpSession session) {
        Teacher teacher = (Teacher) session.getAttribute("teacher");
        if (teacher == null) return "redirect:/";
        requireOwnedQuestion(teacher, questionId);
        validateQuestion(content, questionType, correctAnswer, score);
        teacherMapper.updateQuestion(questionId, content, questionType,
                optionA, optionB, optionC, optionD,
                AnswerUtils.normalize(correctAnswer), score);
        return "redirect:/teacher/questions";
    }

    // 删除题目
    @PostMapping("/question/{questionId}/delete")
    public String deleteQuestion(@PathVariable int questionId,
                                 HttpSession session) {
        Teacher teacher = (Teacher) session.getAttribute("teacher");
        if (teacher == null) return "redirect:/";
        requireOwnedQuestion(teacher, questionId);
        teacherMapper.deletePaperQuestion(questionId);
        teacherMapper.deleteQuestion(questionId);
        return "redirect:/teacher/questions";
    }

    private boolean passwordMatches(String rawPassword, String storedPassword) {
        return PasswordUtil.isEncoded(storedPassword)
                ? PasswordUtil.matches(rawPassword, storedPassword)
                : storedPassword != null && storedPassword.equals(rawPassword);
    }

    private void validateQuestion(String content, int questionType,
                                  String correctAnswer, double score) {
        if (content == null || content.isBlank() || questionType < 1 || questionType > 3
                || AnswerUtils.normalize(correctAnswer).isEmpty() || score <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "题目信息不合法");
        }
    }

    private void requireOwnedQuestion(Teacher teacher, int questionId) {
        if (teacherMapper.ownsQuestion(teacher.getTeacherId(), questionId) == 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权操作该题目");
        }
    }

    private void requireOwnedPaper(Teacher teacher, int paperId) {
        if (teacherMapper.ownsPaper(teacher.getTeacherId(), paperId) == 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权操作该试卷");
        }
    }

    private void requireOwnedExam(Teacher teacher, int examId) {
        if (teacherMapper.ownsExam(teacher.getTeacherId(), examId) == 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权操作该考试");
        }
    }
}
