package org.usac.bots.jbot.util;

import java.text.SimpleDateFormat;

public class DateUtils {
    public static transient SimpleDateFormat fullFormat = new SimpleDateFormat("MM/dd/yyyy h:mm:ss a");
    public static transient SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy h:mm a");
    public static transient SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a");
}
