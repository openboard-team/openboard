package org.dslul.openboard.inputmethod.latin.ciphers;

/** Classic rail fence transposition cipher. */
public final class RailFenceCipher implements MessageCipher {
    private final int mRails;

    public RailFenceCipher(final int rails) {
        mRails = Math.max(2, rails);
    }

    @Override
    public String encrypt(final String input) {
        if (input.length() <= 1) {
            return input;
        }
        final StringBuilder[] rails = new StringBuilder[mRails];
        for (int i = 0; i < mRails; i++) {
            rails[i] = new StringBuilder();
        }
        int rail = 0;
        int direction = 1;
        for (int i = 0; i < input.length(); i++) {
            rails[rail].append(input.charAt(i));
            if (rail == 0) {
                direction = 1;
            } else if (rail == mRails - 1) {
                direction = -1;
            }
            rail += direction;
        }
        final StringBuilder output = new StringBuilder(input.length());
        for (StringBuilder builder : rails) {
            output.append(builder);
        }
        return output.toString();
    }

    @Override
    public String decrypt(final String input) {
        if (input.length() <= 1) {
            return input;
        }
        final int length = input.length();
        final int[] pattern = new int[length];
        int rail = 0;
        int direction = 1;
        final int[] railCounts = new int[mRails];
        for (int i = 0; i < length; i++) {
            pattern[i] = rail;
            railCounts[rail]++;
            if (rail == 0) {
                direction = 1;
            } else if (rail == mRails - 1) {
                direction = -1;
            }
            rail += direction;
        }
        final char[][] rails = new char[mRails][];
        int inputIndex = 0;
        for (int i = 0; i < mRails; i++) {
            rails[i] = input.substring(inputIndex, inputIndex + railCounts[i]).toCharArray();
            inputIndex += railCounts[i];
        }
        final int[] railIndexes = new int[mRails];
        final StringBuilder output = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            final int currentRail = pattern[i];
            output.append(rails[currentRail][railIndexes[currentRail]++]);
        }
        return output.toString();
    }
}
