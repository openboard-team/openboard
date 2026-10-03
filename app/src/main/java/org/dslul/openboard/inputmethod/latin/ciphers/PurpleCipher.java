package org.dslul.openboard.inputmethod.latin.ciphers;

import java.util.Locale;

/** Educational stepping substitution inspired by the historical Japanese Purple machine. */
public final class PurpleCipher implements PositionedMessageCipher {
    private static final String[] ALPHABETS = {
            "QWERTYUIOPASDFGHJKLZXCVBNM",
            "MNBVCXZLKJHGFDSAPOIUYTREWQ",
            "PHQGIUMEAYLNOFDXJKRCVSTZWB"
    };
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private final int[] mPlugboard;
    private final int mStartPosition;

    public PurpleCipher() {
        this("", 0);
    }

    public PurpleCipher(final String plugboardPairs, final int startPosition) {
        mPlugboard = buildPlugboard(plugboardPairs);
        mStartPosition = positiveMod(startPosition, ALPHABETS.length);
    }

    @Override
    public String encrypt(final String input) {
        return encrypt(input, 0);
    }

    @Override
    public String decrypt(final String input) {
        return decrypt(input, 0);
    }

    @Override
    public String encrypt(final String input, final int position) {
        return transform(input, false, position);
    }

    @Override
    public String decrypt(final String input, final int position) {
        return transform(input, true, position);
    }

    private String transform(final String input, final boolean decrypt, final int position) {
        final StringBuilder output = new StringBuilder(input.length());
        int letters = Math.max(0, position);
        for (int i = 0; i < input.length(); i++) {
            final char c = input.charAt(i);
            final char upper = Character.toUpperCase(c);
            int index = ALPHABET.indexOf(upper);
            if (index < 0) {
                output.append(c);
                continue;
            }
            final String alphabet = ALPHABETS[(mStartPosition + letters) % ALPHABETS.length];
            final char result;
            if (decrypt) {
                index = swap(index, mPlugboard);
                result = ALPHABET.charAt(swap(alphabet.indexOf(ALPHABET.charAt(index)), mPlugboard));
            } else {
                index = swap(index, mPlugboard);
                result = ALPHABET.charAt(swap(ALPHABET.indexOf(alphabet.charAt(index)), mPlugboard));
            }
            output.append(Character.isLowerCase(c) ? Character.toLowerCase(result) : result);
            letters++;
        }
        return output.toString();
    }

    private static int[] buildPlugboard(final String plugboardPairs) {
        final int[] plugboard = new int[ALPHABET.length()];
        for (int i = 0; i < plugboard.length; i++) {
            plugboard[i] = i;
        }
        if (plugboardPairs == null) {
            return plugboard;
        }
        final String[] pairs = plugboardPairs.toUpperCase(Locale.US).split("[\\s,]+");
        for (String pair : pairs) {
            pair = pair.replaceAll("[^A-Z]", "");
            if (pair.length() != 2) {
                continue;
            }
            final int first = ALPHABET.indexOf(pair.charAt(0));
            final int second = ALPHABET.indexOf(pair.charAt(1));
            if (first >= 0 && second >= 0 && plugboard[first] == first && plugboard[second] == second) {
                plugboard[first] = second;
                plugboard[second] = first;
            }
        }
        return plugboard;
    }

    private static int swap(final int value, final int[] plugboard) {
        return plugboard[value];
    }

    private static int positiveMod(final int value, final int modulo) {
        return ((value % modulo) + modulo) % modulo;
    }
}
