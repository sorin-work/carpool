package com.carpool.calendar;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Writes a minimal iCalendar (RFC 5545) document. */
public final class IcsWriter {

    public record Event(String uid, Instant start, Instant end, String summary, String location, String description) {
    }

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
            .withZone(ZoneOffset.UTC);

    private IcsWriter() {
    }

    public static String write(String calendarName, List<Event> events, Instant now) {
        StringBuilder out = new StringBuilder();
        line(out, "BEGIN:VCALENDAR");
        line(out, "VERSION:2.0");
        line(out, "PRODID:-//carpool//rides//EN");
        line(out, "CALSCALE:GREGORIAN");
        line(out, "X-WR-CALNAME:" + escape(calendarName));
        for (Event event : events) {
            line(out, "BEGIN:VEVENT");
            line(out, "UID:" + event.uid());
            line(out, "DTSTAMP:" + STAMP.format(now));
            line(out, "DTSTART:" + STAMP.format(event.start()));
            line(out, "DTEND:" + STAMP.format(event.end()));
            line(out, "SUMMARY:" + escape(event.summary()));
            if (event.location() != null) {
                line(out, "LOCATION:" + escape(event.location()));
            }
            if (event.description() != null) {
                line(out, "DESCRIPTION:" + escape(event.description()));
            }
            line(out, "END:VEVENT");
        }
        line(out, "END:VCALENDAR");
        return out.toString();
    }

    static String escape(String text) {
        return text.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\n", "\\n");
    }

    /** Lines longer than 75 octets are folded with CRLF and a leading space. */
    private static void line(StringBuilder out, String content) {
        int limit = 75;
        String rest = content;
        boolean first = true;
        while (rest.length() > limit) {
            out.append(first ? "" : " ").append(rest, 0, limit).append("\r\n");
            rest = rest.substring(limit);
            limit = 74;
            first = false;
        }
        out.append(first ? "" : " ").append(rest).append("\r\n");
    }
}
