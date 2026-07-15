package com.applify.securepass;

import java.security.SecureRandom;

public class PasswordGenerator {
    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SYMBOLS = "!@#$%^&*()-_=+[]{}|;:,.<>?";

    public static String generate(int length, boolean useUpper, boolean useLower,
                                  boolean useDigits, boolean useSymbols) {
        StringBuilder pool = new StringBuilder();
        if (useUpper) pool.append(UPPER);
        if (useLower) pool.append(LOWER);
        if (useDigits) pool.append(DIGITS);
        if (useSymbols) pool.append(SYMBOLS);
        if (pool.length() == 0) {
            pool.append(LOWER).append(DIGITS);
        }

        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(pool.length());
            password.append(pool.charAt(index));
        }
        return password.toString();
    }
}