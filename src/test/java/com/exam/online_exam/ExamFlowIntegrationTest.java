package com.exam.online_exam;

import com.exam.online_exam.mapper.ExamMapper;
import com.exam.online_exam.mapper.ExamResultMapper;
import com.exam.online_exam.service.ExamSubmissionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("demo")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.sql.init.mode=always"
)
class ExamFlowIntegrationTest {

    @Autowired
    private ExamMapper examMapper;

    @Autowired
    private ExamResultMapper examResultMapper;

    @Autowired
    private ExamSubmissionService submissionService;

    @Test
    void completesDemoExamAndCalculatesScore() {
        assertThat(submissionService.startExam("S001", 1)).isPositive();

        MultiValueMap<String, String> answers = new LinkedMultiValueMap<>();
        answers.add("answer_1", "A");
        answers.add("answer_2", "B");
        answers.add("answer_2", "A");
        answers.add("answer_3", "A");
        answers.add("answer_4", "A");

        submissionService.submit("S001", 1, answers);

        Map<String, Object> session = examMapper.getExamSession("S001", 1);
        assertThat(session.get("submittedAt")).isNotNull();
        assertThat(examMapper.checkAlreadySubmitted("S001", 1)).isEqualTo(1);
        Map<String, Object> result = examResultMapper.getResultsByStudentId("S001").getFirst();
        assertThat(result.get("totalScore").toString()).isEqualTo("40.00");
        assertThat(result.get("isPass")).isEqualTo("及格");
    }
}
