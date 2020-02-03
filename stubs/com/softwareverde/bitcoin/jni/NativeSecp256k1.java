package com.softwareverde.bitcoin.jni;

public class NativeSecp256k1 {

    public static boolean isEnabled() {
        return false;
    }

    public static long getContext() {
        return -1;
    }

    public static boolean verify(byte[] data, byte[] signature, byte[] pub) {
        return false;
    }

    public static synchronized void shutdown() {
        return;
    }
}
