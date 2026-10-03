package org.dslul.openboard.inputmethod.latin.ciphers;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/** AES-256-CBC with encrypt-then-HMAC-SHA256 authentication for Unicode messages. */
public final class AesCbcHmacCipher implements MessageCipher {
    private static final String PREFIX = "STEG-AES-CBC-HMAC-1:";
    private static final String LEGACY_PREFIX = "OB-AES-CBC-HMAC-1:";
    private static final int SALT_BYTES = 16;
    private static final int IV_BYTES = 16;
    private static final int MAC_BYTES = 32;
    private static final int ITERATIONS = 210000;

    private final char[] mPassword;
    private final SecureRandom mRandom = new SecureRandom();

    public AesCbcHmacCipher(final String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password must not be empty");
        }
        mPassword = password.toCharArray();
    }

    @Override
    public String encrypt(final String input) {
        final byte[] salt = randomBytes(SALT_BYTES);
        final byte[] iv = randomBytes(IV_BYTES);
        try {
            final byte[][] keys = deriveKeys(salt);
            final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keys[0], "AES"),
                    new IvParameterSpec(iv));
            final byte[] encrypted = cipher.doFinal(input.getBytes(StandardCharsets.UTF_8));
            final byte[] authenticated = join(salt, iv, encrypted);
            return PREFIX + encode(join(authenticated, hmac(keys[1], authenticated)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalArgumentException("AES-CBC encryption is unavailable", exception);
        }
    }

    @Override
    public String decrypt(final String input) {
        final String prefix = getPrefix(input);
        if (prefix == null) {
            throw new IllegalArgumentException("Not a Steganographia AES-CBC message");
        }
        try {
            final byte[] payload = CipherBase64.decode(input.substring(prefix.length()));
            if (payload.length <= SALT_BYTES + IV_BYTES + MAC_BYTES) {
                throw new IllegalArgumentException("AES-CBC message is truncated");
            }
            final int macStart = payload.length - MAC_BYTES;
            final byte[] authenticated = slice(payload, 0, macStart);
            final byte[] suppliedMac = slice(payload, macStart, payload.length);
            final byte[] salt = slice(payload, 0, SALT_BYTES);
            final byte[][] keys = deriveKeys(salt);
            if (!MessageDigest.isEqual(suppliedMac, hmac(keys[1], authenticated))) {
                throw new IllegalArgumentException("AES-CBC authentication failed");
            }
            final byte[] iv = slice(payload, SALT_BYTES, SALT_BYTES + IV_BYTES);
            final byte[] encrypted = slice(payload, SALT_BYTES + IV_BYTES, macStart);
            final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keys[0], "AES"),
                    new IvParameterSpec(iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalArgumentException("Wrong password or damaged AES-CBC message",
                    exception);
        }
    }

    private static String getPrefix(final String input) {
        if (input != null && input.startsWith(PREFIX)) {
            return PREFIX;
        }
        return input != null && input.startsWith(LEGACY_PREFIX) ? LEGACY_PREFIX : null;
    }

    private byte[][] deriveKeys(final byte[] salt) throws GeneralSecurityException {
        final PBEKeySpec spec = new PBEKeySpec(mPassword, salt, ITERATIONS, 512);
        try {
            final byte[] material = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
                    .generateSecret(spec).getEncoded();
            return new byte[][] { slice(material, 0, 32), slice(material, 32, 64) };
        } finally {
            spec.clearPassword();
        }
    }

    private static byte[] hmac(final byte[] key, final byte[] input)
            throws GeneralSecurityException {
        final Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(input);
    }

    private byte[] randomBytes(final int length) {
        final byte[] bytes = new byte[length];
        mRandom.nextBytes(bytes);
        return bytes;
    }

    private static String encode(final byte[] bytes) {
        return CipherBase64.encode(bytes);
    }

    private static byte[] join(final byte[]... arrays) {
        int length = 0;
        for (byte[] array : arrays) {
            length += array.length;
        }
        final byte[] result = new byte[length];
        int offset = 0;
        for (byte[] array : arrays) {
            System.arraycopy(array, 0, result, offset, array.length);
            offset += array.length;
        }
        return result;
    }

    private static byte[] slice(final byte[] source, final int start, final int end) {
        final byte[] result = new byte[end - start];
        System.arraycopy(source, start, result, 0, result.length);
        return result;
    }
}
