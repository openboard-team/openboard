package org.dslul.openboard.inputmethod.latin.ciphers;

/** Scytale transposition cipher using a configurable number of columns. */
public final class ScytaleCipher implements MessageCipher {
    private final int mColumns;

    public ScytaleCipher(final int columns) {
        mColumns = Math.max(2, columns);
    }

    @Override
    public String encrypt(final String input) {
        if (input.length() <= 1) {
            return input;
        }
        final int rows = (input.length() + mColumns - 1) / mColumns;
        final StringBuilder output = new StringBuilder(input.length());
        for (int column = 0; column < mColumns; column++) {
            for (int row = 0; row < rows; row++) {
                final int index = row * mColumns + column;
                if (index < input.length()) {
                    output.append(input.charAt(index));
                }
            }
        }
        return output.toString();
    }

    @Override
    public String decrypt(final String input) {
        if (input.length() <= 1) {
            return input;
        }
        final int rows = (input.length() + mColumns - 1) / mColumns;
        final char[] output = new char[input.length()];
        int inputIndex = 0;
        for (int column = 0; column < mColumns; column++) {
            for (int row = 0; row < rows; row++) {
                final int outputIndex = row * mColumns + column;
                if (outputIndex < output.length) {
                    output[outputIndex] = input.charAt(inputIndex++);
                }
            }
        }
        return new String(output);
    }
}
