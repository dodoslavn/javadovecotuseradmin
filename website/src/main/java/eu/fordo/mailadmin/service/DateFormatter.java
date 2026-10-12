package eu.fordo.mailadmin.service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/** Slovak/European date order (dd.MM.yyyy), matching phpopenvpnadmin's fmt_connected(). */
public final class DateFormatter {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss").withZone(ZoneOffset.UTC);

    private DateFormatter() {}

    public static String format(Instant instant) {
        return instant == null ? "" : FORMAT.format(instant) + " UTC";
    }

    /** Same timestamp, without the trailing " UTC" — for tables whose column header already says it. */
    public static String formatNoZone(Instant instant) {
        return instant == null ? "" : FORMAT.format(instant);
    }
}
