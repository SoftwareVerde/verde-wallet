package com.softwareverde.bitcoin.app.database;

import com.softwareverde.bitcoin.app.database.wrapper.DatabaseConnectionFactoryWrapper;
import com.softwareverde.bitcoin.server.database.Database;
import com.softwareverde.bitcoin.server.database.DatabaseConnection;
import com.softwareverde.bitcoin.server.database.DatabaseConnectionFactory;
import com.softwareverde.database.DatabaseException;
import com.softwareverde.database.android.sqlite.AndroidSqliteDatabase;

public class VerdeWalletDatabase implements Database {
    protected final AndroidSqliteDatabase _core;
    protected final DatabaseConnectionFactory _databaseConnectionFactory;

    protected DatabaseConnectionFactory _newConnectionFactory() {
        return new DatabaseConnectionFactoryWrapper(_core);
    }

    public VerdeWalletDatabase(final AndroidSqliteDatabase core) {
        _core = core;
        _databaseConnectionFactory = _newConnectionFactory();
    }

    @Override
    public DatabaseConnection newConnection() throws DatabaseException {
        return _databaseConnectionFactory.newConnection();
    }

    @Override
    public DatabaseConnectionFactory newConnectionFactory() {
        return _newConnectionFactory();
    }

    @Override
    public void close() throws DatabaseException {
        _databaseConnectionFactory.close();
    }
}
