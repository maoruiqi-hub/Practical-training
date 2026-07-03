package com.neu.CoursePlatform.service.impl;

import com.neu.CoursePlatform.entity.Question;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskSubmissionServiceImplAnswerMatchingTest {
    private final TaskSubmissionServiceImpl service = new TaskSubmissionServiceImpl(
            null, null, null, null, null, null, null, null);

    @Test
    void singleChoiceTextOptionMatchesLetterAnswer() throws Exception {
        Question question = question("single",
                "[\"auto close file resource\",\"auto encrypt file\",\"auto compress file\",\"convert text to JSON\"]",
                "A");

        assertTrue(isAnswerCorrect(question, "auto close file resource"));
        assertTrue(isAnswerCorrect(question, "A"));
        assertFalse(isAnswerCorrect(question, "auto encrypt file"));
    }

    @Test
    void chineseSingleChoiceTextOptionMatchesLetterAnswer() throws Exception {
        Question question = question("single",
                "[\"自动关闭文件资源\",\"自动加密文件\",\"自动压缩文件\",\"自动把文本转成JSON\"]",
                "A");

        assertTrue(isAnswerCorrect(question, "自动关闭文件资源"));
        assertTrue(isAnswerCorrect(question, "A"));
        assertTrue(isAnswerCorrect(question, "A. 自动关闭文件资源"));
        assertTrue(isAnswerCorrect(question, "A、自动关闭文件资源"));
        assertFalse(isAnswerCorrect(question, "自动加密文件"));
    }

    @Test
    void multiChoiceTextOptionsMatchLetterAnswer() throws Exception {
        Question question = question("multi", "[\"r\",\"w\",\"a\",\"delete\"]", "A,B,C");

        assertTrue(isAnswerCorrect(question, "r,w,a"));
        assertTrue(isAnswerCorrect(question, List.of("r", "w", "a")));
        assertTrue(isAnswerCorrect(question, "A,B,C"));
        assertFalse(isAnswerCorrect(question, "r,w,delete"));
        assertFalse(isAnswerCorrect(question, "r,w,a,delete"));
    }

    @Test
    void objectAndPlainTextOptionsMatchByKeyAndMarkedText() throws Exception {
        Question objectOptions = question("single", "{\"A\":\"接口\",\"B\":\"实现\"}", "A");
        assertTrue(isAnswerCorrect(objectOptions, "接口"));
        assertTrue(isAnswerCorrect(objectOptions, "A. 接口"));
        assertFalse(isAnswerCorrect(objectOptions, "实现"));

        Question plainOptions = question("single", "A. 文件\nB. 网络", "A");
        assertTrue(isAnswerCorrect(plainOptions, "文件"));
        assertTrue(isAnswerCorrect(plainOptions, "A、文件"));
        assertFalse(isAnswerCorrect(plainOptions, "网络"));
    }

    @Test
    void fillAnswerNormalizesWhitespace() throws Exception {
        Question question = question("fill", "", "面向 对象");

        assertTrue(isAnswerCorrect(question, "  面向   对象  "));
        assertFalse(isAnswerCorrect(question, "面向过程"));
    }

    @Test
    void choiceMatchingFallsBackToNormalizedTextAndSpacedMarkers() throws Exception {
        Question noOptions = question("single", "", "plain answer");
        assertTrue(isAnswerCorrect(noOptions, "plain answer"));
        assertFalse(isAnswerCorrect(noOptions, "other answer"));

        Question invalidJsonOptions = question("single", "{not-json\nA 文件\n\nB 网络", "B");
        assertEquals(3, invokeOptionEntries(invalidJsonOptions).size());
        assertTrue(isAnswerCorrect(invalidJsonOptions, "B 网络"));
        assertFalse(isAnswerCorrect(invalidJsonOptions, "其他"));

        Question jsonTextOptions = question("single", "\"普通文本选项\"", "普通文本选项");
        assertEquals(1, invokeOptionEntries(jsonTextOptions).size());

        assertEquals("27", invokeFallbackLetter(26));
        assertEquals("A", invokeFallbackLetter(0));
    }

    @Test
    void bareLetterOptionTextDoesNotBecomeAnotherOptionLabel() throws Exception {
        Question question = question("single", "[\"first\",\"second\",\"a\"]", "C");

        assertTrue(isAnswerCorrect(question, "a"));

        question.setAnswer("A");
        assertFalse(isAnswerCorrect(question, "a"));
        assertTrue(isAnswerCorrect(question, "A"));
    }

    private Question question(String type, String options, String answer) {
        Question question = new Question();
        question.setType(type);
        question.setOptions(options);
        question.setAnswer(answer);
        question.setScore(5);
        return question;
    }

    private boolean isAnswerCorrect(Question question, Object response) throws Exception {
        Method method = TaskSubmissionServiceImpl.class
                .getDeclaredMethod("isAnswerCorrect", Question.class, Object.class);
        method.setAccessible(true);
        return (Boolean) method.invoke(service, question, response);
    }

    private String invokeFallbackLetter(int index) throws Exception {
        Method method = TaskSubmissionServiceImpl.class
                .getDeclaredMethod("fallbackLetter", int.class);
        method.setAccessible(true);
        return (String) method.invoke(service, index);
    }

    private List<?> invokeOptionEntries(Question question) throws Exception {
        Method method = TaskSubmissionServiceImpl.class
                .getDeclaredMethod("optionEntries", Question.class);
        method.setAccessible(true);
        return (List<?>) method.invoke(service, question);
    }
}
