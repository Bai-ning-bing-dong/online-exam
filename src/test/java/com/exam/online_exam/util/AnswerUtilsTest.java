package com.exam.online_exam.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnswerUtilsTest {
    @Test
    void shouldNormalizeMultipleChoiceAnswers() {
        assertEquals("AC", AnswerUtils.normalize(List.of("C", "A", "A")));
        assertEquals("AB", AnswerUtils.normalize("b, a"));
    }
}
