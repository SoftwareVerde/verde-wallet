package com.softwareverde.bitcoin.app.android;

import com.softwareverde.bitcoin.app.StringUtil;

import java.util.Locale;

public class BitcoinUtil extends com.softwareverde.bitcoin.util.BitcoinUtil {
    public static String spaceHex(final String input, final int segmentCharLength, final int lineCount) {
        final StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < input.length(); ++i) {
            stringBuilder.append(input.charAt(i));
            if ( (((i + 1) % segmentCharLength) == 0) && ((i + 1) < input.length()) ) {
                stringBuilder.append((i == ((input.length() / lineCount) - 1)) ? '\n' : ' ');
            }
        }
        return stringBuilder.toString();
    }

    public static String formatDollars(final Double amount) {
        final Long dollars = amount.longValue();
        final Double cents = (amount - dollars);

        final String centsString = String.format(Locale.US, "%.2f", cents);
        final String penniesString = centsString.substring(centsString.indexOf(".") + 1);

        return (StringUtil.formatNumberString(dollars) + "." + penniesString);
    }

    protected BitcoinUtil() { }
}
