package com.softwareverde.security.rsa;

public interface RsaKeys {
    byte[] getPublicKey();
    byte[] encrypt(final byte[] plainText);
    byte[] decrypt(final byte[] cipherText);
    byte[] sign(final byte[] data);
    Boolean verify(final byte[] data, final byte[] signature);
}
