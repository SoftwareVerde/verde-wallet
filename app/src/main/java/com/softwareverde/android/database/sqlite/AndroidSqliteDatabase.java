package com.softwareverde.android.database.sqlite;

import android.content.Context;

import com.softwareverde.database.Database;

import java.sql.Connection;

public class AndroidSqliteDatabase extends AndroidSqliteDatabaseConnectionFactory implements Database<Connection> {
    public AndroidSqliteDatabase(final Context context, final String databaseName, final Integer requiredDatabaseVersion) {
        super(context, databaseName, requiredDatabaseVersion);
    }

    public Boolean shouldBeCreated() {
        final AndroidDatabaseHelper databaseHelper = getThreadDatabaseHelper();
        if (databaseHelper == null) {
            return false;
        }
        else {
            return databaseHelper.shouldBeCreated();
        }
    }
}
