package com.softwareverde.android.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

public class SharedPreferencesEncryptedKeyStore implements EncryptedKeyStore {
    protected static final String ENCRYPTION_SHARED_PREFERENCES_NAME = "EncryptedPrivateKeys";
    protected static final String ENCRYPTION_KEY_CIPHER_PROPERTY_NAME = "encryptionKeyCipher";

    protected final Context _context;

    public SharedPreferencesEncryptedKeyStore(final Context context) {
        _context = context;
    }

    @Override
    public void setEncryptionKeyCipher(final String cipherName) {
        final SharedPreferences.Editor editor = _context.getSharedPreferences(ENCRYPTION_SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).edit();
        editor.putString(ENCRYPTION_KEY_CIPHER_PROPERTY_NAME, cipherName);
        editor.apply();
    }

    @Override
    public String getEncryptionKeyCipher() {
        final SharedPreferences sharedPreferences = _context.getSharedPreferences(ENCRYPTION_SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(ENCRYPTION_KEY_CIPHER_PROPERTY_NAME, null);
    }

    @Override
    public void storeEncryptedKey(final byte[] bytes, final String name) {
        final SharedPreferences.Editor editor = _context.getSharedPreferences(ENCRYPTION_SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).edit();
        final String valueBase64String = Base64.encodeToString(bytes, Base64.NO_WRAP);
        editor.putString(name, valueBase64String);
        editor.apply();
    }

    @Override
    public byte[] loadEncryptedKey(final String name) {
        final SharedPreferences sharedPreferences = _context.getSharedPreferences(ENCRYPTION_SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        final String encryptedKeyBase64 = sharedPreferences.getString(name, null);
        if (encryptedKeyBase64 == null) {
            return null;
        }
        return Base64.decode(encryptedKeyBase64, Base64.NO_WRAP);
    }

    @Override
    public void clear() {
        final SharedPreferences sharedPreferences = _context.getSharedPreferences(ENCRYPTION_SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);

        final SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }
}
