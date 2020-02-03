package com.softwareverde.database.android.sqlite.row;

import com.softwareverde.database.android.sqlite.SqliteUtil;
import com.softwareverde.database.row.Row;
import com.softwareverde.util.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AndroidSqliteRow implements Row {
    protected final List<String> _columnNames = new ArrayList<String>();
    protected final Map<String, String> _columnValues = new HashMap<String, String>();

    protected String _getString(final String columnName) {
        if (! _columnValues.containsKey(columnName)) {
            throw new IllegalArgumentException("Row does not contain column: " + columnName);
        }

        return _columnValues.get(columnName);
    }

    protected AndroidSqliteRow() { }

    @Override
    public String getString(final String columnName) {
        return _getString(columnName);
    }

    @Override
    public List<String> getColumnNames() {
        return Util.copyList(_columnNames);
    }

    @Override
    public Integer getInteger(final String columnName) {
        return Util.parseInt(_getString(columnName));
    }

    @Override
    public Long getLong(final String columnName) {
        return Util.parseLong(_getString(columnName));
    }

    @Override
    public Float getFloat(final String columnName) {
        return Util.parseFloat(_getString(columnName));
    }

    @Override
    public Double getDouble(final String columnName) {
        return Util.parseDouble(_getString(columnName));
    }

    @Override
    public Boolean getBoolean(final String columnName) {
        return Util.parseBool(_getString(columnName));
    }

    @Override
    public byte[] getBytes(final String columnName) {
        return SqliteUtil.stringToBytes(_getString(columnName));
    }
}
