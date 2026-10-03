package org.dslul.openboard.inputmethod.latin.ciphers;

/** Vigenere polyalphabetic cipher. */
public final class VigenereCipher implements PositionedMessageCipher {
    private final String mKeyword;

    public VigenereCipher(final String keyword) {
        final StringBuilder cleaned = new StringBuilder();
        if (keyword != null) {
            for (int i = 0; i < keyword.length(); i++) {
                if (CipherAlphabet.contains(keyword.charAt(i))) {
                    cleaned.append(keyword.charAt(i));
                }
            }
        }
        mKeyword = cleaned.length() == 0 ? "KEY" : cleaned.toString();
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
            if (!CipherAlphabet.contains(c)) {
                output.append(c);
                continue;
            }
            final int shift = CipherAlphabet.position(
                    mKeyword.charAt(letters % mKeyword.length()));
            output.append(CipherAlphabet.shift(c, decrypt ? -shift : shift));
            letters++;
        }
        return output.toString();
    }
}
