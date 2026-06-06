package com.carpool.export;

import java.util.List;

/** RFC 4180 CSV with CRLF line endings. */
public final class CsvWriter {

    private final StringBuilder out = new StringBuilder();

    public CsvWriter row(List<?> values) {
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                out.append(',');
            }
            out.append(cell(values.get(i)));
        }
        out.append("\r\n");
        return this;
    }

    static String cell(Object value) {
        if (value == null) {
            return "";
        }
        String text = value.toString();
        if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }

    @Override
    public String toString() {
        return out.toString();
    }
}
