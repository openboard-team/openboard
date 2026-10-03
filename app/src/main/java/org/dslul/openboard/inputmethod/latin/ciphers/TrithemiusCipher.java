package org.dslul.openboard.inputmethod.latin.ciphers;

/** Trithemius progressive Caesar cipher. */
public final class TrithemiusCipher implements PositionedMessageCipher {
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

    private String transform(final String input, final boolean decrypt, final int startPosition) {
        final StringBuilder output = new StringBuilder(input.length());
        int position = Math.max(0, startPosition);
        for (int i = 0; i < input.length(); i++) {
            final char c = input.charAt(i);
            if (!CipherAlphabet.contains(c)) {
                output.append(c);
                continue;
            }
            output.append(CipherAlphabet.shift(c, decrypt ? -position : position));
            position++;
        }
        return output.toString();
    }
}
