package com.carpool.calendar;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IcsWriterTest {

    private static final Instant NOW = Instant.parse("2026-03-02T06:00:00Z");

    @Test
    void escapesSpecialCharacters() {
        assertThat(IcsWriter.escape("Elm St, no. 12; back gate\nring twice"))
                .isEqualTo("Elm St\\, no. 12\\; back gate\\nring twice");
    }

    @Test
    void foldsLongLines() {
        String summary = "x".repeat(200);
        String ics = IcsWriter.write("Cal", List.of(new IcsWriter.Event("1@carpool", NOW, NOW, summary, null, null)), NOW);

        for (String line : ics.split("\r\n")) {
            assertThat(line.length()).isLessThanOrEqualTo(75);
        }
        assertThat(ics.replace("\r\n ", "")).contains("SUMMARY:" + summary);
    }

    @Test
    void emptyCalendarIsStillValid() {
        assertThat(IcsWriter.write("Cal", List.of(), NOW))
                .contains("BEGIN:VCALENDAR", "VERSION:2.0", "END:VCALENDAR")
                .doesNotContain("VEVENT");
    }
}
