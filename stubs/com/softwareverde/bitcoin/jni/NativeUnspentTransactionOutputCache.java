package com.softwareverde.bitcoin.jni;

public class NativeUnspentTransactionOutputCache {
    public static void _init() {
        return;
    }

    public static  void _destroy() {
    }

    public static  int _createCache() {
        return -1;
    }

    public static  void _deleteCache(int instanceId) {
        return;
    }

    public static  void _cacheUnspentTransactionOutputId(int instanceId, byte[] transactionHash, int transactionOutputIndex, long transactionOutputId) {
        return;
    }

    public static  long _getCachedUnspentTransactionOutputId(int instanceId, byte[] transactionHash, int transactionOutputIndex) {
        return -1;
    }

    public static  void _setMasterCache(int instanceId, int masterCacheId) {
        return;
    }

    public static  void _invalidateUnspentTransactionOutputId(int instanceId, byte[] transactionHash, int transactionOutputIndex) {
        return;
    }

    public static  void _commit(int instanceId, int masterCacheId) {
        return;
    }

    public static  void _commit(int instanceId) {
        return;
    }

    // Used for the initial load of UTXOs (usually in reverse order)...
    public static  void _loadUnspentTransactionOutputId(int instanceId, long insertId, byte[] transactionHash, int transactionOutputIndex, long transactionOutputId) {
        return;
    }

    public static  void _setMaxItemCount(int instanceId, long maxItemCount) {
        return;
    }

    public static  void _pruneHalf(int instanceId) {
        return;
    }
}
