package org.dslul.openboard.inputmethod.latin.ciphers;

import java.util.Locale;

/** Bifid cipher with a keyed Polybius square and I/J sharing a square cell. */
public final class BifidCipher implements MessageCipher {
    private static final String DEFAULT_ALPHABET = "ABCDEFGHIKLMNOPQRSTUVWXYZ";
    private final String mSquare;

    public BifidCipher(final String keyword) {
        mSquare = keyedAlphabet(keyword);
    }

    @Override
    public String encrypt(final String input) {
        return transformSegments(input, false);
    }

    @Override
    public String decrypt(final String input) {
        return transformSegments(input, true);
    }

    private String transformSegments(final String input, final boolean decrypt) {
        final StringBuilder output = new StringBuilder(input.length());
        final StringBuilder segment = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = Character.toUpperCase(input.charAt(i));
            if (c == 'J') {
                c = 'I';
            }
            if (c >= 'A' && c <= 'Z') {
                segment.append(c);
            } else {
                appendSegment(output, segment, decrypt);
                segment.setLength(0);
                output.append(input.charAt(i));
            }
        }
        appendSegment(output, segment, decrypt);
        return output.toString();
    }

    private void appendSegment(final StringBuilder output, final StringBuilder segment,
            final boolean decrypt) {
        if (segment.length() == 0) {
            return;
        }
        output.append(decrypt ? decryptSegment(segment.toString())
                : encryptSegment(segment.toString()));
    }

    private String encryptSegment(final String segment) {
        final int[] coordinates = new int[segment.length() * 2];
        for (int i = 0; i < segment.length(); i++) {
            final int index = mSquare.indexOf(segment.charAt(i));
            coordinates[i] = index / 5;
            coordinates[i + segment.length()] = index % 5;
        }
        return readCoordinatePairs(coordinates);
    }

    private String decryptSegment(final String segment) {
        final int[] coordinates = new int[segment.length() * 2];
        for (int i = 0; i < segment.length(); i++) {
            final int index = mSquare.indexOf(segment.charAt(i));
            coordinates[i * 2] = index / 5;
            coordinates[i * 2 + 1] = index % 5;
        }
        final StringBuilder output = new StringBuilder(segment.length());
        for (int i = 0; i < segment.length(); i++) {
            output.append(mSquare.charAt(coordinates[i] * 5
                    + coordinates[i + segment.length()]));
        }
        return output.toString();
    }

    private String readCoordinatePairs(final int[] coordinates) {
        final StringBuilder output = new StringBuilder(coordinates.length / 2);
        for (int i = 0; i < coordinates.length; i += 2) {
            output.append(mSquare.charAt(coordinates[i] * 5 + coordinates[i + 1]));
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
