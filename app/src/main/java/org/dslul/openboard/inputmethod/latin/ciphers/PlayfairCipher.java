package org.dslul.openboard.inputmethod.latin.ciphers;

import java.util.Locale;

/** Playfair digraph cipher with I/J sharing a square cell. */
public final class PlayfairCipher implements MessageCipher {
    private static final String DEFAULT_ALPHABET = "ABCDEFGHIKLMNOPQRSTUVWXYZ";
    private final String mSquare;

    public PlayfairCipher(final String keyword) {
        mSquare = keyedAlphabet(keyword);
    }

    @Override
    public String encrypt(final String input) {
        final String prepared = preparePlainText(input);
        final StringBuilder output = new StringBuilder(prepared.length());
        for (int i = 0; i < prepared.length(); i += 2) {
            appendTransformedPair(output, prepared.charAt(i), prepared.charAt(i + 1), false);
        }
        return output.toString();
    }

    @Override
    public String decrypt(final String input) {
        final String normalized = normalizeLetters(input);
        final StringBuilder output = new StringBuilder(normalized.length());
        for (int i = 0; i + 1 < normalized.length(); i += 2) {
            appendTransformedPair(output, normalized.charAt(i), normalized.charAt(i + 1), true);
        }
        if (normalized.length() % 2 == 1) {
            output.append(normalized.charAt(normalized.length() - 1));
        }
        return output.toString();
    }

    private void appendTransformedPair(final StringBuilder output, final char first,
            final char second, final boolean decrypt) {
        final int firstIndex = mSquare.indexOf(first);
        final int secondIndex = mSquare.indexOf(second);
        final int firstRow = firstIndex / 5;
        final int firstColumn = firstIndex % 5;
        final int secondRow = secondIndex / 5;
        final int secondColumn = secondIndex % 5;
        final int direction = decrypt ? 4 : 1;
        if (firstRow == secondRow) {
            output.append(mSquare.charAt(firstRow * 5 + (firstColumn + direction) % 5));
            output.append(mSquare.charAt(secondRow * 5 + (secondColumn + direction) % 5));
        } else if (firstColumn == secondColumn) {
            output.append(mSquare.charAt(((firstRow + direction) % 5) * 5 + firstColumn));
            output.append(mSquare.charAt(((secondRow + direction) % 5) * 5 + secondColumn));
        } else {
            output.append(mSquare.charAt(firstRow * 5 + secondColumn));
            output.append(mSquare.charAt(secondRow * 5 + firstColumn));
        }
    }

    private static String preparePlainText(final String input) {
        final String normalized = normalizeLetters(input);
        final StringBuilder prepared = new StringBuilder(normalized.length() + 1);
        for (int i = 0; i < normalized.length(); i++) {
            final char current = normalized.charAt(i);
            prepared.append(current);
            if (i + 1 < normalized.length() && current == normalized.charAt(i + 1)) {
                prepared.append('X');
            }
        }
        if (prepared.length() % 2 == 1) {
            prepared.append('X');
        }
        return prepared.toString();
    }

    private static String normalizeLetters(final String input) {
        final StringBuilder output = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = Character.toUpperCase(input.charAt(i));
            if (c == 'J') {
                c = 'I';
            }
            if (c >= 'A' && c <= 'Z') {
                output.append(c);
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
