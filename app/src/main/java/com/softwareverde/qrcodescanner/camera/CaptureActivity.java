package com.softwareverde.qrcodescanner.camera;

import android.graphics.Bitmap;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;

import com.google.zxing.Result;

public abstract class CaptureActivity extends AppCompatActivity {
    protected abstract Handler getHandler();

    protected abstract ViewfinderView getViewfinderView();

    protected abstract CameraManager getCameraManager();

    protected abstract void handleDecode(final Result obj, final Bitmap barcode, final float scaleFactor);

    protected abstract void drawViewfinder();
}
