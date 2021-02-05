package com.softwareverde.android.database.sqlite;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class AndroidDatabaseHelper extends SQLiteOpenHelper {
    private final Integer _requiredDatabaseVersion;
    private Integer _currentVersion;

    public AndroidDatabaseHelper(final Context context, final String databaseName, final Integer requiredDatabaseVersion) {
        super(context, databaseName, null, requiredDatabaseVersion);
        _requiredDatabaseVersion = requiredDatabaseVersion;
        _currentVersion = requiredDatabaseVersion;
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            close();
        }
        finally {
            super.finalize();
        }
    }

    @Override
    public void onCreate(final SQLiteDatabase db) {
        _currentVersion = null;
    }

    @Override
    public void onUpgrade(final SQLiteDatabase db, int oldVersion, int newVersion) {
        _currentVersion = oldVersion;
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        _currentVersion = oldVersion;
    }

    public Integer getVersion() {
        return _currentVersion;
    }

    public void setVersion(final Integer newVersion) {
        _currentVersion = newVersion;
    }

    public Boolean shouldBeCreated() {
        return (_currentVersion == null);
    }

    public Boolean shouldBeUpgraded() {
        return (_currentVersion != null && _currentVersion < _requiredDatabaseVersion);
    }

    public Boolean shouldBeDowngraded() {
        return (_currentVersion != null && _currentVersion > _requiredDatabaseVersion);
    }
}