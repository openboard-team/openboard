package org.dslul.openboard.inputmethod.latin.ciphers;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Enigma M3/M4-style cipher with configurable rotors, reflector, rings, positions, and plugboard.
 */
public final class EnigmaCipher implements PositionedMessageCipher {
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final Map<String, RotorSpec> ROTORS = new HashMap<>();
    private static final Map<String, String> REFLECTORS = new HashMap<>();

    static {
        ROTORS.put("I", new RotorSpec("EKMFLGDQVZNTOWYHXUSPAIBRCJ", "Q"));
        ROTORS.put("II", new RotorSpec("AJDKSIRUXBLHWTMCQGZNPYFVOE", "E"));
        ROTORS.put("III", new RotorSpec("BDFHJLCPRTXVZNYEIWGAKMUSQO", "V"));
        ROTORS.put("IV", new RotorSpec("ESOVPZJAYQUIRHXLNFTGKDCMWB", "J"));
        ROTORS.put("V", new RotorSpec("VZBRGITYUPSDNHLXAWMJQOFECK", "Z"));
        ROTORS.put("VI", new RotorSpec("JPGVOUMFYQBENHZRDKASXLICTW", "ZM"));
        ROTORS.put("VII", new RotorSpec("NZJHGRCXMYSWBOUFAIVLPEKQDT", "ZM"));
        ROTORS.put("VIII", new RotorSpec("FKQHTLXOCBJSPDZRAMEWNIUYGV", "ZM"));
        ROTORS.put("BETA", new RotorSpec("LEYJVCNIXWPBQMDRTAKZGFUHOS", ""));
        ROTORS.put("GAMMA", new RotorSpec("FSOKANUERHMBTIYCWLQPZXVGJD", ""));

        REFLECTORS.put("B", "YRUHQSLDPXNGOKMIEBFZCWVJAT");
        REFLECTORS.put("C", "FVPJIAOYEDRZXWGCTKUQSBNMHL");
        REFLECTORS.put("B THIN", "ENKQAUYWJICOPBLMDXZVFTHRGS");
        REFLECTORS.put("C THIN", "RDOBJNTKVEHMLFCWZAXGYIPSUQ");
    }

    private final Rotor[] mRotors;
    private final String mReflector;
    private final int[] mPlugboard;

    private EnigmaCipher(final Rotor[] rotors, final String reflector, final int[] plugboard) {
        mRotors = rotors;
        mReflector = reflector;
        mPlugboard = plugboard;
    }

    public static EnigmaCipher createM3(final String rotorNames, final String reflector,
            final String positions, final String rings, final String plugboardPairs) {
        final String[] names = normalizeRotorNames(rotorNames, new String[] {"I", "II", "III"});
        return new EnigmaCipher(new Rotor[] {
                createRotor(names[0], positions, rings, 0, true),
                createRotor(names[1], positions, rings, 1, true),
                createRotor(names[2], positions, rings, 2, true)
        }, normalizeReflector(reflector, "B"), buildPlugboard(plugboardPairs));
    }

    public static EnigmaCipher createM4(final String thinRotor, final String rotorNames,
            final String reflector, final String positions, final String rings,
            final String plugboardPairs) {
        final String[] names = normalizeRotorNames(rotorNames, new String[] {"I", "II", "III"});
        final String normalizedThinRotor = normalizeRotorName(thinRotor, "BETA");
        return new EnigmaCipher(new Rotor[] {
                createRotor(normalizedThinRotor, positions, rings, 0, false),
                createRotor(names[0], positions, rings, 1, true),
                createRotor(names[1], positions, rings, 2, true),
                createRotor(names[2], positions, rings, 3, true)
        }, normalizeReflector(reflector, "B THIN"), buildPlugboard(plugboardPairs));
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
        return transform(input, position);
    }

    @Override
    public String decrypt(final String input, final int position) {
        return transform(input, position);
    }

