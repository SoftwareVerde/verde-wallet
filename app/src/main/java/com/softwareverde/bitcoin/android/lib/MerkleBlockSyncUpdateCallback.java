package com.softwareverde.bitcoin.android.lib;

public interface MerkleBlockSyncUpdateCallback {
    void onMerkleBlockHeightUpdated(Long currentBlockHeight, Boolean isSynchronizing);
}
