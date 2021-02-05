package com.softwareverde.android.database.sqlite;

import android.content.Context;

import com.softwareverde.android.database.wrapper.SqliteDatabaseConnectionWrapper;
import com.softwareverde.bitcoin.server.database.DatabaseConnection;
import com.softwareverde.database.DatabaseConnectionFactory;

import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public class AndroidSqliteDatabaseConnectionFactory implements DatabaseConnectionFactory<Connection> {
    private final Context _context;
    private final String _databaseName;
    private final Integer _requiredDatabaseVersion;
    private final ThreadLocal<AndroidDatabaseHelper> _threadDatabaseHelper = new ThreadLocal<AndroidDatabaseHelper>();
    private final List<WeakReference<AndroidDatabaseHelper>> _databaseHelperReferences = new ArrayList<WeakReference<AndroidDatabaseHelper>>();

    public AndroidSqliteDatabaseConnectionFactory(final Context context, final String databaseName, final Integer requiredDatabaseVersion) {
        _context = context;
        _databaseName = databaseName;
        _requiredDatabaseVersion = requiredDatabaseVersion;
    }

    public AndroidDatabaseHelper getThreadDatabaseHelper() {
        return _threadDatabaseHelper.get();
    }

    @Override
    public DatabaseConnection newConnection() {
        AndroidDatabaseHelper databaseHelper = _threadDatabaseHelper.get();
        if (databaseHelper == null) {
            databaseHelper = new AndroidDatabaseHelper(_context, _databaseName, _requiredDatabaseVersion);
            databaseHelper.setWriteAheadLoggingEnabled(true);
            _threadDatabaseHelper.set(databaseHelper);
            synchronized (_databaseHelperReferences) {
                _databaseHelperReferences.add(new WeakReference<>(databaseHelper));
            }
        }

        final AndroidSqliteDatabaseConnection androidSqliteDatabaseConnection = new AndroidSqliteDatabaseConnection(databaseHelper, _databaseName);
        return new SqliteDatabaseConnectionWrapper(androidSqliteDatabaseConnection);
    }

    public void close() {
        while (! _databaseHelperReferences.isEmpty()) {
            final WeakReference<AndroidDatabaseHelper> reference = _databaseHelperReferences.remove(0);
            final AndroidDatabaseHelper androidDatabaseHelper = reference.get();
            if (androidDatabaseHelper != null) {
                androidDatabaseHelper.close();
            }
        }
    }
}