    private String transform(final String input, final int position) {
        final Rotor[] rotors = cloneRotors();
        for (int i = 0; i < position; i++) {
            stepRotors(rotors);
        }
        final StringBuilder output = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            final char c = input.charAt(i);
            final int letter = ALPHABET.indexOf(Character.toUpperCase(c));
            if (letter < 0) {
                output.append(c);
                continue;
            }
            stepRotors(rotors);
            int value = swap(letter, mPlugboard);
            for (int rotor = rotors.length - 1; rotor >= 0; rotor--) {
                value = rotors[rotor].forward(value);
            }
            value = ALPHABET.indexOf(mReflector.charAt(value));
            for (Rotor rotor : rotors) {
                value = rotor.backward(value);
            }
            value = swap(value, mPlugboard);
            final char encrypted = ALPHABET.charAt(value);
            output.append(Character.isLowerCase(c) ? Character.toLowerCase(encrypted) : encrypted);
        }
        return output.toString();
    }

    private Rotor[] cloneRotors() {
        final Rotor[] copy = new Rotor[mRotors.length];
        for (int i = 0; i < mRotors.length; i++) {
            copy[i] = mRotors[i].copy();
        }
        return copy;
    }

    private static void stepRotors(final Rotor[] rotors) {
        final int right = rotors.length - 1;
        final int middle = rotors.length - 2;
        final int left = rotors.length - 3;
        final boolean stepLeft = left >= 0 && rotors[middle].atNotch();
        final boolean stepMiddle = rotors[right].atNotch() || stepLeft;
        if (stepLeft) {
            rotors[left].step();
        }
        if (stepMiddle) {
            rotors[middle].step();
        }
        rotors[right].step();
    }

    private static Rotor createRotor(final String name, final String positions, final String rings,
            final int index, final boolean stepping) {
        final RotorSpec spec = ROTORS.get(normalizeRotorName(name, "I"));
        final int position = letterAt(positions, index) - 'A';
        final int ring = letterAt(rings, index) - 'A';
        return new Rotor(spec, position, ring, stepping);
    }

    private static String normalizeRotorName(final String name, final String defaultName) {
        final String normalized = name == null ? "" : name.trim().toUpperCase(Locale.US);
        return ROTORS.containsKey(normalized) ? normalized : defaultName;
    }

    private static String[] normalizeRotorNames(final String rotorNames, final String[] defaults) {
        final String[] parts = rotorNames == null ? new String[0]
                : rotorNames.trim().toUpperCase(Locale.US).split("[\\s,]+", -1);
        final String[] normalized = new String[defaults.length];
        for (int i = 0; i < defaults.length; i++) {
            normalized[i] = i < parts.length ? normalizeRotorName(parts[i], defaults[i]) : defaults[i];
        }
        return normalized;
    }

    private static String normalizeReflector(final String reflector, final String defaultReflector) {
        final String normalized = reflector == null ? "" : reflector.trim().toUpperCase(Locale.US);
        return REFLECTORS.containsKey(normalized) ? REFLECTORS.get(normalized)
                : REFLECTORS.get(defaultReflector);
    }

    private static int[] buildPlugboard(final String plugboardPairs) {
        final int[] plugboard = new int[ALPHABET.length()];
        for (int i = 0; i < plugboard.length; i++) {
            plugboard[i] = i;
        }
        if (plugboardPairs == null) {
            return plugboard;
        }
        final String[] pairs = plugboardPairs.toUpperCase(Locale.US).split("[\\s,]+");
        for (String pair : pairs) {
            pair = pair.replaceAll("[^A-Z]", "");
            if (pair.length() != 2) {
                continue;
            }
            final int first = ALPHABET.indexOf(pair.charAt(0));
            final int second = ALPHABET.indexOf(pair.charAt(1));
            if (first >= 0 && second >= 0 && plugboard[first] == first && plugboard[second] == second) {
                plugboard[first] = second;
                plugboard[second] = first;
            }
        }
        return plugboard;
    }

    private static int swap(final int value, final int[] plugboard) {
        return plugboard[value];
    }

    private static char letterAt(final String value, final int index) {
        if (value == null) {
            return 'A';
        }
        final String letters = value.toUpperCase(Locale.US).replaceAll("[^A-Z]", "");
        return index < letters.length() ? letters.charAt(index) : 'A';
    }

    private static final class RotorSpec {
        final String wiring;
        final String notches;

        RotorSpec(final String wiring, final String notches) {
            this.wiring = wiring;
            this.notches = notches;
        }
    }

    private static final class Rotor {
        final RotorSpec spec;
        final int ring;
        final boolean stepping;
        int position;

        Rotor(final RotorSpec spec, final int position, final int ring, final boolean stepping) {
            this.spec = spec;
            this.position = position;
            this.ring = ring;
            this.stepping = stepping;
        }

        Rotor copy() {
            return new Rotor(spec, position, ring, stepping);
        }

        void step() {
            if (stepping) {
                position = (position + 1) % ALPHABET.length();
            }
        }

        boolean atNotch() {
            if (!stepping) {
                return false;
            }
            for (int i = 0; i < spec.notches.length(); i++) {
                final int notch = ALPHABET.indexOf(spec.notches.charAt(i));
                if (position == positiveMod(notch - ring)) {
                    return true;
                }
            }
            return false;
        }

        int forward(final int value) {
            final int shifted = positiveMod(value + position - ring);
            final int wired = ALPHABET.indexOf(spec.wiring.charAt(shifted));
            return positiveMod(wired - position + ring);
        }

        int backward(final int value) {
            final int shifted = positiveMod(value + position - ring);
            final int wired = spec.wiring.indexOf(ALPHABET.charAt(shifted));
            return positiveMod(wired - position + ring);
        }

        private static int positiveMod(final int value) {
            return ((value % ALPHABET.length()) + ALPHABET.length()) % ALPHABET.length();
        }
    }
}
