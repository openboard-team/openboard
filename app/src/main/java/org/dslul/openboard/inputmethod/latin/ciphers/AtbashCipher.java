package org.dslul.openboard.inputmethod.latin.ciphers;

/** Simple reciprocal Atbash substitution cipher. */
public final class AtbashCipher implements MessageCipher {
    @Override
    public String encrypt(final String input) {
        return transform(input);
    }

    @Override
    public String decrypt(final String input) {
        return transform(input);
    }

    private static String transform(final String input) {
        final StringBuilder output = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            output.append(CipherAlphabet.reverse(input.charAt(i)));
        }
        return output.toString();
    }

    public static boolean supports(final int codePoint) {
        return CipherAlphabet.contains(codePoint);
    }
}
