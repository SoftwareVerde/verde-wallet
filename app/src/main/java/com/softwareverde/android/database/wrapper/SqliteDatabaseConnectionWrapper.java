package com.softwareverde.android.database.wrapper;

import com.softwareverde.bitcoin.server.database.DatabaseConnectionCore;
import com.softwareverde.android.database.sqlite.AndroidSqliteDatabaseConnection;

public class SqliteDatabaseConnectionWrapper extends DatabaseConnectionCore {
    public SqliteDatabaseConnectionWrapper(final AndroidSqliteDatabaseConnection core) {
        super(core);
    }

    @Override
    public Integer getRowsAffectedCount() {
        return ((AndroidSqliteDatabaseConnection) _core).getRowsAffectedCount();
    }
}
