package com.softwareverde.bitcoin.app;

public class StringUtil extends com.softwareverde.util.StringUtil {
    protected StringUtil() { }

    public static String formatNumberString(final Long number) {
        if (number == null) { return null; }
        return _numberFormatter.format(number);
    }
}
