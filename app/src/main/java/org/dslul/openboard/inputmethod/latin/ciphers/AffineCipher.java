package org.dslul.openboard.inputmethod.latin.ciphers;

/** Affine substitution cipher over the English alphabet. */
public final class AffineCipher implements MessageCipher {
    private final int mA;
    private final int mB;
    private final int mInverseA;

    public AffineCipher(final int a, final int b) {
        mA = normalizeCoprime(a);
        mB = positiveMod(b);
        mInverseA = modularInverse(mA);
    }

    @Override
    public String encrypt(final String input) {
        return transform(input, false);
    }

    @Override
    public String decrypt(final String input) {
        return transform(input, true);
    }

    private String transform(final String input, final boolean decrypt) {
        final StringBuilder output = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            final char c = input.charAt(i);
            final char upper = Character.toUpperCase(c);
            if (upper < 'A' || upper > 'Z') {
                output.append(c);
                continue;
            }
            final int x = upper - 'A';
            final int transformed = decrypt ? positiveMod(mInverseA * (x - mB)) : positiveMod(mA * x + mB);
            final char result = (char)('A' + transformed);
            output.append(Character.isLowerCase(c) ? Character.toLowerCase(result) : result);
        }
        return output.toString();
    }

    private static int normalizeCoprime(final int value) {
        final int normalized = positiveMod(value);
        return modularInverse(normalized) < 0 ? 5 : normalized;
    }

    private static int modularInverse(final int value) {
        for (int i = 1; i < 26; i++) {
            if (positiveMod(value * i) == 1) {
                return i;
            }
        }
        return -1;
    }

    private static int positiveMod(final int value) {
        return ((value % 26) + 26) % 26;
    }
}
