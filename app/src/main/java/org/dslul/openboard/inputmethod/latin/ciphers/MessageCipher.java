package org.dslul.openboard.inputmethod.latin.ciphers;

/**
 * Common contract for ciphers that can be exposed from the cipher tools UI.
 */
public interface MessageCipher {
    String encrypt(String input);
    String decrypt(String input);
}
