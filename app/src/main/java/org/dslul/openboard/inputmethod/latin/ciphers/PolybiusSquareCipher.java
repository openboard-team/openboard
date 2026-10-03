package org.dslul.openboard.inputmethod.latin.ciphers;

import java.util.Locale;

/** Polybius square cipher using an optional keyed alphabet and I/J merging. */
public final class PolybiusSquareCipher implements MessageCipher {
    private static final String DEFAULT_ALPHABET = "ABCDEFGHIKLMNOPQRSTUVWXYZ";
    private final String mAlphabet;

    public PolybiusSquareCipher(final String keyword) {
        mAlphabet = keyedAlphabet(keyword);
    }

    @Override
    public String encrypt(final String input) {
        final StringBuilder output = new StringBuilder(input.length() * 2);
        for (int i = 0; i < input.length(); i++) {
            final char c = Character.toUpperCase(input.charAt(i)) == 'J'
                    ? 'I' : Character.toUpperCase(input.charAt(i));
            final int index = mAlphabet.indexOf(c);
            if (index >= 0) {
                if (output.length() > 0 && output.charAt(output.length() - 1) != ' ') {
                    output.append(' ');
                }
                output.append(index / 5 + 1).append(index % 5 + 1);
            } else if (Character.isWhitespace(input.charAt(i))) {
                if (output.length() > 0 && output.charAt(output.length() - 1) != ' ') {
                    output.append(' ');
                }
                output.append('/');
            } else {
                if (output.length() > 0 && output.charAt(output.length() - 1) != ' ') {
                    output.append(' ');
                }
                output.append(input.charAt(i));
            }
        }
        return output.toString();
    }

    @Override
    public String decrypt(final String input) {
        final StringBuilder output = new StringBuilder(input.length() / 2);
        final String[] tokens = input.trim().split("\\s+");
        for (String token : tokens) {
            if (token.isEmpty()) {
                continue;
            }
            if ("/".equals(token)) {
                output.append(' ');
                continue;
            }
            if (token.length() == 2 && token.charAt(0) >= '1' && token.charAt(0) <= '5'
                    && token.charAt(1) >= '1' && token.charAt(1) <= '5') {
                final int row = token.charAt(0) - '1';
                final int column = token.charAt(1) - '1';
                output.append(mAlphabet.charAt(row * 5 + column));
            } else {
                output.append(token);
            }
        }
        return output.toString();
    }

    private static String keyedAlphabet(final String keyword) {
        final String source = (keyword == null ? "" : keyword.toUpperCase(Locale.US))
                + DEFAULT_ALPHABET;
        final StringBuilder output = new StringBuilder(DEFAULT_ALPHABET.length());
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == 'J') {
                c = 'I';
            }
            if (DEFAULT_ALPHABET.indexOf(c) >= 0 && output.indexOf(String.valueOf(c)) < 0) {
                output.append(c);
            }
        }
        return output.toString();
    }
}
