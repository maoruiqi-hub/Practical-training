package com.neu.CoursePlatform.service.impl;

import com.neu.CoursePlatform.entity.Question;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskSubmissionServiceImplAnswerMatchingTest {
    private final TaskSubmissionServiceImpl service = new TaskSubmissionServiceImpl(
            null, null, null, null, null, null, null, null, null, null);

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
        assertTrue(isAnswerCorrect(question, "A,B,C"));
        assertFalse(isAnswerCorrect(question, "r,w,delete"));
        assertFalse(isAnswerCorrect(question, "r,w,a,delete"));
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
}
