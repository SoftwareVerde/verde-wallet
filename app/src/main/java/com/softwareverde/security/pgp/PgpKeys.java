package com.softwareverde.security.pgp;

import com.softwareverde.logging.Logger;
import com.softwareverde.security.rsa.BouncyCastleRsaKeys;
import com.softwareverde.security.rsa.RsaKeys;
import com.softwareverde.util.Util;

import org.bouncycastle.bcpg.HashAlgorithmTags;
import org.bouncycastle.bcpg.PublicKeyAlgorithmTags;
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.bouncycastle.bcpg.sig.Features;
import org.bouncycastle.bcpg.sig.KeyFlags;
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator;
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPEncryptedData;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPKeyPair;
import org.bouncycastle.openpgp.PGPKeyRingGenerator;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureSubpacketGenerator;
import org.bouncycastle.openpgp.PGPSignatureSubpacketVector;
import org.bouncycastle.openpgp.operator.PBESecretKeyDecryptor;
import org.bouncycastle.openpgp.operator.PBESecretKeyEncryptor;
import org.bouncycastle.openpgp.operator.PGPDigestCalculator;
import org.bouncycastle.openpgp.operator.bc.BcKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.bc.BcPBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPBESecretKeyEncryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider;
import org.bouncycastle.openpgp.operator.bc.BcPGPKeyPair;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Date;
import java.util.Iterator;

/**
 * <p>Provides mechanisms for generating, loading, and using PGP keys.</p>
 *
 * <p>Based on code found <a href="http://sloanseaman.com/wordpress/2012/05/13/revisited-pgp-encryptiondecryption-in-java/">here</a>
 * and <a href="https://bouncycastle-pgp-cookbook.blogspot.com/">here</a>.</p>
 */
public class PgpKeys {
    protected static final String DEFAULT_KEY_STORE_BASE_FILENAME = "keystore";
    protected static final BigInteger RSA_PUBLIC_EXPONENT = BigInteger.valueOf(0x010001); // 2^16 + 1
    protected static final Integer RSA_KEY_LENGTH = 4096;
    protected static final Integer RSA_S2K_COUNT = 192; // ~130,000 hash iterations
    protected static final Integer RSA_CERTAINTY = 12;

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    protected static PGPKeyRingGenerator generateKeyRingGenerator(final String id, final char[] password) throws PGPException {
        final RSAKeyGenerationParameters rsaKeyGenerationParameters = new RSAKeyGenerationParameters(RSA_PUBLIC_EXPONENT, new SecureRandom(), RSA_KEY_LENGTH, RSA_CERTAINTY);

        final RSAKeyPairGenerator rsaKeyPairGenerator = new RSAKeyPairGenerator();
        rsaKeyPairGenerator.init(rsaKeyGenerationParameters);

        final PGPKeyPair signatureKeyPair = new BcPGPKeyPair(PGPPublicKey.RSA_SIGN, rsaKeyPairGenerator.generateKeyPair(), new Date());
        final PGPKeyPair encryptionKeyPair = new BcPGPKeyPair(PGPPublicKey.RSA_ENCRYPT, rsaKeyPairGenerator.generateKeyPair(), new Date());

        final PGPKeyRingGenerator keyRingGenerator = PgpKeys.createSelfSignedKeyRingGenerator(signatureKeyPair, id, password);
        PgpKeys.signAndAddEncryptionKeyPair(keyRingGenerator, encryptionKeyPair);

        return keyRingGenerator;
    }

    protected static File getPublicKeyPath(final File configurationDirectory, final String keyStoreBaseFilename) {
        return new File(configurationDirectory.getAbsolutePath() + File.separator + keyStoreBaseFilename + ".pkr");
    }

    protected static File getPrivateKeyPath(final File configurationDirectory, final String keyStoreBaseFilename) {
        return new File(configurationDirectory.getAbsolutePath() + File.separator + keyStoreBaseFilename + ".skr");
    }

