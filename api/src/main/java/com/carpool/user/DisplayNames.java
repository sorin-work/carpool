package com.carpool.user;

import com.carpool.common.ApiException;

import java.text.Normalizer;

/** Cleans up the names people type before they are shown to the rest of the group. */
public final class DisplayNames {

    private DisplayNames() {
    }

    /**
     * NFC-normalises, collapses runs of whitespace (including non-breaking spaces), drops invisible format characters (but keeps the joiner inside emoji like 👩‍👧)
     * and refuses names with nothing visible left.
     */
    public static String clean(String raw) {
        String name = Normalizer.normalize(raw, Normalizer.Form.NFC)
                .replaceAll("[\\p{Cf}&&[^\\x{200D}]]", "")
                .replaceAll("[\\s\\p{Z}]+", " ")
                .strip();
        if (name.isEmpty()) {
            throw ApiException.badRequest("Name cannot be blank");
        }
        return name;
    }
}
