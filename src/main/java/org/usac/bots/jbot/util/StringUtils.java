package org.usac.bots.jbot.util;

import com.joestelmach.natty.DateGroup;
import com.joestelmach.natty.Parser;
import org.usac.bots.jbot.entities.ChatMessage;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils {

    public static String getUniqueId(int length, Collection<String> keys) {
        String id;
        do {
            SecureRandom random = new SecureRandom();
            id = "";
            for (int i = 0; i < length; i++) {
                int c = 97 + random.nextInt(26);
                id += (char) c;
            }
        }
        while (keys.contains(id));

        return id;
    }

    public static String renderProgressBar(String title, float percent) {
        StringBuilder result = new StringBuilder();
        result.append("> <b>`").append(title).append("`</b> ");

        int completedCount = (int) Math.floor(percent * 10);
        for (int i = 0; i < 10; i++) {
            result.append(i <= completedCount ? "&#x2588;" : "&#x2591;");
        }

        int completedPercent = (int) Math.floor(percent * 100);

        result.append(" ").append(completedPercent).append("%  \n");
        return result.toString();
    }

    public static String getRandomReply(String category, Map<String, List<String>> replies) {
        String result = null;
        SecureRandom random = new SecureRandom();

        if (replies.containsKey(category)) {
            result = replies.get(category).get((int) Math.floor(random.nextDouble() * replies.get(category).size()));
        }

        return result != null ? result : "???";
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

    public static boolean heard(ChatMessage message, String... terms) {
        boolean result = false;
        String cleanMessage = message.getText().toLowerCase();

        for (String term : terms) {
            result |= cleanMessage.contains(term);
        }

        return result;
    }


}
