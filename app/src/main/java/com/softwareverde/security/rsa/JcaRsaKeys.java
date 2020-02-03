package com.softwareverde.security.rsa;

import com.softwareverde.logging.Logger;

import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.MGF1ParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

public class JcaRsaKeys implements RsaKeys {
    private final KeyPair _keyPair;
    private final String _cipherName;

    protected static Cipher _createCipher(final String cipherName, final Integer mode, final Key key) throws GeneralSecurityException {
        final Cipher cipher = Cipher.getInstance(cipherName);
        // TODO: this could be made more robust/configurable, but isn't generally necessary unless you are trying to match compatibility to an existing system
        if (cipherName.contains("OAEP")) {
            // interprets as OAEPwithSHA-256andMGF1Padding but using MGF1 using SHA-1 (usually unspecified and/or implementation-specific)
            OAEPParameterSpec spec = new OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA1, PSource.PSpecified.DEFAULT);
            cipher.init(mode, key, spec);
        }
        else {
            cipher.init(mode, key);
        }
        return cipher;
    }

    public static byte[] encryptWithPublicKey(final String cipherName, final PublicKey publicKey, final byte[] plainText) {
        try {
            final Cipher cipher = _createCipher(cipherName, Cipher.ENCRYPT_MODE, publicKey);
            final byte[] cipherText = cipher.doFinal(plainText);
            return cipherText;
        }
        catch (final Exception exception) {
            Logger.error("Unable to perform encryption", exception);
            return null;
        }
    }

    public static byte[] decryptWithPrivateKey(final String cipherName, final PrivateKey privateKey, final byte[] cipherText) {
        try {
            final Cipher cipher = _createCipher(cipherName, Cipher.DECRYPT_MODE, privateKey);
            final byte[] plainText = cipher.doFinal(cipherText);
            return plainText;
        }
        catch (final Exception exception) {
            Logger.error("Unable to perform decryption", exception);
            return null;
        }
    }

    public static byte[] signWithPrivateKey(final PrivateKey privateKey, final byte[] data) {
        try {
            Signature signature = Signature.getInstance("SHA1withRSAandMGF1");
            signature.initSign(privateKey);
            signature.update(data);
            return signature.sign();
        }
        catch (final Exception exception) {
            Logger.error("Unable to sign data", exception);
            return null;
        }
    }

    public static Boolean verifySignatureWithPublicKey(final PublicKey publicKey, final byte[] data, final byte[] signature) {
        try {
            final Signature rsaVerify = Signature.getInstance("SHA1withRSAandMGF1");
            rsaVerify.initVerify(publicKey);
            rsaVerify.update(data);
            return rsaVerify.verify(signature);
        }
        catch (final Exception exception) {
            Logger.error("Unable to verify signature", exception);
            return false;
        }
    }

    public JcaRsaKeys(final KeyPair keyPair, final String cipherName) {
        _keyPair = keyPair;
        _cipherName = cipherName;
    }

    @Override
    public byte[] getPublicKey() {
        return _keyPair.getPublic().getEncoded();
    }

    @Override
    public byte[] encrypt(final byte[] plainText) {
        return JcaRsaKeys.encryptWithPublicKey(_cipherName, _keyPair.getPublic(), plainText);
    }

    @Override
    public byte[] decrypt(final byte[] cipherText) {
        return JcaRsaKeys.decryptWithPrivateKey(_cipherName, _keyPair.getPrivate(), cipherText);
    }

    @Override
    public byte[] sign(final byte[] data) {
        return JcaRsaKeys.signWithPrivateKey(_keyPair.getPrivate(), data);
    }

    @Override
    public Boolean verify(final byte[] data, final byte[] signature) {
        return JcaRsaKeys.verifySignatureWithPublicKey(_keyPair.getPublic(), data, signature);
    }

}
