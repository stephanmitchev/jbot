package org.usac.bots.jbot;

import com.ciscospark.*;
import com.joestelmach.natty.DateGroup;
import com.joestelmach.natty.Parser;
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
import java.security.SecureRandom;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    public void init() {
        spark = Spark.builder()
                .baseUrl(URI.create("https://api.ciscospark.com/v1"))
                .accessToken(accessToken)
                .build();
    }

    public String getRandomReply(String category) {
        String result = null;
        SecureRandom random = new SecureRandom();

        if (replies.containsKey(category)) {
            result = replies.get(category).get((int) Math.floor(random.nextDouble() * replies.get(category).size()));
        }

        return result != null ? result : "???";
    }

    public Message replyTo(ChatMessage message, String reply) {

        return replyTo(message.getRoomId(), reply);
    }


    public Message replyTo(ChatMessage message, String reply, String... mentions) {

        return replyTo(message.getRoomId(), reply, mentions);
    }


    public Message replyTo(Room room, String reply) {

        return replyTo(room, reply, (String[]) null);
    }

    public Message replyTo(Room room, String reply, String... mentions) {

        return replyTo(room.getId(), reply, mentions);
    }

    public Message replyTo(String roomId, String reply, String... mentions) {
        if (reply == null || reply.isEmpty()) {
            reply = getRandomReply("idk");
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

    public Message replyToWithAttachment(ChatMessage message, String reply, File file, boolean deleteOnSend) {
        Message replyMessage = new Message();
        replyMessage.setRoomId(message.getRoomId());

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();

        builder.addTextBody("roomId", message.getRoomId(), ContentType.TEXT_PLAIN);
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

    public static boolean beginsWith(String text, String... terms) {
        boolean result = false;
        if (text != null) {
            String cleanMessage = text.toLowerCase();


            for (String term : terms) {
                result |= cleanMessage.startsWith(term.toLowerCase());
            }
        }

        return result;
    }

    public static String getRegexGroup(ChatMessage message, Pattern pattern) {
        String result = null;
        if (message != null) {
            String cleanMessage = message.getText().toLowerCase();

            Matcher matcher = pattern.matcher(cleanMessage);

            if (matcher.find()) {
                result = matcher.group();
            }
        }

        return result;
    }

    public static List<String> getRegexGroups(ChatMessage message, Pattern pattern) {
        List<String> result = new ArrayList<>();
        if (message != null) {
            String cleanMessage = message.getText();//.toLowerCase();

            Matcher matcher = pattern.matcher(cleanMessage);

            if (matcher.find()) {
                result.add(message.getText());
                for (int i = 0; i < matcher.groupCount(); i++) {
                    result.add(matcher.group(i + 1));
                }
            }
        }

        return result;
    }

    public static boolean heardRegex(ChatMessage message, Pattern pattern) {
        boolean result = false;
        if (message != null) {
            String cleanMessage = message.getText();//.toLowerCase();

            Matcher matcher = pattern.matcher(cleanMessage);

            result = matcher.find();
        }

        return result;
    }


    public static String removeTermsFromStartRegex(String message, Pattern pattern) {
        if (message != null) {
            message = message.toLowerCase();

            Matcher matcher = pattern.matcher(message);

            message = matcher.replaceFirst("");
        }

        return message;
    }


    public static boolean heardAll(ChatMessage message, String... terms) {
        boolean result = false;

        if (message != null) {
            String cleanMessage = message.getText().toLowerCase();


            for (int i = 0; i < terms.length; i++) {
                String term = terms[i];
                if (i == 0) {
                    result = cleanMessage.contains(term.toLowerCase());
                } else {
                    result &= cleanMessage.contains(term.toLowerCase());
                }
            }
        }

        return result;
    }

    public static String removeTermsFromStart(String message, String... terms) {
        for (String term : terms) {
            if (message != null) {
                message = message.replaceAll("^(?i)" + Pattern.quote(term), "").trim();
            }
        }

        return message;
    }

    public static String removeBeforeAndIncluding(String message, String needle) {
        int start = message.toLowerCase().indexOf(needle.toLowerCase());
        message = message.substring(start + needle.length()).trim();

        return message;
    }


    public static List<DateGroup> getDateGroups(ChatMessage message) {
        Parser parser = new Parser();
        List<DateGroup> parsed = parser.parse(message.getText());
        List<DateGroup> result = new ArrayList<>();
        for (DateGroup group : parsed) {
            if (group.getPosition() <= 10) {
                result.add(group);
            }
        }

        return result;
    }


    public static Message getMessage(Spark spark, Message message, String botId) {

        if (!message.getRoomType().equals("direct")) {
            if (message.getMentionedPeople() == null || !Arrays.asList(message.getMentionedPeople()).contains(botId)) {
                return null;
            }
        }

        Message result = null;
        try {
            result = spark.messages().path("/" + message.getId()).get();
        } catch (SparkException e) {
        }

        return result;
    }

    public static Room getRoom(Spark spark, Message message) {

        Room result = null;
        try {
            result = spark.rooms().path("/" + message.getRoomId()).get();
        } catch (SparkException e) {
        }

        return result;
    }

    public static Person getFrom(Spark spark, Message message, String botId) {

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

    public static boolean heard(ChatMessage message, String... terms) {
        boolean result = false;
        String cleanMessage = message.getText().toLowerCase();

        for (String term : terms) {
            result |= cleanMessage.contains(term);
        }

        return result;
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
