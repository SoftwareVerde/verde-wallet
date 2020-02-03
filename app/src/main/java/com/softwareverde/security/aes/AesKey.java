package com.softwareverde.security.aes;

import com.softwareverde.logging.Logger;
import com.softwareverde.util.ByteUtil;
import com.softwareverde.util.bytearray.ByteArrayBuilder;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AesKey {
    public static final Integer DEFAULT_KEY_BIT_COUNT = 256;

    protected static final String KEY_ALGORITHM = "AES";
    protected static final String ENCRYPTION_CIPHER = "AES/GCM/NoPadding"; // Using GCM instead of CBC as it provides authentication
    protected static final Integer INITIALIZATION_VECTOR_LENGTH = 12; // IV size of 12-bytes is specifically recommended

    protected final SecretKey _key;

    protected SecretKey _createKey(final Integer keySize) {
        try {
            final KeyGenerator keyGenerator = KeyGenerator.getInstance(KEY_ALGORITHM);
            keyGenerator.init(keySize);
            return keyGenerator.generateKey();
        }
        catch (final NoSuchAlgorithmException exception) {
            Logger.error("Bad algorithm.", exception);
            return null;
        }
    }

    protected byte[] _createInitializationVector() {
        final byte[] initializationVector = new byte[INITIALIZATION_VECTOR_LENGTH];

        final SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(initializationVector);

        return initializationVector;
    }

    public AesKey() {
        this(DEFAULT_KEY_BIT_COUNT);
    }

    public AesKey(final Integer keySize) {
        _key = _createKey(keySize);
    }

    /**
     * <p>Uses the provided data as an AES encryption key. Must be a valid key length.</p>
     * @param key
     */
    public AesKey(final byte[] key) {
        _key = new SecretKeySpec(key, 0, key.length, KEY_ALGORITHM);
    }

    public SecretKey getKey() {
        return _key;
    }

    public byte[] getBytes() {
        return _key.getEncoded();
    }

    public byte[] encrypt(final byte[] plainText) {
        try {
            final Cipher aesCipher = Cipher.getInstance(ENCRYPTION_CIPHER);
            final byte[] initializationVectorBytes = _createInitializationVector();
            final AlgorithmParameterSpec initializationVector = new GCMParameterSpec(initializationVectorBytes.length * Byte.SIZE, initializationVectorBytes);
            aesCipher.init(Cipher.ENCRYPT_MODE, _key, initializationVector);
            final byte[] cipherText = aesCipher.doFinal(plainText);

            // prefix cipher text with initialization vector
            final ByteArrayBuilder byteArrayBuilder = new ByteArrayBuilder();
            byteArrayBuilder.appendByte((byte) initializationVectorBytes.length);
            byteArrayBuilder.appendBytes(initializationVectorBytes);
            byteArrayBuilder.appendBytes(cipherText);

            Arrays.fill(initializationVectorBytes, (byte) 0);

            return byteArrayBuilder.build();
        }
        catch (final Exception exception) {
            Logger.error("Unable to encrypt data.", exception);
            return null;
        }
    }

    public byte[] decrypt(final byte[] cipherText) {
        try {
            // Remove initialization vector from cipher text
            final byte initializationVectorLength = cipherText[0];
            final int cipherTextOffset = ByteUtil.byteToInteger(initializationVectorLength) + 1;
            final byte[] initializationVectorBytes = Arrays.copyOfRange(cipherText, 1, cipherTextOffset);
            final AlgorithmParameterSpec initializationVector = new GCMParameterSpec(initializationVectorLength * Byte.SIZE, initializationVectorBytes);
            final byte[] encryptedData = Arrays.copyOfRange(cipherText, cipherTextOffset, cipherText.length);

            final Cipher aesCipher = Cipher.getInstance(ENCRYPTION_CIPHER);
            aesCipher.init(Cipher.DECRYPT_MODE, _key, initializationVector);
            final byte[] plainText = aesCipher.doFinal(encryptedData);

            Arrays.fill(initializationVectorBytes, (byte) 0);

            return plainText;
        }
        catch (final Exception exception) {
            Logger.error("Unable to decrypt data.", exception);
            return null;
        }
    }
}
