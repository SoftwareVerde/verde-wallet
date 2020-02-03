package com.softwareverde.android.util;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.support.annotation.ColorInt;
import android.view.Display;
import android.view.WindowManager;

public class AndroidUtil {
    protected AndroidUtil() { }

    public static Point getDeviceSize(final Context context) {
        final WindowManager windowManager = (WindowManager) (context.getSystemService(Context.WINDOW_SERVICE));
        if (windowManager == null) { return null; }

        final Display display = windowManager.getDefaultDisplay();
        final Point deviceSize = new Point();
        display.getSize(deviceSize);

        return deviceSize;
    }

    public static void copyToClipboard(final String label, final String value, final Context context) {
        final ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) { return; }

        final ClipData clip = ClipData.newPlainText(label, value);
        clipboard.setPrimaryClip(clip);
    }

    @ColorInt
    public static int setAlpha(@ColorInt final int color, final float alphaPercent) {
        final int alpha = Math.round(255 * alphaPercent);
        final int red = Color.red(color);
        final int green = Color.green(color);
        final int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }
}
