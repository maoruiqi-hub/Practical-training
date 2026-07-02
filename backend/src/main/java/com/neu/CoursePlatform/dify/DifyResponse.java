package com.neu.CoursePlatform.dify;

/**
 * Dify API 统一响应封装。
 */
public class DifyResponse {

    private boolean success;
    private String content;
    private String error;
    private String conversationId;
    private String messageId;

    private DifyResponse() {}

    public static DifyResponse success(String content) {
        DifyResponse resp = new DifyResponse();
        resp.success = true;
        resp.content = content;
        return resp;
    }

    public static DifyResponse error(String error) {
        DifyResponse resp = new DifyResponse();
        resp.success = false;
        resp.error = error;
        return resp;
    }

    public boolean isSuccess() { return success; }
    public String getContent() { return content; }
    public String getError() { return error; }

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
}
