package org.dslul.openboard.inputmethod.latin.ciphers;

/** ROT47 substitution over printable ASCII characters. */
public final class Rot47Cipher implements MessageCipher {
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
        for (int i = 0; i < input.length(); i++) {
            final char c = input.charAt(i);
            if (c >= '!' && c <= '~') {
                output.append((char)('!' + (c - '!' + 47) % 94));
            } else {
                output.append(c);
            }
        }
        return output.toString();
    }
}
