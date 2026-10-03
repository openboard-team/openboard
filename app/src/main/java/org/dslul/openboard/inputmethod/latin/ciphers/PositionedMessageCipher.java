package org.dslul.openboard.inputmethod.latin.ciphers;

/** Cipher that can encrypt/decrypt text as if it started at a given stream position. */
public interface PositionedMessageCipher extends MessageCipher {
    String encrypt(String input, int position);
    String decrypt(String input, int position);
}
