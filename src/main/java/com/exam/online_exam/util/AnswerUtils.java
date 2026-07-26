package com.exam.online_exam.util;

import java.util.Collection;
import java.util.Locale;
import java.util.TreeSet;

/** 将单选、多选答案统一规范化为按字母排序的答案串，例如 C、A -> AC。 */
public final class AnswerUtils {
    private AnswerUtils() {
    }

    public static String normalize(Collection<String> values) {
        TreeSet<Character> answers = new TreeSet<>();
        if (values != null) {
            for (String value : values) {
                if (value == null) {
                    continue;
                }
                for (char ch : value.toUpperCase(Locale.ROOT).toCharArray()) {
                    if (ch >= 'A' && ch <= 'D') {
                        answers.add(ch);
                    }
                }
            }
        }
        StringBuilder result = new StringBuilder();
        for (Character answer : answers) {
            result.append(answer);
        }
        return result.toString();
    }

    public static String normalize(String value) {
        return normalize(value == null ? null : java.util.List.of(value));
    }
}
