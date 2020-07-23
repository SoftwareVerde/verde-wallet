package com.softwareverde.bitcoin.app.android.store;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Log;

import com.softwareverde.android.security.EncryptedKeyStore;
import com.softwareverde.android.security.SharedPreferencesEncryptedKeyStore;
import com.softwareverde.bitcoin.app.lib.KeyStore;
import com.softwareverde.bitcoin.app.lib.KeyValueStore;
import com.softwareverde.constable.bytearray.ByteArray;
import com.softwareverde.constable.bytearray.MutableByteArray;
import com.softwareverde.constable.list.List;
import com.softwareverde.constable.list.mutable.MutableList;
import com.softwareverde.cryptography.secp256k1.key.PrivateKey;
import com.softwareverde.cryptography.secp256k1.key.PublicKey;
import com.softwareverde.security.rsa.JcaRsaKeys;
import com.softwareverde.util.Util;

import java.security.InvalidAlgorithmParameterException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

public class AndroidKeyManager implements KeyStore {
    protected static final String TAG = AndroidKeyManager.class.getSimpleName();

    protected static final String SHARED_PREFERENCES_NAME = "android-key-manager";
    protected static final String ANDROID_KEY_STORE_NAME = "AndroidKeyStore";
    protected static final String ENCRYPTION_KEY_ALIAS = "privateKeyEncryptionKey";

    protected static class SharedPreferenceKeys {
        public static final String INIT_TIMESTAMP = "initTimestamp";
        public static final String PUBLIC_KEY_PREFIX = "publicKey";
        public static final String KEY_COUNT = "keyCount";
    }

    protected static final String CIPHER_NAME = "RSA/ECB/OAEPwithSHA-256andMGF1Padding";

    protected final Context _context;
    protected final KeyValueStore _keyValueStore;
    protected final EncryptedKeyStore _encryptedKeyStore;
    protected final KeyPair _keyPair;


