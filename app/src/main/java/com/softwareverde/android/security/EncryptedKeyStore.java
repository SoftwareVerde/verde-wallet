package com.softwareverde.android.security;

public interface EncryptedKeyStore {
    void setEncryptionKeyCipher(final String cipherName);
    String getEncryptionKeyCipher();

    void storeEncryptedKey(final byte[] bytes, final String name);
    byte[] loadEncryptedKey(final String name);

    void clear();
}