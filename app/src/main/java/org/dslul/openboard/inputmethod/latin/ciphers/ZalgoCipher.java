package org.dslul.openboard.inputmethod.latin.ciphers;

/** Adds or removes deterministic Zalgo combining marks. */
public final class ZalgoCipher implements MessageCipher {
    private static final int MAX_INTENSITY = 4096;
    private static final char[] MARKS = {
            '\u030d', '\u030e', '\u0304', '\u0305', '\u033f', '\u0311', '\u0306', '\u0310',
            '\u0352', '\u0357', '\u0351', '\u0307', '\u0308', '\u030a', '\u0342', '\u0343',
            '\u0344', '\u034a', '\u034b', '\u034c', '\u0303', '\u0302', '\u030c', '\u0350',
            '\u0300', '\u0301', '\u030b', '\u030f', '\u0312', '\u0313', '\u0314', '\u033D',
            '\u0315', '\u031b', '\u0346', '\u031a', '\u0316', '\u0317', '\u0318', '\u0319',
            '\u031c', '\u031d', '\u031e', '\u031f', '\u0320', '\u0324', '\u0325', '\u0326',
            '\u0329', '\u032a', '\u032b', '\u032c', '\u032d', '\u032e', '\u032f', '\u0330',
            '\u0331', '\u0332', '\u0333', '\u0339', '\u033a', '\u033b', '\u033c', '\u0345',
            '\u0347', '\u0348', '\u0349', '\u034d', '\u034e', '\u0353', '\u0354', '\u0355',
            '\u0356', '\u0359', '\u035a', '\u0323'
    };

    private final int mIntensity;

    public ZalgoCipher(final int intensity) {
        mIntensity = Math.min(MAX_INTENSITY, Math.max(0, intensity));
    }

    @Override
    public String encrypt(final String input) {
        final StringBuilder output = new StringBuilder(input.length());
        int characterIndex = 0;
        for (int offset = 0; offset < input.length(); characterIndex++) {
            final int codePoint = input.codePointAt(offset);
            output.appendCodePoint(codePoint);
            offset += Character.charCount(codePoint);
            if (Character.isWhitespace(codePoint) || isCombiningMark(codePoint)) {
                continue;
            }
            for (int markIndex = 0; markIndex < mIntensity; markIndex++) {
                output.append(MARKS[Math.floorMod(
                        codePoint + characterIndex * 31 + markIndex * 17, MARKS.length)]);
            }
        }
        return output.toString();
    }

    @Override
    public String decrypt(final String input) {
        final StringBuilder output = new StringBuilder(input.length());
        for (int offset = 0; offset < input.length();) {
            final int codePoint = input.codePointAt(offset);
            offset += Character.charCount(codePoint);
            if (!isCombiningMark(codePoint)) {
                output.appendCodePoint(codePoint);
            }
        }
        return output.toString();
    }

    private static boolean isCombiningMark(final int codePoint) {
        final int type = Character.getType(codePoint);
        return type == Character.NON_SPACING_MARK
                || type == Character.COMBINING_SPACING_MARK
                || type == Character.ENCLOSING_MARK;
    }
}
