package org.bitcoin;

import java.nio.ByteBuffer;

public class NativeSecp256k1 {
    public static void secp256k1_destroy_context(long context) {
        return;
    }

    public static int secp256k1_ecdsa_verify(ByteBuffer byteBuff, long context, int sigLen, int pubLen) {
        return -1;
    }
}