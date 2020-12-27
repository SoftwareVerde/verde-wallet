package com.softwareverde.database.android.sqlite;

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.softwareverde.database.DatabaseConnection;
import com.softwareverde.database.android.sqlite.row.AndroidSqliteRowFactory;
import com.softwareverde.database.query.Query;
import com.softwareverde.database.query.parameter.TypedParameter;
import com.softwareverde.database.row.Row;
import com.softwareverde.util.Util;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class AndroidSqliteDatabaseConnection implements DatabaseConnection<Connection> {
    protected final String _databaseName;
    protected final SQLiteDatabase _sqliteDatabase;
    protected final AndroidSqliteRowFactory _rowFactory = new AndroidSqliteRowFactory();

    private static final ReentrantLock _transactionLock = new ReentrantLock();

    protected void _startTransaction() throws SQLException {
        _transactionLock.lock();
        _sqliteDatabase.beginTransaction();
    }

    protected String _convertInsertIgnore(final String queryString) {
        if (queryString.startsWith("INSERT IGNORE")) {
            return queryString.replaceFirst("INSERT IGNORE", "INSERT OR IGNORE");
        }

        return queryString;
    }

    protected String _convertOnDuplicateKeyUpdate(final String queryString) {
        final int startIndex = queryString.indexOf(" ON DUPLICATE KEY");
        if (startIndex >= 0) {
            return queryString.substring(0, startIndex).replaceFirst("INSERT", "INSERT OR REPLACE");
        }

        return queryString;
    }

    protected String _convertIsNull(final String queryString) {
        if (queryString.contains("IS NULL")) {
            return queryString.replaceAll("IS NULL", "IN (NULL, '')");
        }

        return queryString;
    }

    protected Long _getInsertId() {
        try (final Cursor cursor = _sqliteDatabase.rawQuery("SELECT last_insert_rowid()", null)) {
            if (cursor.moveToFirst()) {
                return cursor.getLong(0);
            }
        }

        return 0L;
    }

    protected String[] _parametersToStringArray(final Query query) {
        final List<TypedParameter> parameters = query.getParameters();
        final String[] parameterStrings = new String[parameters.size()];
        for (int i = 0; i < parameterStrings.length; ++i) {
            final TypedParameter typedParameter = parameters.get(i);
            if (typedParameter.value == null) {
                parameterStrings[i] = null;
                continue;
            }

            switch (typedParameter.type) {
                case BYTE_ARRAY: {
                    parameterStrings[i] = SqliteUtil.bytesToString((byte[]) typedParameter.value);
                } break;

                default: {
                    parameterStrings[i] = typedParameter.value.toString();
                }
            }
        }

        return parameterStrings;
    }

    protected List<Row> _query(final String query, final String[] parameters) {
        Cursor cursor;
        try {
            cursor = _sqliteDatabase.rawQuery(query, parameters);
        }
        catch (final IllegalArgumentException exception) {
            // Android's Sqlite wrapper does not support rawQueries with NULL values, and there is
            //  no completely awful way to execute a query with a null value in it unless ::insert,
            //  ::update, etc are used.  Sqlite doesn't actually enforce types, so therefore NULL
            //  values are converted to an empty string.  This unfortunately will cause all actual
            //  empty string values to be interpreted as NULL.
            final String[] nonNullParameters = new String[parameters.length];
            for (int j = 0; j < parameters.length; ++j) {
                final String parameter = parameters[j];
                nonNullParameters[j] = Util.coalesce(parameter);
            }
            cursor = _sqliteDatabase.rawQuery(query, nonNullParameters);
        }

        final List<Row> results = new ArrayList<Row>(cursor.getCount());

        int rowNumber = 0;
        if (cursor.moveToFirst()) {
            do {
                results.add(rowNumber, _rowFactory.fromCursor(cursor));
                rowNumber += 1;
            } while (cursor.moveToNext());
        }

        cursor.close();

        return results;
    }

    /**
     * Executes the query with the provided parameters and returns the last insert Id, if applicable.
     *  In order to bypass a quirk of Sqlite, if the query is not already in a transaction, a
     *  transaction is created between the query execution and the retrieval of the insert Id,
     *  otherwise the insert Id would not be returned.
     */
    protected Long _executeWithInsertId(final String queryString, final String[] parameters) {
        final boolean requiresMiniTransaction = (! _sqliteDatabase.inTransaction());
        if (requiresMiniTransaction) {
            _startTransaction();
        }

        try {
            _sqliteDatabase.execSQL(queryString, parameters);

            final Long insertId = _getInsertId();

            if (requiresMiniTransaction) {
                _sqliteDatabase.setTransactionSuccessful();
            }

            return insertId;
        }
        finally {
            if (requiresMiniTransaction) {
                _sqliteDatabase.endTransaction();
                _transactionLock.unlock();
            }
        }
    }

    public AndroidSqliteDatabaseConnection(final AndroidDatabaseHelper androidDatabaseHelper, final String databaseName) {
        _databaseName = databaseName;
        _sqliteDatabase = androidDatabaseHelper.getWritableDatabase();
    }

    @Override
    public synchronized void executeDdl(final String query) {
        _sqliteDatabase.execSQL(query);
    }

    @Override
    public synchronized void executeDdl(final Query query) {
        _sqliteDatabase.execSQL(query.getQueryString());
    }

    @Override
    public synchronized Long executeSql(final String query, final String[] parameters) {
        final String queryString = _convertIsNull(_convertOnDuplicateKeyUpdate(_convertInsertIgnore(query)));

        return _executeWithInsertId(queryString, parameters);
    }

    @Override
    public synchronized Long executeSql(final Query query) {
        final String queryString = _convertIsNull(_convertOnDuplicateKeyUpdate(_convertInsertIgnore(query.getQueryString())));
        final String[] parameterStrings = _parametersToStringArray(query);
        return _executeWithInsertId(queryString, parameterStrings);
    }

    @Override
    public synchronized List<Row> query(final String query, final String[] parameters) {
        final String queryString = _convertIsNull(query);
        return _query(queryString, parameters);
    }

    @Override
    public synchronized List<Row> query(final Query query) {
        final String queryString = _convertIsNull(query.getQueryString());
        final String[] parameterStrings = _parametersToStringArray(query);
        return _query(queryString, parameterStrings);
    }

    @Override
    public synchronized void close() {
        if (_sqliteDatabase.inTransaction()) {
            _sqliteDatabase.endTransaction();
            if (_transactionLock.isHeldByCurrentThread()) {
                _transactionLock.unlock();
            }
        }
    }

    @Override
    public SqliteJdbcConnectionWrapper getRawConnection() {
        return new SqliteJdbcConnectionWrapper(this);
    }

    public void startTransaction() throws SQLException {
        _startTransaction();
    }

    public void commitTransaction() {
        _sqliteDatabase.setTransactionSuccessful();
        _sqliteDatabase.endTransaction();
        _transactionLock.unlock();
    }

    public void rollbackTransaction() {
        _sqliteDatabase.endTransaction();
        _transactionLock.unlock();
    }

    public boolean getAutoCommit() {
        return ! _sqliteDatabase.inTransaction();
    }

    public synchronized Integer getRowsAffectedCount() {
        try (final Cursor cursor = _sqliteDatabase.rawQuery("SELECT changes()", null)) {
            if (cursor.moveToFirst()) {
                final Integer rowsAffected = cursor.getInt(0);
                return rowsAffected;
            }
        }
        return 0;
    }
}
