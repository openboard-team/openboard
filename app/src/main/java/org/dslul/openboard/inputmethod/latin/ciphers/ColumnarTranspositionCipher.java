package org.dslul.openboard.inputmethod.latin.ciphers;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;

/** Columnar transposition cipher using a keyword for column order. */
public final class ColumnarTranspositionCipher implements MessageCipher {
    private final String mKeyword;

    public ColumnarTranspositionCipher(final String keyword) {
        final String normalized = keyword == null ? "" : keyword.replaceAll("\\s+", "")
                .toUpperCase(Locale.US);
        mKeyword = normalized.isEmpty() ? "KEY" : normalized;
    }

    @Override
    public String encrypt(final String input) {
        final int columns = mKeyword.length();
        final int rows = (input.length() + columns - 1) / columns;
        final char[][] grid = new char[rows][columns];
        int index = 0;
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns && index < input.length(); column++) {
                grid[row][column] = input.charAt(index++);
            }
        }
        final StringBuilder output = new StringBuilder(input.length());
        for (int column : columnOrder()) {
            for (int row = 0; row < rows; row++) {
                if (row * columns + column < input.length()) {
                    output.append(grid[row][column]);
                }
            }
        }
        return output.toString();
    }

    @Override
    public String decrypt(final String input) {
        final int columns = mKeyword.length();
        if (input.isEmpty()) {
            return input;
        }
        final int rows = (input.length() + columns - 1) / columns;
        final char[][] grid = new char[rows][columns];
        int index = 0;
        for (int column : columnOrder()) {
            for (int row = 0; row < rows; row++) {
                if (row * columns + column < input.length()) {
                    grid[row][column] = input.charAt(index++);
                }
            }
        }
        final StringBuilder output = new StringBuilder(input.length());
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                if (row * columns + column < input.length()) {
                    output.append(grid[row][column]);
                }
            }
        }
        return output.toString();
    }

    private Integer[] columnOrder() {
        final Integer[] order = new Integer[mKeyword.length()];
        for (int i = 0; i < order.length; i++) {
            order[i] = i;
        }
        Arrays.sort(order, new Comparator<Integer>() {
            @Override public int compare(Integer left, Integer right) {
                final int charCompare = Character.compare(mKeyword.charAt(left),
                        mKeyword.charAt(right));
                return charCompare != 0 ? charCompare : Integer.compare(left, right);
            }
        });
        return order;
    }
}
