package org.dslul.openboard.inputmethod.latin.ciphers;

/** Android-version-independent unpadded Base64 codec for cipher payloads. */
final class CipherBase64 {
    private static final char[] ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();

    private CipherBase64() {
    }

    static String encode(final byte[] input) {
        final StringBuilder output = new StringBuilder((input.length * 4 + 2) / 3);
        for (int i = 0; i < input.length; i += 3) {
            final int remaining = input.length - i;
            final int value = (input[i] & 0xff) << 16
                    | (remaining > 1 ? (input[i + 1] & 0xff) << 8 : 0)
                    | (remaining > 2 ? input[i + 2] & 0xff : 0);
            output.append(ALPHABET[(value >>> 18) & 0x3f]);
            output.append(ALPHABET[(value >>> 12) & 0x3f]);
            if (remaining > 1) {
                output.append(ALPHABET[(value >>> 6) & 0x3f]);
            }
            if (remaining > 2) {
                output.append(ALPHABET[value & 0x3f]);
            }
        }
        return output.toString();
    }

    static byte[] decode(final String input) {
        if (input == null || input.length() % 4 == 1) {
            throw new IllegalArgumentException("Invalid Base64 payload");
        }
        final int outputLength = input.length() * 6 / 8;
        final byte[] output = new byte[outputLength];
        int buffer = 0;
        int bits = 0;
        int outputIndex = 0;
        for (int i = 0; i < input.length(); i++) {
            final int value = decodeCharacter(input.charAt(i));
            buffer = (buffer << 6) | value;
            bits += 6;
            if (bits >= 8) {
                bits -= 8;
                output[outputIndex++] = (byte)(buffer >>> bits);
                buffer &= (1 << bits) - 1;
            }
        }
        return output;
    }

    private static int decodeCharacter(final char character) {
        if (character >= 'A' && character <= 'Z') {
            return character - 'A';
        }
        if (character >= 'a' && character <= 'z') {
            return character - 'a' + 26;
        }
        if (character >= '0' && character <= '9') {
            return character - '0' + 52;
        }
        if (character == '+') {
            return 62;
        }
        if (character == '/') {
            return 63;
        }
        throw new IllegalArgumentException("Invalid Base64 character");
    }
}
