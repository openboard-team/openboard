package org.dslul.openboard.inputmethod.latin.ciphers;

import java.util.Locale;

/** Reciprocal Beaufort cipher. */
public final class BeaufortCipher implements MessageCipher {
    private final String mKeyword;

    public BeaufortCipher(final String keyword) {
        final String normalized = (keyword == null ? "" : keyword).toUpperCase(Locale.US)
                .replaceAll("[^A-Z]", "");
        mKeyword = normalized.isEmpty() ? "KEY" : normalized;
    }

    @Override
    public String encrypt(final String input) {
        return transform(input);
    }

    @Override
    public String decrypt(final String input) {
        return transform(input);
    }

    private String transform(final String input) {
        final StringBuilder output = new StringBuilder(input.length());
        int keyIndex = 0;
        for (int i = 0; i < input.length(); i++) {
            final char c = input.charAt(i);
            final char upper = Character.toUpperCase(c);
            if (upper < 'A' || upper > 'Z') {
                output.append(c);
                continue;
            }
            final int key = mKeyword.charAt(keyIndex % mKeyword.length()) - 'A';
            final char transformed = (char)('A' + (key - (upper - 'A') + 26) % 26);
            output.append(Character.isLowerCase(c)
                    ? Character.toLowerCase(transformed) : transformed);
            keyIndex++;
        }
        return output.toString();
    }
}
