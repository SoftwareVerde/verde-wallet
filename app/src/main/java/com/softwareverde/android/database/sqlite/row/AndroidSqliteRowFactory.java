package com.softwareverde.android.database.sqlite.row;

import android.database.Cursor;

import com.softwareverde.android.database.sqlite.SqliteUtil;

public class AndroidSqliteRowFactory {
    protected static Boolean _isBinaryType(final Integer sqlDataType) {
        switch (sqlDataType) {
            case android.database.Cursor.FIELD_TYPE_BLOB: {
                return true;
            }
            default: { return false; }
        }
    }

    public AndroidSqliteRow fromCursor(final Cursor cursor) {
        final AndroidSqliteRow androidSqliteRow = new AndroidSqliteRow();

        for (final String columnName : cursor.getColumnNames()) {
            final String columnValue;
            final int columnIndex = cursor.getColumnIndex(columnName);
            final boolean isBinaryType = _isBinaryType(cursor.getType(columnIndex));
            if (isBinaryType) {
                columnValue = SqliteUtil.bytesToString(cursor.getBlob(columnIndex));
            }
            else {
                columnValue = cursor.getString(columnIndex);
            }

            androidSqliteRow._columnNames.add(columnName);
            androidSqliteRow._columnValues.put(columnName, columnValue);
        }


        return androidSqliteRow;
    }
}
