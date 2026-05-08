package com.solvians.showcase;

import java.util.concurrent.ThreadLocalRandom;

public class IsinGenerator {

    private static final String LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    public static String generate() {
        return "";
    }

    public static String generate(ThreadLocalRandom random){
        StringBuilder body = new StringBuilder(11);

        // 2 random uppercase letters
        for (int i = 0; i < 2; i++) {
            body.append(LETTERS.charAt(random.nextInt(LETTERS.length())));
        }

        // 9 random alphanumeric characters
        for (int i = 0; i < 9; i++) {
            body.append(ALPHANUMERIC.charAt(random.nextInt(ALPHANUMERIC.length())));
        }

        return body.toString();
    }
}
