package com.carpool.group;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.random.RandomGenerator;

/** Short codes people can read out loud or type from a phone: no 0/O, 1/I/L. */
public final class InviteCodes {

    static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    static final int LENGTH = 8;

    private static final RandomGenerator RANDOM = new SecureRandom();

    private InviteCodes() {
    }

    public static String generate() {
        return generate(RANDOM);
    }

    static String generate(RandomGenerator random) {
        StringBuilder code = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }

    /** Accepts what people actually type: lower case, spaces and dashes ("abcd-efgh"). */
    public static String normalize(String input) {
        return input == null ? "" : input.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
    }
}
