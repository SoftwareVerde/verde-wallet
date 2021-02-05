package com.softwareverde.android.database.wrapper;


import com.softwareverde.bitcoin.server.database.DatabaseConnection;
import com.softwareverde.bitcoin.server.database.DatabaseConnectionFactory;
import com.softwareverde.android.database.sqlite.AndroidSqliteDatabaseConnectionFactory;

public class DatabaseConnectionFactoryWrapper implements DatabaseConnectionFactory {
    protected final AndroidSqliteDatabaseConnectionFactory _core;

    public DatabaseConnectionFactoryWrapper(final AndroidSqliteDatabaseConnectionFactory core) {
        _core = core;
    }

    @Override
    public DatabaseConnection newConnection() {
        return _core.newConnection();
    }

    @Override
    public void close() {
        _core.close();
    }
}
