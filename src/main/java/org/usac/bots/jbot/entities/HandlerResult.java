package org.usac.bots.jbot.entities;

import org.usac.bots.jbot.messagehandlers.LogGrootMessageHandler;

import java.io.File;

public class HandlerResult extends SerializableItem {

    private String reply;
    private File attachment;
    private boolean question;
    private boolean successful;
    private String conversationTheme;
    private String status;
    private Object item;
    private transient LogGrootMessageHandler handler;

    public static HandlerResult success() {
        HandlerResult result = new HandlerResult();
        result.setSuccessful(true);
        return result;
    }
    public static HandlerResult failure() {
        HandlerResult result = new HandlerResult();
        result.setSuccessful(false);
        return result;
    }
    public HandlerResult reply(String reply) {
        this.setReply(reply);
        return this;
    }
    public HandlerResult conversationTheme(String theme) {
        this.setConversationTheme(theme);
        return this;
    }
    public HandlerResult question() {
        this.setQuestion(true);
        return this;
    }
    public HandlerResult item(Object item) {
        this.setItem(item);
        return this;
    }
    public HandlerResult success(boolean status) {
        this.setSuccessful(status);
        return this;
    }
    public HandlerResult attachment(File attachment) {
        this.setAttachment(attachment);
        return this;
    }
    public HandlerResult handler(LogGrootMessageHandler handler) {
        this.setHandler(handler);
        return this;
    }
    public HandlerResult status(String status) {
        this.setStatus(status);
        return this;
    }




    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public File getAttachment() {
        return attachment;
    }

    public void setAttachment(File attachment) {
        this.attachment = attachment;
    }

    public boolean isQuestion() {
        return question;
    }

    public void setQuestion(boolean question) {
        this.question = question;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Object getItem() {
        return item;
    }

    public void setItem(Object item) {
        this.item = item;
    }

    public String getConversationTheme() {
        return conversationTheme;
    }

    public void setConversationTheme(String conversationTheme) {
        this.conversationTheme = conversationTheme;
    }

    public LogGrootMessageHandler getHandler() {
        return handler;
    }

    public void setHandler(LogGrootMessageHandler handler) {
        this.handler = handler;
    }


}