    protected static PGPKeyRingGenerator createSelfSignedKeyRingGenerator(final PGPKeyPair signatureKeyPair, final String id, final char[] password) throws PGPException {
        final PGPSignatureSubpacketGenerator signatureSubpacketGenerator = new PGPSignatureSubpacketGenerator();
        signatureSubpacketGenerator.setKeyFlags(false, KeyFlags.SIGN_DATA | KeyFlags.CERTIFY_OTHER);
        signatureSubpacketGenerator.setPreferredSymmetricAlgorithms(false, new int[]{
            SymmetricKeyAlgorithmTags.AES_256,
            SymmetricKeyAlgorithmTags.AES_192,
            SymmetricKeyAlgorithmTags.AES_128
        });
        signatureSubpacketGenerator.setPreferredHashAlgorithms(false, new int[]{
            HashAlgorithmTags.SHA256,
            HashAlgorithmTags.SHA512,
            HashAlgorithmTags.SHA384,
            HashAlgorithmTags.SHA224
        });
        // request additional checksums on messages
        signatureSubpacketGenerator.setFeature(false, Features.FEATURE_MODIFICATION_DETECTION);

        final PGPDigestCalculator sha256Calc = new BcPGPDigestCalculatorProvider().get(HashAlgorithmTags.SHA256);
        final BcPBESecretKeyEncryptorBuilder secretKeyEncryptorBuilder = new BcPBESecretKeyEncryptorBuilder(PGPEncryptedData.AES_256, sha256Calc, RSA_S2K_COUNT);
        final PBESecretKeyEncryptor secretKeyEncryptor = secretKeyEncryptorBuilder.build(password);

        final PGPSignatureSubpacketVector signatureSubpacketVector = signatureSubpacketGenerator.generate();

        // SHA-1 is apparently required here by the PGP standard
        // See:
        //      http://bouncy-castle.1462172.n4.nabble.com/SHA-1-Collision-is-it-okay-to-use-for-checksum-td4658560.html#a4658566
        //
        final PGPDigestCalculator sha1Calc = new BcPGPDigestCalculatorProvider().get(HashAlgorithmTags.SHA1);
        final BcPGPContentSignerBuilder contentSignerBuilder = new BcPGPContentSignerBuilder(signatureKeyPair.getPublicKey().getAlgorithm(), HashAlgorithmTags.SHA1);
        final PGPKeyRingGenerator keyRingGenerator = new PGPKeyRingGenerator(PGPSignature.POSITIVE_CERTIFICATION, signatureKeyPair, id, sha1Calc, signatureSubpacketVector, null, contentSignerBuilder, secretKeyEncryptor);
        return keyRingGenerator;
    }

    protected static void signAndAddEncryptionKeyPair(final PGPKeyRingGenerator keyRingGenerator, final PGPKeyPair encryptionKeyPair) throws PGPException {
        final PGPSignatureSubpacketGenerator signatureSubpacketGenerator = new PGPSignatureSubpacketGenerator();
        signatureSubpacketGenerator.setKeyFlags(false, KeyFlags.ENCRYPT_COMMS | KeyFlags.ENCRYPT_STORAGE);

        final PGPSignatureSubpacketVector encryptionSubpacketVector = signatureSubpacketGenerator.generate();
        keyRingGenerator.addSubKey(encryptionKeyPair, encryptionSubpacketVector, null);
    }


    public static void clear(final File configurationDirectory, final String keyStoreBaseFilename) {
        final File publicKeyPath = PgpKeys.getPublicKeyPath(configurationDirectory, keyStoreBaseFilename);
        final File privateKeyPath = PgpKeys.getPrivateKeyPath(configurationDirectory, keyStoreBaseFilename);
        try {
            final boolean publicKeyPathDeleteWasSuccessful = publicKeyPath.delete();
            if (! publicKeyPathDeleteWasSuccessful) {
                throw new IOException("Unable to delete public key file: " + publicKeyPath.getAbsolutePath());
            }

            final boolean privateKeyPathDeleteWasSuccessful = privateKeyPath.delete();
            if (! privateKeyPathDeleteWasSuccessful) {
                throw new IOException("Unable to delete private key file: " + privateKeyPath.getAbsolutePath());
            }
        }
        catch (final Exception exception) {
            Logger.error("Unable to delete local PGP keys.", exception);
        }
    }

    /**
     * <p>Checks for PGP keys at the specified directory and returns true if both the public and private keyRings are present.</p>
     *
     * <p>Alternately, returns true if the specified directory is null, as it is assumed that the PGP keys are not stored on disk
     * and are therefore always available.</p>
     */
    public static Boolean doLocalPgpKeysExist(final File configurationDirectory, final String keyStoreBaseFilename) {
        if (configurationDirectory == null) { return true; } // If PGP Keys are not stored on disk then return true.

        final File publicKeyPath = PgpKeys.getPublicKeyPath(configurationDirectory, keyStoreBaseFilename);
        final File privateKeyPath = PgpKeys.getPrivateKeyPath(configurationDirectory, keyStoreBaseFilename);

        return (publicKeyPath.exists() && privateKeyPath.exists());
    }

    protected final String _keyStoreBaseFilename;
    protected PGPPublicKeyRing _publicKeyRing;
    protected PGPSecretKeyRing _secretKeyRing;

    protected void _init(final InputStream publicKeyRingInputStream, final InputStream secretKeyRingInputStream) throws IOException, PGPException {
        _publicKeyRing = new PGPPublicKeyRing(publicKeyRingInputStream, new BcKeyFingerprintCalculator());
        _secretKeyRing = new PGPSecretKeyRing(secretKeyRingInputStream, new BcKeyFingerprintCalculator());
    }

