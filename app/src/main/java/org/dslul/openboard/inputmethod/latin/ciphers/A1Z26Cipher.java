package org.dslul.openboard.inputmethod.latin.ciphers;

/** A1Z26 cipher that maps A-Z to 1-26 and preserves unsupported characters. */
public final class A1Z26Cipher implements MessageCipher {
    @Override
    public String encrypt(final String input) {
        final StringBuilder output = new StringBuilder(input.length() * 2);
        for (int i = 0; i < input.length(); i++) {
            final char c = input.charAt(i);
            final char upper = Character.toUpperCase(c);
            if (upper >= 'A' && upper <= 'Z') {
                if (output.length() > 0 && output.charAt(output.length() - 1) != ' ') {
                    output.append(' ');
                }
                output.append(upper - 'A' + 1);
            } else if (Character.isWhitespace(c)) {
                if (output.length() > 0 && output.charAt(output.length() - 1) != ' ') {
                    output.append(' ');
                }
                output.append('/');
            } else {
                if (output.length() > 0 && output.charAt(output.length() - 1) != ' ') {
                    output.append(' ');
                }
                if (Character.isDigit(c)) {
                    output.append('#');
                }
                output.append(c);
            }
        }
        return output.toString();
    }

    @Override
    public String decrypt(final String input) {
        final StringBuilder output = new StringBuilder(input.length());
        final String[] tokens = input.trim().split("\\s+");
        for (String token : tokens) {
            if (token.isEmpty()) {
                continue;
            }
            if ("/".equals(token)) {
                output.append(' ');
                continue;
            }
            if (token.charAt(0) == '#') {
                output.append(token.substring(1));
                continue;
            }
            try {
                final int value = Integer.parseInt(token);
                if (value >= 1 && value <= 26) {
                    output.append((char)('A' + value - 1));
                } else {
                    output.append(token);
                }
            } catch (NumberFormatException ignored) {
                output.append(token);
            }
        }
        return output.toString();
    }
}
