package org.dslul.openboard.inputmethod.latin.ciphers;

import java.util.Locale;

/** Monoalphabetic substitution generated from a keyword. */
public class KeywordSubstitutionCipher implements MessageCipher {
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private final String mCipherAlphabet;

    public KeywordSubstitutionCipher(final String keyword) {
        mCipherAlphabet = keyedAlphabet(keyword == null ? "KEYWORD" : keyword);
    }

    @Override
    public String encrypt(final String input) {
        return transform(input, false);
    }

    @Override
    public String decrypt(final String input) {
        return transform(input, true);
    }

    private String transform(final String input, final boolean decrypt) {
        final StringBuilder output = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            final char c = input.charAt(i);
            final int index = ALPHABET.indexOf(Character.toUpperCase(c));
            if (index < 0) {
                output.append(c);
                continue;
            }
            final int transformed = decrypt ? mCipherAlphabet.indexOf(ALPHABET.charAt(index)) : index;
            final char result = decrypt ? ALPHABET.charAt(transformed) : mCipherAlphabet.charAt(index);
            output.append(Character.isLowerCase(c) ? Character.toLowerCase(result) : result);
        }
        return output.toString();
    }

    private static String keyedAlphabet(final String keyword) {
        final String cleaned = keyword.toUpperCase(Locale.US).replaceAll("[^A-Z]", "");
        final StringBuilder builder = new StringBuilder(ALPHABET.length());
        appendUnique(builder, cleaned.isEmpty() ? "KEYWORD" : cleaned);
        appendUnique(builder, ALPHABET);
        return builder.toString();
    }

    private static void appendUnique(final StringBuilder builder, final String value) {
        for (int i = 0; i < value.length(); i++) {
            final char c = value.charAt(i);
            if (builder.indexOf(String.valueOf(c)) < 0) {
                builder.append(c);
            }
        }
    }
}
