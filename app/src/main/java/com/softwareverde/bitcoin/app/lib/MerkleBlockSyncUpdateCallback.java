package com.softwareverde.bitcoin.app.lib;

public interface MerkleBlockSyncUpdateCallback {
    void onMerkleBlockHeightUpdated(Long currentBlockHeight, Boolean isSynchronizing);
}