    protected KeyPair _createKeyPair() throws NoSuchProviderException, NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        final KeyPairGenerator generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEY_STORE_NAME);
        generator.initialize(
            (new KeyGenParameterSpec.Builder(ENCRYPTION_KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT))
                .setBlockModes(KeyProperties.BLOCK_MODE_ECB)
                .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
                .build()
        );

        _encryptedKeyStore.setEncryptionKeyCipher(CIPHER_NAME);
        return generator.generateKeyPair();
    }


    protected PrivateKey _getPrivateKey(final String sourcePublicKeyString) {
        final byte[] encryptedPrivateKey = _encryptedKeyStore.loadEncryptedKey(sourcePublicKeyString);
        if (encryptedPrivateKey == null) { return null; }

        final String cipherName = _encryptedKeyStore.getEncryptionKeyCipher();
        return _decryptRsaPrivateKey(encryptedPrivateKey, cipherName);
    }

    protected byte[] _encryptRsaPrivateKey(final PrivateKey protectedKey, final String cipherName) {
        try {
            final byte[] protectedKeyBytes = protectedKey.getBytes();
            return _encryptRsa(protectedKeyBytes, cipherName);
        }
        catch (final Exception exception) {
            Log.e(TAG, "Unable to encrypt protected key", exception);
            return null;
        }
    }

    protected PrivateKey _decryptRsaPrivateKey(final byte[] encryptedPrivateKey, final String cipherName) {
        try {
            final byte[] plainText = _decryptRsa(encryptedPrivateKey, cipherName);
            return PrivateKey.fromBytes(MutableByteArray.wrap(plainText));
        }
        catch (final Exception exception) {
            Log.e(TAG, "Unable to decrypt protected key", exception);
            return null;
        }
    }

    protected byte[] _encryptRsa(final byte[] plainText, final String cipherName) {
        return JcaRsaKeys.encryptWithPublicKey(cipherName, _keyPair.getPublic(), plainText);
    }

    protected byte[] _decryptRsa(final byte[] cipherText, final String cipherName) {
        return JcaRsaKeys.decryptWithPrivateKey(cipherName, _keyPair.getPrivate(), cipherText);
    }

    protected String _getSharedPreferenceValue(final String key) {
        return Util.coalesce(_keyValueStore.getString(key));
    }

    protected PrivateKey _getPrivateKey(final Integer index) {
        final String primaryPublicKeyString = _getSharedPreferenceValue(SharedPreferenceKeys.PUBLIC_KEY_PREFIX + index);
        if (primaryPublicKeyString.isEmpty()) { return null; }

        final byte[] encryptedKey = _encryptedKeyStore.loadEncryptedKey(primaryPublicKeyString);
        if (encryptedKey == null) { return null; }

        final String encryptionKeyCipher = _encryptedKeyStore.getEncryptionKeyCipher();
        return _decryptRsaPrivateKey(encryptedKey, encryptionKeyCipher);
    }

    protected Integer _storePrivateKey(final PrivateKey privateKey) {
        final Integer keyIndex = _getKeyCount();

        final PublicKey publicKey = privateKey.getPublicKey();
        final PublicKey compressedPublicKey = publicKey.compress();

        final String encryptionKeyCipher = _encryptedKeyStore.getEncryptionKeyCipher();
        final byte[] encryptedKey = _encryptRsaPrivateKey(privateKey, encryptionKeyCipher);
        _encryptedKeyStore.storeEncryptedKey(encryptedKey, compressedPublicKey.toString());

        _keyValueStore.putString(SharedPreferenceKeys.KEY_COUNT, String.valueOf(keyIndex + 1));
        _keyValueStore.putString(SharedPreferenceKeys.PUBLIC_KEY_PREFIX + keyIndex, compressedPublicKey.toString());

        return keyIndex;
    }

    protected Integer _getKeyCount() {
        return Util.parseInt(_getSharedPreferenceValue(SharedPreferenceKeys.KEY_COUNT), 0);
    }

    public AndroidKeyManager(final Context context) {
        this(context, new SharedPreferencesEncryptedKeyStore(context));
    }

    public AndroidKeyManager(final Context context, final EncryptedKeyStore encryptedKeyStore) {
        _context = context.getApplicationContext();
        _encryptedKeyStore = encryptedKeyStore;
        _keyValueStore = new SharedPreferencesKeyValueStore(_context, SHARED_PREFERENCES_NAME);

        try {
            final java.security.KeyStore keyStore = java.security.KeyStore.getInstance(ANDROID_KEY_STORE_NAME);
            keyStore.load(null);
            final Key secretKey = keyStore.getKey(ENCRYPTION_KEY_ALIAS, null);
            if (secretKey == null) {
                // not yet stored, create
                _keyPair = _createKeyPair();

                Log.i(TAG, "Created " + _keyPair.getPrivate().getAlgorithm() + " keys in " + ANDROID_KEY_STORE_NAME);
                Log.i(TAG, "Using algorithm: " + _encryptedKeyStore.getEncryptionKeyCipher());
            }
            else {
                java.security.PrivateKey protectedKey = (java.security.PrivateKey) secretKey;
                java.security.PublicKey publicKey = keyStore.getCertificate(ENCRYPTION_KEY_ALIAS).getPublicKey();
                _keyPair = new KeyPair(publicKey, protectedKey);

                Log.i(TAG, "Loaded " + _keyPair.getPrivate().getAlgorithm() + " keys from " + ANDROID_KEY_STORE_NAME);
                Log.i(TAG, "Using algorithm: " + _encryptedKeyStore.getEncryptionKeyCipher());
            }
        }
        catch (final Exception exception) {
            throw new RuntimeException("Unable to initialize KeyManager.", exception);
        }
    }

    @Override
    public synchronized void setInitializationTimestamp(final Long initializationTimestamp) {
        _keyValueStore.putString(SharedPreferenceKeys.INIT_TIMESTAMP, (initializationTimestamp != null ? initializationTimestamp.toString() : null));
    }

    @Override
    public synchronized Boolean hasKeys() {
        return (_getKeyCount() > 0);
    }

    @Override
    public synchronized void storePrivateKey(final PrivateKey privateKey) {
        _storePrivateKey(privateKey);
    }

    @Override
    public synchronized PrivateKey getPrivateKey(final PublicKey publicKey) {
        final PublicKey compressedPublicKey = publicKey.compress();
        final int keyCount = _getKeyCount();
        for (int i = 0; i < keyCount; ++i) {
            final String compressedPublicKeyString = _getSharedPreferenceValue(SharedPreferenceKeys.PUBLIC_KEY_PREFIX + i);

            final PublicKey storedPublicKey = PublicKey.fromBytes(ByteArray.fromHexString(compressedPublicKeyString));
            final PublicKey storedCompressedPublicKey = storedPublicKey.compress();
            if (Util.areEqual(compressedPublicKey, storedCompressedPublicKey)) {
                return _getPrivateKey(i);
            }
        }

        return null;
    }

    @Override
    public synchronized PrivateKey createPrivateKey() {
        final PrivateKey privateKey = PrivateKey.createNewKey();
        _storePrivateKey(privateKey);
        return privateKey;
    }

    @Override
    public synchronized List<PrivateKey> getPrivateKeys() {
        final Integer keyCount = _getKeyCount();

        final MutableList<PrivateKey> privateKeys = new MutableList<PrivateKey>(keyCount);
        for (int i = 0; i < keyCount; ++i) {
            final PrivateKey privateKey = _getPrivateKey(i);
            privateKeys.add(privateKey);
        }
        return privateKeys;
    }

    @Override
    public synchronized Long getInitializationTimestamp() {
        return Util.parseLong(_getSharedPreferenceValue(SharedPreferenceKeys.INIT_TIMESTAMP), 0L);
    }

    @Override
    public synchronized void clear() {
        try {
            final java.security.KeyStore keyStore = java.security.KeyStore.getInstance(ANDROID_KEY_STORE_NAME);
            keyStore.load(null);
            keyStore.deleteEntry(ENCRYPTION_KEY_ALIAS);

            _keyValueStore.clear();
            _encryptedKeyStore.clear();
        }
        catch (final Exception exception) {
            throw new RuntimeException("Unable to delete KeyManager.", exception);
        }
    }
}
