package org.dslul.openboard.inputmethod.latin.ciphers;

/** Gronsfeld cipher, a Vigenere variant using digit shifts. */
public final class GronsfeldCipher implements PositionedMessageCipher {
    private final String mKey;

    public GronsfeldCipher(final String key) {
        final String normalized = key == null ? "" : key.replaceAll("[^0-9]", "");
        mKey = normalized.isEmpty() ? "31415" : normalized;
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
        int keyIndex = Math.max(0, position);
        for (int i = 0; i < input.length(); i++) {
            final char c = input.charAt(i);
            if (!CipherAlphabet.contains(c)) {
                output.append(c);
                continue;
            }
            final int shift = mKey.charAt(keyIndex % mKey.length()) - '0';
            output.append(CipherAlphabet.shift(c, decrypt ? -shift : shift));
            keyIndex++;
        }
        return output.toString();
    }
}
