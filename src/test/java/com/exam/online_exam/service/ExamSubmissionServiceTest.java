package com.exam.online_exam.service;

import com.exam.online_exam.mapper.ExamMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExamSubmissionServiceTest {
    private ExamMapper examMapper;
    private ExamSubmissionService service;

    @BeforeEach
    void setUp() {
        examMapper = mock(ExamMapper.class);
        service = new ExamSubmissionService(examMapper);
    }

    @Test
    void shouldNormalizeAnswersAndFinalizeScore() {
        when(examMapper.getExamSession("S001", 1)).thenReturn(openSession());
        when(examMapper.getQuestionIdsByExamId(1)).thenReturn(List.of(7));
        when(examMapper.calculateScore("S001", 1)).thenReturn(1);
        when(examMapper.finalizeSubmission("S001", 1)).thenReturn(1);

        service.submit("S001", 1, Map.of("answer_7", List.of("C", "A", "A")));

        verify(examMapper).submitAnswer("S001", 1, 7, "AC");
        verify(examMapper).calculateScore("S001", 1);
        verify(examMapper).finalizeSubmission("S001", 1);
    }

    @Test
    void shouldRejectQuestionThatDoesNotBelongToExam() {
        when(examMapper.getExamSession("S001", 1)).thenReturn(openSession());
        when(examMapper.getQuestionIdsByExamId(1)).thenReturn(List.of(7));

        assertThrows(IllegalArgumentException.class,
                () -> service.submit("S001", 1, Map.of("answer_999", List.of("A"))));

        verify(examMapper, never()).finalizeSubmission("S001", 1);
    }

    @Test
    void shouldRejectExamBeforeScheduledStart() {
        Map<String, Object> session = openSession();
        session.put("scheduledStarted", 0);
        when(examMapper.getExamSession("S001", 1)).thenReturn(session);

        assertThrows(IllegalStateException.class, () -> service.startExam("S001", 1));

        verify(examMapper, never()).recordStartTime("S001", 1);
    }

    private Map<String, Object> openSession() {
        Map<String, Object> session = new HashMap<>();
        session.put("status", 1);
        session.put("scheduledStarted", 1);
        session.put("duration", 30);
        session.put("elapsedSeconds", 10);
        session.put("startTime", LocalDateTime.now());
        session.put("submittedAt", null);
        return session;
    }
}
