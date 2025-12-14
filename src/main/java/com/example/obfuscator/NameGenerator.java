package com.example.obfuscator;

import java.util.Random;

public class NameGenerator {
    private static final String CHARS = "abcdefghijklmnopqrstuvwxyz";
    private static final Random rand = new Random();
    private static int counter = 0;

    public static String nextShortName() {
        counter++;
        if (counter <= 26) {
            return String.valueOf(CHARS.charAt(counter - 1));
        }
        return CHARS.charAt(rand.nextInt(26)) + String.valueOf(counter - 26);
    }

    public static String nextLongName() {
        return "var" + (counter++);
    }

    public static void reset() {
        counter = 0;
    }

    public static String randomName() {
        return "v" + Math.abs(rand.nextInt(10000));
    }
}