package org.usac.bots.jbot.util;

import java.security.SecureRandom;
import java.util.Collection;

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

        int completedCount = (int)Math.floor(percent * 10);
        for (int i = 0; i < 10; i++) {
            result.append(i <= completedCount ? "&#x2588;" : "&#x2591;");
        }

        int completedPercent = (int)Math.floor(percent * 100);

        result.append(" ").append(completedPercent).append("%  \n");
        return result.toString();
    }


}
