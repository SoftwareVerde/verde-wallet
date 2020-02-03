package com.softwareverde.bitcoin.app.android.store;

import android.content.Context;
import android.content.SharedPreferences;

import com.softwareverde.bitcoin.app.lib.KeyValueStore;

public class SharedPreferencesKeyValueStore implements KeyValueStore {

    protected final SharedPreferences _sharedPreferences;

    public SharedPreferencesKeyValueStore(final Context context, final String keyValueStoreName) {
        _sharedPreferences = context.getSharedPreferences(keyValueStoreName, Context.MODE_PRIVATE);
    }

    @Override
    public String getString(final String key) {
        return _sharedPreferences.getString(key, null);
    }

    @Override
    public void putString(final String key, final String value) {
        final SharedPreferences.Editor editor = _sharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    @Override
    public Boolean hasKey(final String key) {
        return _sharedPreferences.contains(key);
    }

    @Override
    public void removeKey(final String key) {
        final SharedPreferences.Editor editor = _sharedPreferences.edit();
        editor.remove(key);
        editor.apply();
    }

    @Override
    public void clear() {
        final SharedPreferences.Editor editor = _sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }
}
