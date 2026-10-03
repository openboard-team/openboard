package org.dslul.openboard.inputmethod.latin.ciphers;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** International Morse code converter. */
public final class MorseCipher implements MessageCipher {
    private static final String[] LETTERS = {
            "A", ".-", "B", "-...", "C", "-.-.", "D", "-..", "E", ".", "F", "..-.",
            "G", "--.", "H", "....", "I", "..", "J", ".---", "K", "-.-", "L", ".-..",
            "M", "--", "N", "-.", "O", "---", "P", ".--.", "Q", "--.-", "R", ".-.",
            "S", "...", "T", "-", "U", "..-", "V", "...-", "W", ".--", "X", "-..-",
            "Y", "-.--", "Z", "--..", "0", "-----", "1", ".----", "2", "..---",
            "3", "...--", "4", "....-", "5", ".....", "6", "-....", "7", "--...",
            "8", "---..", "9", "----."
    };
    private static final Map<String, String> TO_MORSE = new HashMap<>();
    private static final Map<String, String> FROM_MORSE = new HashMap<>();

    static {
        for (int i = 0; i < LETTERS.length; i += 2) {
            TO_MORSE.put(LETTERS[i], LETTERS[i + 1]);
            FROM_MORSE.put(LETTERS[i + 1], LETTERS[i]);
        }
    }

    @Override
    public String encrypt(final String input) {
        final StringBuilder output = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            final char c = input.charAt(i);
            if (Character.isWhitespace(c)) {
                output.append(" / ");
                continue;
            }
            final String morse = TO_MORSE.get(String.valueOf(Character.toUpperCase(c)));
            if (morse != null) {
                if (output.length() > 0 && output.charAt(output.length() - 1) != ' ') {
                    output.append(' ');
                }
                output.append(morse).append(' ');
            } else {
                if (output.length() > 0 && output.charAt(output.length() - 1) != ' ') {
                    output.append(' ');
                }
                output.append(c).append(' ');
            }
        }
        return output.toString().trim();
    }

    @Override
    public String decrypt(final String input) {
        final StringBuilder output = new StringBuilder();
        final String[] words = input.trim().split("\\s*/\\s*");
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                output.append(' ');
            }
            final String[] symbols = words[i].trim().split("\\s+");
            for (String symbol : symbols) {
                final String trimmedSymbol = symbol.trim();
                if (trimmedSymbol.isEmpty()) {
                    continue;
                }
                final String decoded = FROM_MORSE.get(trimmedSymbol.toUpperCase(Locale.US));
                if (decoded != null) {
                    output.append(decoded);
                    continue;
                }
                int morseEnd = 0;
                while (morseEnd < trimmedSymbol.length()
                        && (trimmedSymbol.charAt(morseEnd) == '.'
                                || trimmedSymbol.charAt(morseEnd) == '-')) {
                    morseEnd++;
                }
                final String morsePrefix = trimmedSymbol.substring(0, morseEnd);
                final String prefixDecoded = FROM_MORSE.get(morsePrefix.toUpperCase(Locale.US));
                if (prefixDecoded != null) {
                    output.append(prefixDecoded).append(trimmedSymbol.substring(morseEnd));
                } else {
                    output.append(trimmedSymbol);
                }
            }
        }
        return output.toString();
    }
}
