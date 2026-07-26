package com.exam.online_exam.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordUtilTest {
    @Test
    void shouldHashWithRandomSaltAndVerify() {
        String first = PasswordUtil.hash("123456");
        String second = PasswordUtil.hash("123456");

        assertTrue(PasswordUtil.matches("123456", first));
        assertFalse(PasswordUtil.matches("wrong", first));
        assertNotEquals(first, second);
    }
}
