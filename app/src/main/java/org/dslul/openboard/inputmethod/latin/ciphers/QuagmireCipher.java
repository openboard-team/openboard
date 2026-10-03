package org.dslul.openboard.inputmethod.latin.ciphers;

import java.util.Locale;

/** Configurable Quagmire I-IV polyalphabetic substitution cipher. */
public final class QuagmireCipher implements PositionedMessageCipher {
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public enum Variant { I, II, III, IV }

    private final String mPlainAlphabet;
    private final String mCipherAlphabet;
    private final String mIndicatorKeyword;

    public QuagmireCipher(final Variant variant, final String plainKeyword,
            final String cipherKeyword, final String indicatorKeyword) {
        mPlainAlphabet = variant == Variant.I || variant == Variant.III || variant == Variant.IV
                ? keyedAlphabet(plainKeyword) : ALPHABET;
        mCipherAlphabet = variant == Variant.II || variant == Variant.III || variant == Variant.IV
                ? keyedAlphabet(cipherKeyword) : ALPHABET;
        mIndicatorKeyword = cleanKeyword(indicatorKeyword, "KEY");
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
        int letterIndex = Math.max(0, position);
        for (int i = 0; i < input.length(); i++) {
            final char c = input.charAt(i);
            final char upper = Character.toUpperCase(c);
            if (ALPHABET.indexOf(upper) < 0) {
                output.append(c);
                continue;
            }
            final int shift = ALPHABET.indexOf(mIndicatorKeyword.charAt(
                    letterIndex % mIndicatorKeyword.length()));
            final char transformed = decrypt ? decryptLetter(upper, shift) : encryptLetter(upper, shift);
            output.append(Character.isLowerCase(c) ? Character.toLowerCase(transformed) : transformed);
            letterIndex++;
        }
        return output.toString();
    }

    private char encryptLetter(final char letter, final int shift) {
        final int plainIndex = mPlainAlphabet.indexOf(letter);
        return mCipherAlphabet.charAt((plainIndex + shift) % ALPHABET.length());
    }

    private char decryptLetter(final char letter, final int shift) {
        final int cipherIndex = mCipherAlphabet.indexOf(letter);
        return mPlainAlphabet.charAt((cipherIndex - shift + ALPHABET.length()) % ALPHABET.length());
    }

    private static String keyedAlphabet(final String keyword) {
        final String cleaned = cleanKeyword(keyword, "KEYWORD");
        final StringBuilder builder = new StringBuilder(ALPHABET.length());
        appendUnique(builder, cleaned);
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

    private static String cleanKeyword(final String keyword, final String defaultKeyword) {
        final String cleaned = keyword == null ? ""
                : keyword.toUpperCase(Locale.US).replaceAll("[^A-Z]", "");
        return cleaned.isEmpty() ? defaultKeyword : cleaned;
    }
}
