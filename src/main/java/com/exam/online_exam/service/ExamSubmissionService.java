package com.exam.online_exam.service;

import com.exam.online_exam.mapper.ExamMapper;
import com.exam.online_exam.util.AnswerUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ExamSubmissionService {
    private static final int SUBMISSION_GRACE_SECONDS = 5;

    private final ExamMapper examMapper;

    public ExamSubmissionService(ExamMapper examMapper) {
        this.examMapper = examMapper;
    }

    @Transactional
    public int startExam(String studentId, int examId) {
        Map<String, Object> session = examMapper.getExamSession(studentId, examId);
        requireAvailableExam(session);
        if (session.get("submittedAt") != null) {
            throw new IllegalStateException("该考试已经提交，不能重复作答");
        }

        examMapper.recordStartTime(studentId, examId);
        session = requireOpenSession(studentId, examId);
        return number(session.get("duration"), "考试时长") * 60
                - number(session.get("elapsedSeconds"), "答题计时");
    }

    @Transactional
    public void submit(String studentId, int examId, Map<String, List<String>> parameters) {
        Map<String, Object> session = requireOpenSession(studentId, examId);
        int durationSeconds = number(session.get("duration"), "考试时长") * 60;
        int elapsedSeconds = number(session.get("elapsedSeconds"), "答题计时");
        if (elapsedSeconds > durationSeconds + SUBMISSION_GRACE_SECONDS) {
            throw new IllegalStateException("考试已超时，不能继续提交答案");
        }

        Set<Integer> allowedQuestionIds = new HashSet<>(examMapper.getQuestionIdsByExamId(examId));
        for (Map.Entry<String, List<String>> entry : parameters.entrySet()) {
            if (!entry.getKey().startsWith("answer_")) {
                continue;
            }
            int questionId = parseQuestionId(entry.getKey());
            if (!allowedQuestionIds.contains(questionId)) {
                throw new IllegalArgumentException("提交内容包含不属于本场考试的题目");
            }
            String answer = AnswerUtils.normalize(entry.getValue());
            if (!answer.isEmpty()) {
                examMapper.submitAnswer(studentId, examId, questionId, answer);
            }
        }

        finalizeResult(studentId, examId);
    }

    @Transactional
    public void finalizeExpired(String studentId, int examId) {
        Map<String, Object> session = requireOpenSession(studentId, examId);
        int durationSeconds = number(session.get("duration"), "考试时长") * 60;
        int elapsedSeconds = number(session.get("elapsedSeconds"), "答题计时");
        if (elapsedSeconds < durationSeconds) {
            throw new IllegalStateException("考试尚未超时");
        }
        finalizeResult(studentId, examId);
    }

    private Map<String, Object> requireOpenSession(String studentId, int examId) {
        Map<String, Object> session = examMapper.getExamSession(studentId, examId);
        requireAvailableExam(session);
        if (session.get("startTime") == null) {
            throw new IllegalStateException("尚未进入该考试");
        }
        if (session.get("submittedAt") != null) {
            throw new IllegalStateException("该考试已经提交，不能重复作答");
        }
        return session;
    }

    private void requireAvailableExam(Map<String, Object> session) {
        if (session == null) {
            throw new IllegalArgumentException("考试不存在");
        }
        if (number(session.get("status"), "考试状态") != 1) {
            throw new IllegalStateException("考试当前不在进行中");
        }
        if (number(session.get("scheduledStarted"), "考试开始时间") != 1) {
            throw new IllegalStateException("考试尚未到开始时间");
        }
    }

    private void finalizeResult(String studentId, int examId) {
        if (examMapper.calculateScore(studentId, examId) != 1
                || examMapper.finalizeSubmission(studentId, examId) != 1) {
            throw new IllegalStateException("答卷状态已变化，请刷新后重试");
        }
    }

    private int parseQuestionId(String parameterName) {
        try {
            return Integer.parseInt(parameterName.substring("answer_".length()));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("题目编号格式不正确", ex);
        }
    }

    private int number(Object value, String fieldName) {
        if (!(value instanceof Number)) {
            throw new IllegalStateException(fieldName + "数据缺失");
        }
        return ((Number) value).intValue();
    }
}
