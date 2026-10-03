package org.dslul.openboard.inputmethod.latin.ciphers;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/** Password-based AES-256-GCM authenticated encryption for arbitrary Unicode messages. */
public final class AesGcmCipher implements MessageCipher {
    private static final String PREFIX = "STEG-AES-GCM-1:";
    private static final String LEGACY_PREFIX = "OB-AES-GCM-1:";
    private static final int SALT_BYTES = 16;
    private static final int NONCE_BYTES = 12;
    private static final int KEY_BITS = 256;
    private static final int TAG_BITS = 128;
    private static final int ITERATIONS = 210000;

    private final char[] mPassword;
    private final SecureRandom mRandom = new SecureRandom();

    public AesGcmCipher(final String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password must not be empty");
        }
        mPassword = password.toCharArray();
    }

    @Override
    public String encrypt(final String input) {
        final byte[] salt = randomBytes(SALT_BYTES);
        final byte[] nonce = randomBytes(NONCE_BYTES);
        try {
            final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, deriveKey(salt),
                    new GCMParameterSpec(TAG_BITS, nonce));
            final byte[] encrypted = cipher.doFinal(input.getBytes(StandardCharsets.UTF_8));
            return PREFIX + encode(join(salt, nonce, encrypted));
        } catch (GeneralSecurityException exception) {
            throw new IllegalArgumentException("AES-GCM encryption is unavailable", exception);
        }
    }

    @Override
    public String decrypt(final String input) {
        final String prefix = getPrefix(input);
        if (prefix == null) {
            throw new IllegalArgumentException("Not a Steganographia AES-GCM message");
        }
        try {
            final byte[] payload = CipherBase64.decode(input.substring(prefix.length()));
            if (payload.length < SALT_BYTES + NONCE_BYTES + TAG_BITS / 8) {
                throw new IllegalArgumentException("AES-GCM message is truncated");
            }
            final byte[] salt = slice(payload, 0, SALT_BYTES);
            final byte[] nonce = slice(payload, SALT_BYTES, SALT_BYTES + NONCE_BYTES);
            final byte[] encrypted = slice(payload, SALT_BYTES + NONCE_BYTES, payload.length);
            final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, deriveKey(salt),
                    new GCMParameterSpec(TAG_BITS, nonce));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalArgumentException("Wrong password or damaged AES-GCM message",
                    exception);
        }
    }

    private static String getPrefix(final String input) {
        if (input != null && input.startsWith(PREFIX)) {
            return PREFIX;
        }
        return input != null && input.startsWith(LEGACY_PREFIX) ? LEGACY_PREFIX : null;
    }

    private SecretKeySpec deriveKey(final byte[] salt) throws GeneralSecurityException {
        final PBEKeySpec spec = new PBEKeySpec(mPassword, salt, ITERATIONS, KEY_BITS);
        try {
            final byte[] key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
                    .generateSecret(spec).getEncoded();
            return new SecretKeySpec(key, "AES");
        } finally {
            spec.clearPassword();
        }
    }

    private byte[] randomBytes(final int length) {
        final byte[] bytes = new byte[length];
        mRandom.nextBytes(bytes);
        return bytes;
    }

    private static String encode(final byte[] bytes) {
        return CipherBase64.encode(bytes);
    }

    private static byte[] join(final byte[] first, final byte[] second, final byte[] third) {
        final byte[] result = new byte[first.length + second.length + third.length];
        System.arraycopy(first, 0, result, 0, first.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        System.arraycopy(third, 0, result, first.length + second.length, third.length);
        return result;
    }

    private static byte[] slice(final byte[] source, final int start, final int end) {
        final byte[] result = new byte[end - start];
        System.arraycopy(source, start, result, 0, result.length);
        return result;
    }
}
