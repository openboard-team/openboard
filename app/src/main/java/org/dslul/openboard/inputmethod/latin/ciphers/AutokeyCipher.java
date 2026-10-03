package org.dslul.openboard.inputmethod.latin.ciphers;

import java.util.Locale;

/** Autokey Vigenere cipher that extends the key with plaintext during encryption. */
public final class AutokeyCipher implements MessageCipher {
    private final String mKeyword;

    public AutokeyCipher(final String keyword) {
        final String normalized = normalizeKeyword(keyword);
        mKeyword = normalized.isEmpty() ? "KEY" : normalized;
    }

    @Override
    public String encrypt(final String input) {
        final StringBuilder output = new StringBuilder(input.length());
        final StringBuilder keyStream = new StringBuilder(mKeyword);
        int keyIndex = 0;
        for (int i = 0; i < input.length(); i++) {
            final char c = input.charAt(i);
            final char upper = Character.toUpperCase(c);
            if (upper < 'A' || upper > 'Z') {
                output.append(c);
                continue;
            }
            final int shift = keyStream.charAt(keyIndex++) - 'A';
            final char encrypted = (char)('A' + (upper - 'A' + shift) % 26);
            output.append(Character.isLowerCase(c)
                    ? Character.toLowerCase(encrypted) : encrypted);
            keyStream.append(upper);
        }
        return output.toString();
    }

    @Override
    public String decrypt(final String input) {
        final StringBuilder output = new StringBuilder(input.length());
        final StringBuilder keyStream = new StringBuilder(mKeyword);
        int keyIndex = 0;
        for (int i = 0; i < input.length(); i++) {
            final char c = input.charAt(i);
            final char upper = Character.toUpperCase(c);
            if (upper < 'A' || upper > 'Z') {
                output.append(c);
                continue;
            }
            final int shift = keyStream.charAt(keyIndex++) - 'A';
            final char decrypted = (char)('A' + (upper - 'A' - shift + 26) % 26);
            output.append(Character.isLowerCase(c)
                    ? Character.toLowerCase(decrypted) : decrypted);
            keyStream.append(decrypted);
        }
        return output.toString();
    }

    private static String normalizeKeyword(final String keyword) {
        return (keyword == null ? "" : keyword).toUpperCase(Locale.US)
                .replaceAll("[^A-Z]", "");
    }
}
