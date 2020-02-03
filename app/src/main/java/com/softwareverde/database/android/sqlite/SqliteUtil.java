package com.softwareverde.database.android.sqlite;

import java.io.UnsupportedEncodingException;

public class SqliteUtil {
    public static final String STRING_ENCODING = "ISO-8859-1";

    public static String bytesToString(final byte[] bytes) {
        if (bytes == null) { return null; }

        try {
            return new String(bytes, STRING_ENCODING);
        }
        catch (final UnsupportedEncodingException exception) {
            exception.printStackTrace(System.err);
            return null;
        }
    }

    public static byte[] stringToBytes(final String string) {
        try {
            return string.getBytes(STRING_ENCODING);
        }
        catch (final UnsupportedEncodingException exception) {
            exception.printStackTrace(System.err);
            return null;
        }
    }

    protected SqliteUtil() { }
}
