package org.usac.bots.jbot.entities;

import com.ciscospark.Message;

import java.io.Serializable;

public class ChatMessage extends Message implements Serializable {
    private String processedText;

    public static ChatMessage fromMessage(Message message) {
        ChatMessage result = null;

        if (message != null) {
            result = new ChatMessage();

            result.setId(message.getId());
            result.setRoomId(message.getRoomId());
            result.setToPersonId(message.getToPersonId());
            result.setToPersonEmail(message.getToPersonEmail());
            result.setPersonId(message.getPersonId());
            result.setPersonEmail(message.getPersonEmail());
            result.setText(message.getText());
            result.setProcessedText(message.getText());
            result.setFile(message.getFile());
            result.setRoomType(message.getRoomType());
            result.setCreated(message.getCreated());
            result.setFiles(message.getFiles());
            result.setMarkdown(message.getMarkdown());
            result.setHtml(message.getHtml());
            result.setMentionedPeople(message.getMentionedPeople());
        }
        return result;
    }
    
    public String getProcessedText() {
        return processedText;
    }

    public void setProcessedText(String processedText) {
        this.processedText = processedText;
    }
}
