package com.softwareverde.bitcoin.app.lib;

import com.softwareverde.constable.list.List;
import com.softwareverde.cryptography.secp256k1.key.PrivateKey;
import com.softwareverde.cryptography.secp256k1.key.PublicKey;

public interface KeyStore {
    Boolean hasKeys();

    PrivateKey createPrivateKey();
    void storePrivateKey(final PrivateKey privateKey);

    PrivateKey getPrivateKey(PublicKey publicKey);
    List<PrivateKey> getPrivateKeys();

    /**
     * Sets the timestamp that the keyStore was initialized, in seconds.
     */
    void setInitializationTimestamp(Long initializationTimestamp);

    /**
     * Returns the timestamp that the keyStore was initialized, in seconds.
     *  This value may be used to determine how far back in history the system should check for transactions.
     */
    Long getInitializationTimestamp();

    void clear();
}
