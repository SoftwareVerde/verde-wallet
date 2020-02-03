package com.softwareverde.android.util;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.HashMap;
import java.util.Map;

public class QrUtil {
    protected QrUtil() { }

    protected static Bitmap _createQrCode(final String data, final int imageSize, final ErrorCorrectionLevel errorCorrectionLevel) throws WriterException {
        final Map<EncodeHintType, Object> encodingHints = new HashMap<>();
        encodingHints.put(EncodeHintType.ERROR_CORRECTION, errorCorrectionLevel);
        encodingHints.put(EncodeHintType.MARGIN, 1); // shrink margins

        final QRCodeWriter qrCodeWriter = new QRCodeWriter();
        final BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, imageSize, imageSize, encodingHints);

        return _toBitmap(bitMatrix);
    }

    protected static Bitmap _toBitmap(final BitMatrix bitMatrix) {
        final int width = bitMatrix.getWidth();
        final int height = bitMatrix.getHeight();
        final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                bitmap.setPixel(i, j, bitMatrix.get(i, j) ? Color.BLACK : Color.WHITE);
            }
        }
        return bitmap;
    }

    public static Bitmap createQrCodeBitmap(final String data, final Integer imageSize) {
        try {
            return _createQrCode(data, imageSize, ErrorCorrectionLevel.L);
        }
        catch (final Exception exception) {
            exception.printStackTrace(System.err);
            return null;
        }
    }
}
