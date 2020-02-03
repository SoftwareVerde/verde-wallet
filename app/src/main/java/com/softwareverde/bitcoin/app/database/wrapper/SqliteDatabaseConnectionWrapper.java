package com.softwareverde.bitcoin.app.database.wrapper;

import com.softwareverde.bitcoin.server.database.DatabaseConnectionCore;
import com.softwareverde.database.android.sqlite.AndroidSqliteDatabaseConnection;

public class SqliteDatabaseConnectionWrapper extends DatabaseConnectionCore {
    public SqliteDatabaseConnectionWrapper(final AndroidSqliteDatabaseConnection core) {
        super(core);
    }

    @Override
    public Integer getRowsAffectedCount() {
        return ((AndroidSqliteDatabaseConnection) _core).getRowsAffectedCount();
    }
}
