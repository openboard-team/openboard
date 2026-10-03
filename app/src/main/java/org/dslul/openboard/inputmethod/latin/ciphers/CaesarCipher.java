package org.dslul.openboard.inputmethod.latin.ciphers;

/**
 * Caesar cipher over supported Latin, Greek, Cyrillic, Armenian, Georgian, Hebrew, and Arabic
 * alphabets while leaving other characters unchanged.
 */
public final class CaesarCipher implements MessageCipher {
    private final int mShift;

    public CaesarCipher(final int shift) {
        mShift = shift;
    }

    @Override
    public String encrypt(final String input) {
        return apply(input, mShift);
    }

    @Override
    public String decrypt(final String input) {
        return apply(input, -mShift);
    }

    private static String apply(final String input, final int shift) {
        final StringBuilder result = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            result.append(CipherAlphabet.shift(input.charAt(i), shift));
        }
        return result.toString();
    }

    public static boolean supports(final int codePoint) {
        return CipherAlphabet.contains(codePoint);
    }
}
