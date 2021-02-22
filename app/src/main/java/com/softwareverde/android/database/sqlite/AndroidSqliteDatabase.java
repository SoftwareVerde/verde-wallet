package com.softwareverde.android.database.sqlite;

import android.content.Context;

import com.softwareverde.database.Database;
import com.softwareverde.logging.Logger;

import java.sql.Connection;

public class AndroidSqliteDatabase extends AndroidSqliteDatabaseConnectionFactory implements Database<Connection> {
    public AndroidSqliteDatabase(final Context context, final String databaseName, final Integer requiredDatabaseVersion) {
        super(context, databaseName, requiredDatabaseVersion);
    }

    public Boolean shouldBeCreated() {
        final AndroidDatabaseHelper databaseHelper = this.getThreadDatabaseHelper();
        if (databaseHelper == null) {
            Logger.error("Unable to get database helper.");
            return false;
        }

        return databaseHelper.shouldBeCreated();
    }
}