    protected void _store(final File configurationDirectory, final String keyStoreBaseFilename) throws IOException {
        final File publicKeyPath = PgpKeys.getPublicKeyPath(configurationDirectory, keyStoreBaseFilename);
        final File privateKeyPath = PgpKeys.getPrivateKeyPath(configurationDirectory, keyStoreBaseFilename);

        try (
            final FileOutputStream publicKeyRingOutputStream = new FileOutputStream(publicKeyPath);
            final FileOutputStream secretKeyRingOutputStream = new FileOutputStream(privateKeyPath)
        ) {
            _publicKeyRing.encode(publicKeyRingOutputStream);
            _secretKeyRing.encode(secretKeyRingOutputStream);
        }
        catch (final Exception exception) {
            Logger.error("Unable to generate PGP keys", exception);
            throw new IOException(exception);
        }
    }

    protected PGPKeyRingGenerator _generateKeyRingGeneratorForNewPassword(final String id, final char[] currentPassword, final char[] newPassword) throws PGPException {
        PGPKeyPair signatureKeyPair = null;
        PGPKeyPair encryptionKeyPair = null;

        for (final Iterator<PGPSecretKey> iterator = _secretKeyRing.getSecretKeys(); iterator.hasNext(); ) {
            final PGPSecretKey secretKey = iterator.next();
            final PBESecretKeyDecryptor decryptor = new BcPBESecretKeyDecryptorBuilder(new BcPGPDigestCalculatorProvider()).build(currentPassword);
            final PGPPrivateKey privateKey = secretKey.extractPrivateKey(decryptor);

            if (secretKey.getPublicKey().getAlgorithm() == PublicKeyAlgorithmTags.RSA_SIGN) {
                signatureKeyPair = new PGPKeyPair(secretKey.getPublicKey(), privateKey);
            }
            else if (secretKey.getPublicKey().getAlgorithm() == PublicKeyAlgorithmTags.RSA_ENCRYPT) {
                encryptionKeyPair = new PGPKeyPair(secretKey.getPublicKey(), privateKey);
            }
        }

        if ( (signatureKeyPair == null) || (encryptionKeyPair == null) ) {
            throw new IllegalStateException("Unable to find both matching key pairs.");
        }

        final PGPKeyRingGenerator keyRingGenerator = PgpKeys.createSelfSignedKeyRingGenerator(signatureKeyPair, id, newPassword);
        PgpKeys.signAndAddEncryptionKeyPair(keyRingGenerator, encryptionKeyPair);

        return keyRingGenerator;
    }

    protected RsaKeys _extractRsaKeyPair(final char[] password, final Integer algorithm) throws PGPException {
        for (final Iterator<PGPSecretKey> iterator = _secretKeyRing.getSecretKeys(); iterator.hasNext(); ) {
            final PGPSecretKey secretKey = iterator.next();
            if (Util.areEqual(secretKey.getPublicKey().getAlgorithm(), algorithm)) {
                final PBESecretKeyDecryptor decryptor = new BcPBESecretKeyDecryptorBuilder(new BcPGPDigestCalculatorProvider()).build(password);
                final PGPPrivateKey privateKey = secretKey.extractPrivateKey(decryptor);
                return new BouncyCastleRsaKeys(secretKey.getPublicKey(), privateKey);
            }
        }
        throw new IllegalStateException("Unable to find matching key pair for algorithm: " + algorithm);
    }

    protected PGPPublicKey _getPublicKey(final Integer algorithm) {
        for (final Iterator<PGPPublicKey> iterator = _publicKeyRing.getPublicKeys(); iterator.hasNext(); ) {
            final PGPPublicKey publicKey = iterator.next();
            if (Util.areEqual(publicKey.getAlgorithm(), algorithm)) {
                return publicKey;
            }
        }
        throw new IllegalStateException("Unable to find public key for algorithm: " + algorithm);
    }

    /**
     * <p>Attempts to load PGP keys from disk.</p>
     */
    public PgpKeys(final File configurationDirectory, final String keyStoreBaseFilename) throws IOException, PGPException {
        _keyStoreBaseFilename = keyStoreBaseFilename;

        final File publicKeyPath = PgpKeys.getPublicKeyPath(configurationDirectory, keyStoreBaseFilename);
        final File privateKeyPath = PgpKeys.getPrivateKeyPath(configurationDirectory, keyStoreBaseFilename);

        try (
            final FileInputStream publicKeyRingInputStream = new FileInputStream(publicKeyPath);
            final FileInputStream secretKeyRingInputStream = new FileInputStream(privateKeyPath)
        ) {
            _init(publicKeyRingInputStream, secretKeyRingInputStream);
        }
    }

