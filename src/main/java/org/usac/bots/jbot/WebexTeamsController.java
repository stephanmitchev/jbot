package org.usac.bots.jbot;

import com.ciscospark.*;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.usac.bots.jbot.entities.ChatMessage;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.usac.bots.jbot.util.StringUtils.getRandomReply;
import static org.usac.bots.jbot.util.StringUtils.removeTermsFromStart;

@Component
@ConfigurationProperties(prefix = "webexteams")
public class WebexTeamsController {

    private String accessToken;
    private String botId;

    private Map<String, List<String>> replies;

    private Spark spark;

    private static Logger log = null;

    protected WebexTeamsController() {
        log = LoggerFactory.getLogger(this.getClass());

    }

    @PostConstruct
    private void init() {
        spark = Spark.builder()
                .baseUrl(URI.create("https://api.ciscospark.com/v1"))
                .accessToken(accessToken)
                .build();
    }



    public Message groupMessage(String roomId, String reply, String... mentions) {
        if (reply == null || reply.isEmpty()) {
            reply = getRandomReply("idk", replies);
        }

        Message replyMessage = new Message();

        replyMessage.setRoomId(roomId);
        replyMessage.setMarkdown(reply);
        if (mentions != null) {
            replyMessage.setMentionedPeople(mentions);
        }
        spark.messages().post(replyMessage);

        return replyMessage;
    }

    public Message directMessage(String email, String reply, String... mentions) {
        if (reply == null || reply.isEmpty()) {
            reply = getRandomReply("idk", replies);
        }

        Message replyMessage = new Message();

        replyMessage.setToPersonEmail(email);
        replyMessage.setMarkdown(reply);

        spark.messages().post(replyMessage);

        return replyMessage;
    }

    public Message groupMessageWithAttachment(String roomId, String reply, File file, boolean deleteOnSend) {
        Message replyMessage = new Message();
        replyMessage.setRoomId(roomId);

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();

        builder.addTextBody("roomId", roomId, ContentType.TEXT_PLAIN);
        if (reply != null) {
            replyMessage.setMarkdown(reply);
            builder.addTextBody("markdown", reply, ContentType.TEXT_PLAIN);
        }
        if (file != null) {
            replyMessage.setFile(file.getName());
            ContentType contentType;
            if (file.getName().toLowerCase().endsWith("csv")) { contentType = ContentType.DEFAULT_TEXT; }
            else if (file.getName().toLowerCase().endsWith("gif")) { contentType = ContentType.IMAGE_GIF; }
            else if (file.getName().toLowerCase().endsWith("png")) { contentType = ContentType.IMAGE_PNG; }
            else if (file.getName().toLowerCase().endsWith("jpg")) { contentType = ContentType.IMAGE_JPEG; }
            else if (file.getName().toLowerCase().endsWith("svg")) { contentType = ContentType.IMAGE_SVG; }
            else if (file.getName().toLowerCase().endsWith("json")) { contentType = ContentType.APPLICATION_JSON; }
            else { contentType = ContentType.DEFAULT_BINARY; }
            builder.addBinaryBody("files", file, contentType, file.getName());
        }

        HttpEntity entity = builder.build();
        HttpPost post = new HttpPost("https://api.ciscospark.com/v1/messages");
        post.setEntity(entity);
        post.setHeader("Authorization", "Bearer " + accessToken);

        try {
            HttpResponse response = HttpClients.createDefault().execute(post);
            HttpEntity result = response.getEntity();

        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            if (file != null && deleteOnSend) {
                file.delete();
            }
        }

        return replyMessage;
    }

    public ChatMessage getMessage(Message message, String botId) {

        if (!message.getRoomType().equals("direct")) {
            if (message.getMentionedPeople() == null || !Arrays.asList(message.getMentionedPeople()).contains(botId)) {
                return null;
            }
        }

        ChatMessage result = null;
        try {
            result = ChatMessage.fromMessage(spark.messages().path("/" + message.getId()).get());
            if (result.getMentionedPeople() != null && result.getMentionedPeople().length > 0) {
                result.setText(removeTermsFromStart(result.getText(), "LogGroot"));
                result.setProcessedText(removeTermsFromStart(result.getProcessedText(), "LogGroot"));
                result.setMarkdown(removeTermsFromStart(result.getMarkdown(), "LogGroot"));
            }
        } catch (Exception e) {
            log.debug("Cannot read message with id: %s", message.getId());
        }


        return result;
    }

    public Room getRoom(ChatMessage message) {

        Room result = null;
        try {
            result = spark.rooms().path("/" + message.getRoomId()).get();
        } catch (SparkException e) {

        }

        return result;
    }

    public Person getFrom(ChatMessage message, String botId) {

        if (!message.getRoomType().equals("direct")) {
            if (message.getMentionedPeople() == null || !Arrays.asList(message.getMentionedPeople()).contains(botId)) {
                return null;
            }
        }

        Person result = null;
        try {
            result = spark.people().path("/" + message.getPersonId()).get();
        } catch (SparkException e) {
        }

        return result;

    }

    public Room createRoom(String roomName, List<String> userEmails) {

        final List<Room> roomList = new ArrayList<>();
        spark.rooms().iterate().forEachRemaining(r -> {
            if (r.getTitle().equalsIgnoreCase(roomName)) {
                roomList.add(r);
            }
        });

        Room room = roomList.size() > 0
                ? roomList.get(0)
                : null;

        if (room == null) {
            room = new Room();
            room.setType("group");
            room.setTitle(roomName);
            room = spark.rooms().post(room);
        }

        final List<String> currentEmails = new ArrayList<>();
        spark.memberships()
                .queryParam("roomId", room.getId())
                .iterate()
                .forEachRemaining(member -> {
                    currentEmails.add(member.getPersonEmail());
                });


        for (String email : userEmails) {
            if (!currentEmails.contains(email)) {
                Membership membership = new Membership();
                membership.setRoomId(room.getId());
                membership.setPersonEmail(email);
                spark.memberships().post(membership);
            }

        }

        return room;

    }


    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getBotId() {
        return botId;
    }

    public void setBotId(String botId) {
        this.botId = botId;
    }

    public Spark getSpark() {
        return spark;
    }

    public void setSpark(Spark spark) {
        this.spark = spark;
    }

    public Map<String, List<String>> getReplies() {
        return replies;
    }

    public void setReplies(Map<String, List<String>> replies) {
        this.replies = replies;
    }

    public void delete(ChatMessage message) {
        spark.messages().path("/" + message.getId()).delete();
    }
}