    /**
     * Created an object that only contains the public key ring.  Used when accessing another person's keys.
     * @param publicKeyRingData
     */
    public PgpKeys(final byte[] publicKeyRingData) throws IOException {
        _keyStoreBaseFilename = DEFAULT_KEY_STORE_BASE_FILENAME;
        final ByteArrayInputStream publicKeyRingInputStream = new ByteArrayInputStream(publicKeyRingData);
        _publicKeyRing = new PGPPublicKeyRing(publicKeyRingInputStream, new BcKeyFingerprintCalculator());
    }

    public PgpKeys(final byte[] publicKeyRingData, final byte[] secretKeyRingData) throws IOException, PGPException {
        _keyStoreBaseFilename = DEFAULT_KEY_STORE_BASE_FILENAME;
        _init(new ByteArrayInputStream(publicKeyRingData), new ByteArrayInputStream(secretKeyRingData));
    }

    /**
     * <p>Creates a new set of PGP key rings with the provided ID.  The secret key ring will be encrypted with the provided password.</p>
     * @param id
     * @param password
     * @throws PGPException
     * @throws IOException
     */
    public PgpKeys(final String id, final char[] password, final File configurationDirectory, final String keyStoreBaseFilename) throws PGPException, IOException {
        this(id, password, true, configurationDirectory, keyStoreBaseFilename);
    }

    public PgpKeys(final String id, final char[] password, final Boolean shouldStoreKeys, final File configurationDirectory, final String keyStoreBaseFilename) throws PGPException, IOException {
        final PGPKeyRingGenerator pgpKeyRingGenerator = PgpKeys.generateKeyRingGenerator(id, password);

        _keyStoreBaseFilename = keyStoreBaseFilename;
        _publicKeyRing = pgpKeyRingGenerator.generatePublicKeyRing();
        _secretKeyRing = pgpKeyRingGenerator.generateSecretKeyRing();

        if (shouldStoreKeys) {
            _store(configurationDirectory, keyStoreBaseFilename);
        }
    }

    public void store(final File configurationDirectory, final String keyStoreBaseFilename) throws IOException {
        _store(configurationDirectory, keyStoreBaseFilename);
    }

    public void generateAndStoreKeyRingsForNewPassword(final String id, final char[] currentPassword, final char[] newPassword) throws PGPException {
        final PGPKeyRingGenerator pgpKeyRingGenerator = _generateKeyRingGeneratorForNewPassword(id, currentPassword, newPassword);

        // A new public key ring is not generated because it is not necessary to complete the password change process.
        // Additionally, storing the newly generated public key could cause issues with shared documents.
        _secretKeyRing = pgpKeyRingGenerator.generateSecretKeyRing();
    }

    public RsaKeys getRsaSigningKeys(final char[] password) throws PGPException {
        return _extractRsaKeyPair(password, PGPPublicKey.RSA_SIGN);
    }

    public RsaKeys getRsaEncryptionKeys(final char[] password) throws PGPException {
        return _extractRsaKeyPair(password, PGPPublicKey.RSA_ENCRYPT);
    }

    public PGPPublicKeyRing getPublicKeyRing() {
        return _publicKeyRing;
    }

    public PGPPublicKey getEncryptionKey() {
        return _getPublicKey(PGPPublicKey.RSA_ENCRYPT);
    }

    public PGPPublicKey getSignatureVerificationKey() {
        return _getPublicKey(PGPPublicKey.RSA_SIGN);
    }

    public PGPSecretKeyRing getSecretKeyRing() {
        return _secretKeyRing;
    }

    public byte[] rsaEncrypt(final byte[] plainText) {
        return BouncyCastleRsaKeys.encryptWithPublicKey(getEncryptionKey(), plainText);
    }

    public byte[] rsaDecrypt(final byte[] cipherText, final char[] password) throws PGPException {
        final RsaKeys rsaKeys = getRsaEncryptionKeys(password);
        return rsaKeys.decrypt(cipherText);
    }

    public byte[] rsaSign(final byte[] data, final char[] password) throws PGPException {
        final RsaKeys rsaKeys = getRsaSigningKeys(password);
        return rsaKeys.sign(data);
    }

    public boolean rsaVerify(final byte[] data, final byte[] signature) {
        return BouncyCastleRsaKeys.verifyWithPublicKey(getSignatureVerificationKey(), data, signature);
    }

    public boolean hasMatchingPublicKeys(final PgpKeys pgpKeys) throws IOException {
        return (
            (pgpKeys != null)
            && Util.areEqual(this.getEncryptionKey().getEncoded(), pgpKeys.getEncryptionKey().getEncoded())
            && Util.areEqual(this.getSignatureVerificationKey().getEncoded(), pgpKeys.getSignatureVerificationKey().getEncoded())
        );
    }
}
