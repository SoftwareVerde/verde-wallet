package com.softwareverde.bitcoin.android.lib;

import com.google.j2objc.annotations.AutoreleasePool;
import com.google.j2objc.annotations.WeakOuter;
import com.softwareverde.async.ConcurrentHashSet;
import com.softwareverde.bitcoin.address.Address;
import com.softwareverde.bitcoin.address.AddressInflater;
import com.softwareverde.bitcoin.block.BlockId;
import com.softwareverde.bitcoin.block.header.BlockHeader;
import com.softwareverde.bitcoin.block.header.BlockHeaderInflater;
import com.softwareverde.bitcoin.chain.segment.BlockchainSegmentId;
import com.softwareverde.bitcoin.chain.time.MedianBlockTime;
import com.softwareverde.bitcoin.server.Environment;
import com.softwareverde.bitcoin.server.configuration.CheckpointConfiguration;
import com.softwareverde.bitcoin.server.configuration.NodeProperties;
import com.softwareverde.bitcoin.server.database.Database;
import com.softwareverde.bitcoin.server.database.DatabaseConnection;
import com.softwareverde.bitcoin.server.database.DatabaseConnectionFactory;
import com.softwareverde.bitcoin.server.database.pool.DatabaseConnectionPool;
import com.softwareverde.bitcoin.server.database.query.Query;
import com.softwareverde.bitcoin.server.message.type.node.feature.NodeFeatures;
import com.softwareverde.bitcoin.server.module.node.database.block.header.BlockHeaderDatabaseManager;
import com.softwareverde.bitcoin.server.module.node.database.block.spv.SpvBlockDatabaseManager;
import com.softwareverde.bitcoin.server.module.node.database.blockchain.BlockchainDatabaseManager;
import com.softwareverde.bitcoin.server.module.node.database.spv.SpvDatabaseManager;
import com.softwareverde.bitcoin.server.module.node.database.transaction.TransactionDatabaseManager;
import com.softwareverde.bitcoin.server.module.node.database.transaction.spv.SlpValidity;
import com.softwareverde.bitcoin.server.module.node.database.transaction.spv.SpvTransactionDatabaseManager;
import com.softwareverde.bitcoin.server.module.node.manager.BitcoinNodeManager;
import com.softwareverde.bitcoin.server.module.node.manager.NodeFilter;
import com.softwareverde.bitcoin.server.module.node.manager.banfilter.BanFilter;
import com.softwareverde.bitcoin.server.module.node.manager.banfilter.BanFilterCore;
import com.softwareverde.bitcoin.server.module.spv.SpvModule;
import com.softwareverde.bitcoin.server.node.BitcoinNode;
import com.softwareverde.bitcoin.slp.SlpTokenId;
import com.softwareverde.bitcoin.transaction.Transaction;
import com.softwareverde.bitcoin.transaction.TransactionId;
import com.softwareverde.bitcoin.transaction.output.identifier.TransactionOutputIdentifier;
import com.softwareverde.bitcoin.wallet.SeedPhraseGenerator;
import com.softwareverde.bitcoin.wallet.Wallet;
import com.softwareverde.bloomfilter.MutableBloomFilter;
import com.softwareverde.concurrent.pool.cached.CachedThreadPool;
import com.softwareverde.constable.bytearray.MutableByteArray;
import com.softwareverde.constable.list.List;
import com.softwareverde.constable.list.mutable.MutableList;
import com.softwareverde.cryptography.hash.sha256.Sha256Hash;
import com.softwareverde.cryptography.secp256k1.key.PrivateKey;
import com.softwareverde.cryptography.secp256k1.key.PublicKey;
import com.softwareverde.database.DatabaseException;
import com.softwareverde.database.row.Row;
import com.softwareverde.database.util.TransactionUtil;
import com.softwareverde.logging.Logger;
import com.softwareverde.network.ip.Ip;
import com.softwareverde.network.p2p.node.NodeId;
import com.softwareverde.util.Container;
import com.softwareverde.util.HexUtil;
import com.softwareverde.util.IoUtil;
import com.softwareverde.util.Util;
import com.softwareverde.util.type.time.SystemTime;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class BitcoinVerde {

    public enum Status {
        INITIALIZING            ("Initializing"),
        STARTING                ("Starting"),
        BOOTSTRAPPING           ("Bootstrapping Blocks"),
        INDEXING                ("Rebuilding Indexes"),
        INITIALIZING_DATABASE   ("Initializing Database"),
        ONLINE                  ("Online"),
        OFFLINE                 ("Offline");

        private final String value;
        Status(final String value) { this.value = value; }

        public String getValue() { return this.value; }
    }

    public interface PriceIndexer {
        Double getDollarsPerBitcoin();
    }

    public static class InitData {
        public Database database;
        public InputStream bootstrapHeaders;
        public KeyStore keyStore;
        public KeyValueStore keyValueStore;
        public List<String> seedPhraseWords;
        public PriceIndexer priceIndexer;
        public Boolean shouldOnlyConnectToSeedNodes;
        public List<NodeProperties> seedNodes;
    }

    protected static InitData INIT_DATA = null;
    public static void init(final InitData initData) {
        INIT_DATA = initData;
    }

    public static BitcoinVerde getInstance() {
        if (INSTANCE != null) { return INSTANCE; }

        synchronized (MUTEX) {
            if (INSTANCE != null) { return INSTANCE; }
            if (INIT_DATA == null) { return null; }

            INSTANCE = new BitcoinVerde(INIT_DATA);
            return INSTANCE;
        }
    }

    public interface NewTransactionCallback {
        void onNewTransaction(Transaction transaction);
    }

    public interface TransactionValidityChangedCallback {
        void onTransactionValidityChanged(Sha256Hash transactionHash, SlpValidity validity);
    }

    protected static final Object MUTEX = new Object();
    protected static BitcoinVerde INSTANCE = null;
    protected static final Long BOOTSTRAP_BLOCK_COUNT = 575000L;

    protected static class KeyValueStoreKeys {
        public static final String CHANGE_ADDRESS = "change_address";
        public static final String CHANGE_ADDRESS_IS_COMPRESSED = "change_address_is_compressed";

        protected KeyValueStoreKeys() { }
    }

    public static void runSqlInitFile(final com.softwareverde.database.DatabaseConnection<?> databaseConnection, final String sqlFileContents) throws DatabaseException {
        for (final String query : sqlFileContents.split(";")) {
            if (Util.isBlank(query)) { continue; }
            databaseConnection.executeDdl(query);
        }
    }

    protected static void _clearBlockTransactionsTable(final DatabaseConnection databaseConnection) throws DatabaseException {
        databaseConnection.executeSql(
            new Query("DELETE FROM block_transactions")
        );
    }

    protected static void _clearTransactionDataTable(final DatabaseConnection databaseConnection) throws DatabaseException {
        databaseConnection.executeSql(
            new Query("DELETE FROM transaction_data")
        );
    }

    protected static void _clearTransactionsTable(final DatabaseConnection databaseConnection) throws DatabaseException {
        databaseConnection.executeSql(
            new Query("DELETE FROM transactions")
        );
    }

    protected static void _clearBlockMerkleTreesTable(final DatabaseConnection databaseConnection) throws DatabaseException {
        databaseConnection.executeSql(
            new Query("DELETE FROM block_merkle_trees")
        );
    }

    protected final SystemTime _systemTime = new SystemTime();

    protected Status _status = Status.OFFLINE;
    protected Runnable _onStatusUpdatedCallback = null;
    protected NewTransactionCallback _newTransactionCallback;
    protected TransactionValidityChangedCallback _transactionValidityChangedCallback;
    protected final ConcurrentHashSet<Runnable> _walletUpdatedCallbacks = new ConcurrentHashSet<Runnable>();
    protected final CheckpointConfiguration _checkpointConfiguration = new CheckpointConfiguration();

    protected Thread _initThread = null;
    protected volatile Boolean _abortInit = false;
    protected final CachedThreadPool _mainThreadPool;

    protected Environment _environment;
    protected KeyStore _secureKeyStore;
    protected KeyValueStore _keyValueStore;

    protected final Object _initPin = new Object();
    protected Boolean _isInitialized = false;
    protected InitData _initData;
    protected SpvModule _spvModule;
    protected Thread _spvThread = null;
    protected Thread _pingThread;
    protected Boolean _isConnected = false;
    protected Double _dollarsPerBitcoin = null;

    protected SeedPhraseGenerator _seedPhraseGenerator;

    protected final ConcurrentHashSet<Runnable> _slpTokenChangedCallbacks = new ConcurrentHashSet<Runnable>();
    protected SlpTokenId _slpTokenId = null;

    protected Runnable _onSynchronizationComplete = null;
    protected Runnable _onInitComplete = null;
    protected Runnable _onConnectedNodesChanged = null;
    protected Long _currentBlockHeight = 0L;

    protected final Wallet _wallet = new Wallet();

    protected final ConcurrentLinkedQueue<NewBlockHeaderCallback> _newBlockHeaderCallbacks = new ConcurrentLinkedQueue<NewBlockHeaderCallback>();
    protected final ConcurrentLinkedQueue<MerkleBlockSyncUpdateCallback> _merkleBlockSyncUpdateCallbacks = new ConcurrentLinkedQueue<MerkleBlockSyncUpdateCallback>();

    protected PriceIndexer _priceIndexer;

    protected void _shutdown() {
        { // Shutdown the InitThread if it's still running...
            final Thread initThread = _initThread;
            _initThread = null;

            if (initThread != null) {
                _abortInit = true;
                try { Thread.sleep(5000L); }  catch (final Exception exception) { }
                initThread.interrupt();
                try { initThread.join(5000L); } catch (final Exception exception) { }
            }
        }

        { // Shutdown the PingThread, SpvThread, and Database...
            final Thread pingThread = _pingThread;
            _pingThread = null;

            if (pingThread != null) {
                pingThread.interrupt();
            }

            final Thread spvThread = _spvThread;
            _spvThread = null;

            if (spvThread != null) {
                spvThread.interrupt();
            }

            final InitData initData = _initData;
            _initData = null;

            if (initData != null && initData.database != null) {
                try {
                    initData.database.close();
                }
                catch (final Exception exception) {
                    Logger.error(exception);
                }
            }

            _spvModule = null;
            _environment = null;

            if (spvThread != null) {
                try { spvThread.join(5000L); } catch (final Exception exception) { Logger.error(exception); }
            }
            if (pingThread != null) {
                try { pingThread.join(5000L); } catch (final Exception exception) { Logger.error(exception); }
            }
        }

        _setStatus(Status.OFFLINE);

        BitcoinVerde.INIT_DATA = null;
        BitcoinVerde.INSTANCE = null;

        Logger.debug("BitcoinVerde has completely shut down.");
        _mainThreadPool.stop();
    }

    protected void _setStatus(final Status status) {
        final Status previousStatus = _status;
        _status = status;

        if (previousStatus != status) {
            final Runnable callback = _onStatusUpdatedCallback;
            if (callback != null) {
                _mainThreadPool.execute(callback);
            }
        }
    }

    protected void _waitForInitialization() {
        if (_isInitialized) { return; }

        synchronized (_initPin) {
            if (_isInitialized) { return; }
            try { _initPin.wait(); } catch (final Exception exception) { }
        }
    }

    /**
     * Sets the SpvModule MinimumMerkleBlockHeight based on the initialization timestamp of the KeyStore.
     *  If the headers have not finished synchronizing, then the height may be further in the past than specified by the KeyStore's initialization timestamp.
     *  It is safe to call this function multiple times.
     */
    @AutoreleasePool protected void _initMinMerkleBlockHeight() {
        final Long minMerkleBlockHeight;
        {
            final Long minBlockTime = _secureKeyStore.getInitializationTimestamp();
            Long minBlockHeight = 570247L;

            final Database database = _environment.getDatabase();
            try (final DatabaseConnection databaseConnection = database.newConnection()) {
                final SpvDatabaseManager databaseManager = new SpvDatabaseManager(databaseConnection, database.getMaxQueryBatchSize(), _checkpointConfiguration);
                final BlockchainDatabaseManager blockchainDatabaseManager = databaseManager.getBlockchainDatabaseManager();
                final BlockHeaderDatabaseManager blockHeaderDatabaseManager = databaseManager.getBlockHeaderDatabaseManager();

                final Thread currentThread = Thread.currentThread();
                final BlockchainSegmentId blockchainSegmentId = blockchainDatabaseManager.getHeadBlockchainSegmentId();
                BlockId blockId = blockHeaderDatabaseManager.getHeadBlockHeaderId();
                for (@AutoreleasePool boolean shouldContinue = _shouldContinue(currentThread); shouldContinue; shouldContinue = _shouldContinue(currentThread)) {
                    if (blockId == null) {
                        minBlockHeight = 0L;
                        break;
                    }

                    final Long blockHeight = blockHeaderDatabaseManager.getBlockHeight(blockId);
                    final Long blockTimestamp = blockHeaderDatabaseManager.getBlockTimestamp(blockId);
                    if (blockTimestamp <= minBlockTime) {
                        minBlockHeight = blockHeaderDatabaseManager.getBlockHeight(blockId);
                        break;
                    }

                    final long timestampDifference = (blockTimestamp - minBlockTime);
                    final long goBackBlockCount = (timestampDifference / (10 * 60));
                    blockId = blockHeaderDatabaseManager.getBlockIdAtHeight(blockchainSegmentId, (blockHeight - goBackBlockCount));

                    if (goBackBlockCount == 0) {
                        minBlockHeight = blockHeight;
                        break;
                    }
                }
            }
            catch (final DatabaseException exception) {
                Logger.error(exception);
            }

            minMerkleBlockHeight = minBlockHeight;
        }

        Logger.info("MinimumMerkleBlockHeight: " + minMerkleBlockHeight);
        _spvModule.setMinimumMerkleBlockHeight(minMerkleBlockHeight);
    }

    protected void _synchronizeMerkleBlocks() {
        _spvModule.synchronizeMerkleBlocks();

        { // Download AddressBlocks if connected to an indexing node...
            final List<PrivateKey> privateKeys = _secureKeyStore.getPrivateKeys();

            final BitcoinNodeManager bitcoinNodeManager = _spvModule.getBitcoinNodeManager();
            @WeakOuter final NodeFilter nodeFilter = bitcoinNode -> bitcoinNode.hasFeatureEnabled(NodeFeatures.Feature.BLOCKCHAIN_INDEX_ENABLED);
            final BitcoinNode bitcoinVerdeNode = bitcoinNodeManager.getNode(nodeFilter);

            final AddressInflater addressInflater = new AddressInflater();
            final MutableList<Address> addresses = new MutableList<Address>();
            for (final PrivateKey privateKey : privateKeys) {
                final Address address = addressInflater.fromPrivateKey(privateKey, false);
                final Address compressedAddress = addressInflater.fromPrivateKey(privateKey, true);

                addresses.add(address);
                addresses.add(compressedAddress);
            }

            if (bitcoinVerdeNode != null) {
                Logger.info("Requesting AddressBlocks from: " + bitcoinVerdeNode.getConnectionString());
                if (Logger.isDebugEnabled()) {
                    for (final Address address : addresses) {
                        Logger.debug(address.toBase58CheckEncoded());
                    }
                }
                bitcoinVerdeNode.getAddressBlocks(addresses);
            }
            else {
                Logger.info("No indexing node connected.");
            }
        }
    }

    /**
     * Loads the private keys from the KeyStore.
     *  If keys have not been stored then keys are created and stored.
     */
    protected void _initPrivateKeys() {
        if (! _secureKeyStore.hasKeys()) {
            Logger.debug("No keys found in keystore, creating a private key");
            _secureKeyStore.setInitializationTimestamp(_systemTime.getCurrentTimeInSeconds());
            _secureKeyStore.createPrivateKey();
        }

        final List<PrivateKey> privateKeys = _secureKeyStore.getPrivateKeys();

        Logger.debug("Storing " + privateKeys.getCount() + " private keys in wallet");
        _wallet.addPrivateKeys(privateKeys);

        for (final Runnable walletUpdatedCallback : _walletUpdatedCallbacks) {
            if (walletUpdatedCallback != null) {
                _mainThreadPool.execute(walletUpdatedCallback);
            }
        }
    }

    protected void _initialize() {
        _setStatus(Status.INITIALIZING);
        @WeakOuter final BanFilter banFilter = new BanFilterCore(null) {
            @Override
            public Boolean isIpBanned(final Ip ip) { return false; }

            @Override
            public void banIp(final Ip ip) { }
        };

        _setStatus(Status.INITIALIZING_DATABASE);

        final Database database = _initData.database;
        final WeakReference<Database> databaseWeakReference = new WeakReference<>(_initData.database);
        @WeakOuter final DatabaseConnectionPool databaseConnectionPool = new DatabaseConnectionPool() {
            protected final DatabaseConnectionFactory _databaseConnectionFactory = databaseWeakReference.get().newConnectionFactory();

            @Override
            public DatabaseConnection newConnection() throws DatabaseException {
                return _databaseConnectionFactory.newConnection();
            }

            @Override
            public void close() {
                // Nothing.
            }
        };
        _environment = new Environment(_initData.database, databaseConnectionPool);
        _secureKeyStore = _initData.keyStore;
        _keyValueStore = _initData.keyValueStore;
        _priceIndexer = _initData.priceIndexer;

        _initPrivateKeys();

        _setStatus(Status.STARTING);
        final List<NodeProperties> seedNodes = _initData.seedNodes;

        { // Unban any seed nodes... TODO: Reconfigure as whitelist.
            // NOTE: BitcoinNodeDatabaseManager::setIsBanned is not used because the setIsBanned function performs a "DELETE...INNER JOIN" which is not supported by Sqlite.
            try (final DatabaseConnection databaseConnection = database.newConnection()) {
                for (final NodeProperties seedNodeProperties : seedNodes) {
                    final Ip nodeIp = Ip.fromHostName(seedNodeProperties.getAddress());
                    if (nodeIp == null) { continue; }

                    final Long hostId;
                    {
                        final java.util.List<Row> rows = databaseConnection.query(
                            new Query("SELECT id FROM hosts WHERE host = ?")
                                    .setParameter(nodeIp)
                        );

                        if (rows.isEmpty()) { continue; }
                        final Row row = rows.get(0);

                        hostId = row.getLong("id");
                    }

                    databaseConnection.executeSql(
                        new Query("DELETE FROM nodes WHERE nodes.last_handshake_timestamp IS NULL AND host_id = ?")
                            .setParameter(hostId)
                    );

                    databaseConnection.executeSql(
                        new Query("UPDATE hosts SET is_banned = 0 WHERE id = ?")
                                .setParameter(hostId)
                    );
                }
            }
            catch (final DatabaseException exception) {
                Logger.error(exception);
            }
        }

        try (final DatabaseConnection databaseConnection = database.newConnection()) {
            final SpvDatabaseManager databaseManager = new SpvDatabaseManager(databaseConnection, database.getMaxQueryBatchSize(), _checkpointConfiguration);
            final BlockHeaderDatabaseManager blockHeaderDatabaseManager = databaseManager.getBlockHeaderDatabaseManager();

            final BlockId headBlockHeaderId = blockHeaderDatabaseManager.getHeadBlockHeaderId();
            final long maxDatFileHeight = (BOOTSTRAP_BLOCK_COUNT - 1L);
            final long startingHeight = (headBlockHeaderId == null ? 0L : (blockHeaderDatabaseManager.getBlockHeight(headBlockHeaderId) + 1));
            if (startingHeight < maxDatFileHeight) {
                long currentBlockHeight = startingHeight;

                _setStatus(Status.BOOTSTRAPPING);
                final BlockHeaderInflater blockHeaderInflater = new BlockHeaderInflater();

                final Container<Long> blockHeightContainer = new Container<Long>(currentBlockHeight);
                @WeakOuter final Runnable blockHeaderRunnable = () -> {
                    try {
                        while (blockHeightContainer.value != null) {
                            synchronized (blockHeightContainer) {
                                blockHeightContainer.wait();
                            }

                            final Long blockHeight = blockHeightContainer.value;
                            if (blockHeight == null) { return; }

                            for (final NewBlockHeaderCallback callback : _newBlockHeaderCallbacks) {
                                try {
                                    callback.newBlockHeight(blockHeight);
                                }
                                catch (final Exception ignored) { }
                            }
                        }
                    }
                    catch (final Exception exception) {
                        Logger.error(exception);
                    }
                };
                _mainThreadPool.execute(blockHeaderRunnable);

                long lastUiUpdateTime = 0L;

                Logger.info("Bootstrapping started with block " + startingHeight + "...");

                try (final InputStream inputStream = _initData.bootstrapHeaders) {
                    inputStream.skip(startingHeight * BlockHeaderInflater.BLOCK_HEADER_BYTE_COUNT);

                    final MutableByteArray buffer = new MutableByteArray(BlockHeaderInflater.BLOCK_HEADER_BYTE_COUNT);

                    final int batchSize = 4096;
                    final MutableList<BlockHeader> batchedHeaders = new MutableList<BlockHeader>(batchSize);

                    final Thread currentThread = Thread.currentThread();
                    for (@AutoreleasePool boolean shouldContinue = _shouldContinue(currentThread); shouldContinue; shouldContinue = _shouldContinue(currentThread)) {
                        int readByteCount = IoUtil.readBytesFromStream(inputStream, buffer.unwrap());

                        if (readByteCount != BlockHeaderInflater.BLOCK_HEADER_BYTE_COUNT) {
                            if (readByteCount != 0) {
                                Logger.warn("Partial header read: " + HexUtil.toHexString(buffer.getBytes(0, readByteCount)) + " (" + readByteCount + " bytes)");
                            }
                            break;
                        }

                        final BlockHeader blockHeader = blockHeaderInflater.fromBytes(buffer);
                        if (blockHeader == null) {
                            Logger.warn("Unable to inflate header from bytes: " + buffer.toString());
                            break;
                        }

                        batchedHeaders.add(blockHeader);
                        if (batchedHeaders.getCount() == batchSize) {
                            synchronized (BlockHeaderDatabaseManager.MUTEX) {
                                TransactionUtil.startTransaction(databaseConnection);

                                final List<BlockId> blockIds;
                                try {
                                    try { databaseConnection.executeSql(new Query("PRAGMA foreign_keys = OFF")); } catch (final Exception exception) { Logger.debug(exception); }
                                    blockIds = blockHeaderDatabaseManager.insertBlockHeaders(batchedHeaders);
                                }
                                finally {
                                    try { databaseConnection.executeSql(new Query("PRAGMA foreign_keys = ON")); } catch (final Exception exception) { Logger.debug(exception); }
                                }

                                TransactionUtil.commitTransaction(databaseConnection);

                                batchedHeaders.clear();

                                if (blockIds == null) {
                                    Logger.warn("No block headers stored from batch of size: " + batchSize);
                                    break;
                                }
                                currentBlockHeight += blockIds.getCount();
                            }
                        }

                        _currentBlockHeight = currentBlockHeight;

                        final long now = _systemTime.getCurrentTimeInMilliSeconds();
                        if ((now - lastUiUpdateTime) > 500L) {
                            synchronized (blockHeightContainer) {
                                blockHeightContainer.value = _currentBlockHeight;
                                blockHeightContainer.notifyAll();
                            }
                            lastUiUpdateTime = _systemTime.getCurrentTimeInMilliSeconds();
                        }
                    }

                    if (! batchedHeaders.isEmpty()) {
                        synchronized (BlockHeaderDatabaseManager.MUTEX) {
                            TransactionUtil.startTransaction(databaseConnection);
                            final List<BlockId> blockIds = blockHeaderDatabaseManager.insertBlockHeaders(batchedHeaders);
                            TransactionUtil.commitTransaction(databaseConnection);

                            batchedHeaders.clear();

                            if (blockIds != null) {
                                _currentBlockHeight += blockIds.getCount();
                            }
                        }
                    }

                    if ( (Thread.interrupted()) || (_abortInit) ) { return; } // Intentionally always clear the interrupted flag...
                }
                finally {
                    Logger.info("Bootstrapping ended with block " + _currentBlockHeight + "...");
                    synchronized (blockHeightContainer) {
                        blockHeightContainer.value = null;
                        blockHeightContainer.notifyAll();
                    }
                }

                _setStatus(Status.INDEXING);
            }
        }
        catch (final Exception exception) {
            Logger.error(exception);
            _shutdown();
            return;
        }

        _setStatus(Status.STARTING);

        _spvModule = new SpvModule(_environment, seedNodes, 8, _wallet);
        _spvModule.setOnStatusUpdatedCallback(_onStatusUpdatedCallback);
        _spvModule.setShouldOnlyConnectToSeedNodes(Util.coalesce(_initData.shouldOnlyConnectToSeedNodes, false));

        @WeakOuter final SpvModule.NewTransactionCallback newTransactionCallback = transaction -> {
            Logger.debug("Received new transaction: " + transaction.getHash().toString());

            final NewTransactionCallback newTransactionCallback1 = _newTransactionCallback;
            if (newTransactionCallback1 != null) {
                @WeakOuter final Runnable runnable = () -> newTransactionCallback1.onNewTransaction(transaction);
                _mainThreadPool.execute(runnable);
            }

            for (final Runnable walletUpdatedCallback : _walletUpdatedCallbacks) {
                if (walletUpdatedCallback != null) {
                    _mainThreadPool.execute(walletUpdatedCallback);
                }
            }
        };
        _spvModule.setNewTransactionCallback(newTransactionCallback);

        @WeakOuter final SpvModule.TransactionValidityChangedCallback transactionValidityChangedCallback = (transactionHash, validity) -> {
            Logger.debug("Transaction " + transactionHash.toString() + " validity changed to: " + validity.toString());

            final TransactionValidityChangedCallback transactionValidityChangedCallback1 = _transactionValidityChangedCallback;
            if (transactionValidityChangedCallback1 != null) {
                @WeakOuter final Runnable runnable = () -> transactionValidityChangedCallback1.onTransactionValidityChanged(transactionHash, validity);
                _mainThreadPool.execute(runnable);
            }

            for (final Runnable walletUpdatedCallback : _walletUpdatedCallbacks) {
                if (walletUpdatedCallback != null) {
                    _mainThreadPool.execute(walletUpdatedCallback);
                }
            }
        };
        _spvModule.setTransactionValidityChangedCallback(transactionValidityChangedCallback);

        _spvModule.initialize();

        { // Set the SpvModule's NodeManager's BloomFilter...
            final BitcoinNodeManager bitcoinNodeManager = _spvModule.getBitcoinNodeManager();
            Logger.debug("Initializing bloom filter");
            final MutableBloomFilter bloomFilter = _wallet.generateBloomFilter();
            if (Logger.isTraceEnabled()) {
                Logger.trace("Bloom filter: " + bloomFilter.getBytes().toString());
            }
            bitcoinNodeManager.setBloomFilter(bloomFilter);
        }

        try (final DatabaseConnection databaseConnection = database.newConnection()) {
            final SpvDatabaseManager databaseManager = new SpvDatabaseManager(databaseConnection, database.getMaxQueryBatchSize(), _checkpointConfiguration);
            final BlockHeaderDatabaseManager blockHeaderDatabaseManager = databaseManager.getBlockHeaderDatabaseManager();

            final BlockId headBlockId = blockHeaderDatabaseManager.getHeadBlockHeaderId();
            if (headBlockId != null) {
                _currentBlockHeight = blockHeaderDatabaseManager.getBlockHeight(headBlockId);
            }
            else {
                _currentBlockHeight = 0L;
            }
        }
        catch (final DatabaseException exception) {
            Logger.error("Error loading initial BlockHeight.", exception);
        }

        final BitcoinNodeManager bitcoinNodeManager = _spvModule.getBitcoinNodeManager();
        bitcoinNodeManager.setNodeListChangedCallback(_onConnectedNodesChanged);

        final AtomicBoolean addressBlocksLoaded = new AtomicBoolean(false);
        @WeakOuter final Runnable newBlockHeaderAvailableCallbackRunnable = () -> {
            final Long blockHeight = _spvModule.getBlockHeight();
            _currentBlockHeight = blockHeight;

            for (final NewBlockHeaderCallback callback : _newBlockHeaderCallbacks) {
                callback.newBlockHeight(blockHeight);
            }

            if (_isSynchronizationComplete()) {
                if (! addressBlocksLoaded.getAndSet(true)) {
                    _initMinMerkleBlockHeight();
                    _synchronizeMerkleBlocks();
                }

                final Runnable onSynchronizationComplete = _onSynchronizationComplete;
                _onSynchronizationComplete = null;
                if (onSynchronizationComplete != null) {
                    onSynchronizationComplete.run();
                }
            }
        };

        _spvModule.setNewBlockHeaderAvailableCallback(newBlockHeaderAvailableCallbackRunnable);

        final ConcurrentLinkedQueue<MerkleBlockSyncUpdateCallback> merkleBlockSyncUpdateCallbacks = _merkleBlockSyncUpdateCallbacks;
        @WeakOuter final SpvModule.MerkleBlockSyncUpdateCallback merkleBlockSyncUpdateCallback = (currentBlockHeight, isSynchronizing) -> {
            for (final MerkleBlockSyncUpdateCallback callback : merkleBlockSyncUpdateCallbacks) {
                callback.onMerkleBlockHeightUpdated(currentBlockHeight, isSynchronizing);
            }
        };
        _spvModule.setMerkleBlockSyncUpdateCallback(merkleBlockSyncUpdateCallback);

        final WeakReference<BitcoinVerde> bitcoinVerdeWeakReference = new WeakReference<>(this);
        @WeakOuter final Runnable spvThreadRunnable = () -> {
            try {
                final BitcoinVerde bitcoinVerde = bitcoinVerdeWeakReference.get();
                if (bitcoinVerde != null) {
                    bitcoinVerde._setStatus(Status.ONLINE);
                    bitcoinVerde._spvModule.loop();
                    bitcoinVerde._setStatus(Status.OFFLINE);
                }
            }
            catch (final Exception exception) {
                Logger.error(exception);
            }
        };

        _spvThread = new Thread(spvThreadRunnable);
        _spvThread.setName("SPV Thread");
        _spvThread.start();

        @WeakOuter final Runnable pingThreadRunnable = () -> {
            final BitcoinNodeManager bitcoinNodeManager1 = _spvModule.getBitcoinNodeManager();
            while (true) {
                for (final BitcoinNode bitcoinNode : bitcoinNodeManager1.getNodes()) {
                    bitcoinNode.ping(null);
                }

                try { Thread.sleep(5000L); } catch (final Exception exception) { break; }

                final Runnable onPeerListChanged = _onConnectedNodesChanged;
                if (onPeerListChanged != null) {
                    _mainThreadPool.execute(onPeerListChanged);
                }

                try { Thread.sleep(5000L); } catch (final Exception exception) { break; }
            }
        };

        _pingThread = new Thread(pingThreadRunnable);
        _pingThread.setName("Ping Thread");
        _pingThread.start();

        _isInitialized = true;
        synchronized (_initPin) {
            _initPin.notifyAll();
        }

        final Runnable onInitCompleteCallback = _onInitComplete;
        if (onInitCompleteCallback != null) {
            _mainThreadPool.execute(onInitCompleteCallback);
        }

        if (_isSynchronizationComplete()) {
            _initMinMerkleBlockHeight();
            _synchronizeMerkleBlocks();

            final Runnable onSynchronizationComplete = _onSynchronizationComplete;
            _onSynchronizationComplete = null;
            if (onSynchronizationComplete != null) {
                onSynchronizationComplete.run();
            }
        }
    }

    private boolean _shouldContinue(Thread currentThread) {
        return (! _abortInit) && (! currentThread.isInterrupted());
    }

    /**
     * Returns the BlockHeader for the given BlockHeight.
     *  Requires _isInitialized.
     */
    protected BlockHeader _getBlockHeader(final Long blockHeight) {
        if (!_isInitialized) { return null; }

        final Database database = _environment.getDatabase();
        try (final DatabaseConnection databaseConnection = database.newConnection()) {
            final SpvDatabaseManager databaseManager = new SpvDatabaseManager(databaseConnection, database.getMaxQueryBatchSize(), _checkpointConfiguration);
            final BlockchainDatabaseManager blockchainDatabaseManager = databaseManager.getBlockchainDatabaseManager();
            final BlockHeaderDatabaseManager blockHeaderDatabaseManager = databaseManager.getBlockHeaderDatabaseManager();

            final BlockchainSegmentId blockchainSegmentId = blockchainDatabaseManager.getHeadBlockchainSegmentId();
            final BlockId blockId = blockHeaderDatabaseManager.getBlockIdAtHeight(blockchainSegmentId, blockHeight);
            return blockHeaderDatabaseManager.getBlockHeader(blockId);
        }
        catch (final Exception exception) {
            Logger.error("Error loading BlockHeader.", exception);
            return null;
        }
    }

    protected Float _getSynchronizationPercent() {
        if (! _isInitialized) {
            // Display the bootstrapping progress of blocks...
            return (_currentBlockHeight.floatValue() / BOOTSTRAP_BLOCK_COUNT);
        }

        final BlockHeader blockHeader = _getBlockHeader(_currentBlockHeight);
        if (blockHeader == null) { return 0F; }

        final Long blockTimestamp = blockHeader.getTimestamp();
        final Long currentTimestamp = _systemTime.getCurrentTimeInSeconds();

        return ((blockTimestamp - MedianBlockTime.GENESIS_BLOCK_TIMESTAMP) / ((float) (currentTimestamp - MedianBlockTime.GENESIS_BLOCK_TIMESTAMP)));
    }

    protected Boolean _isSynchronizationComplete() {
        // b: blockTimestamp, n: currentTimestamp, G: genesisTimestamp; where blockTimestamp is the minimum timestamp to be considered synchronized...
        // b = (n - G) * 0.99999 + G
        if (! _isInitialized) { return false; } // Exclude bootstrap percentage...
        return (_getSynchronizationPercent() >= 0.99998F); // 0.9999 equates to about 9 hours in the past, as of 2019-05.  0.99999 equates to 1 hour.
    }

    protected void _initSeedPhraseGenerator() {
        if (_seedPhraseGenerator != null) { return; }

        _seedPhraseGenerator = new SeedPhraseGenerator(_initData.seedPhraseWords);
    }

    public Address _createCompressedReceivingAddress() {
        final PrivateKey privateKey = _secureKeyStore.createPrivateKey();
        _wallet.addPrivateKey(privateKey);

        _updateBloomFilter();

        final AddressInflater addressInflater = new AddressInflater();

        for (final Runnable walletUpdatedCallbacks : _walletUpdatedCallbacks) {
            walletUpdatedCallbacks.run();
        }

        return addressInflater.fromPrivateKey(privateKey, true);
    }

    protected void _updateBloomFilter() {
        @WeakOuter final Runnable runnable = () -> {
            final BitcoinNodeManager bitcoinNodeManager = _spvModule.getBitcoinNodeManager();
            Logger.debug("Updating bloom filter");
            final MutableBloomFilter bloomFilter = _wallet.generateBloomFilter();
            if (Logger.isTraceEnabled()) {
                Logger.trace("Bloom filter: " + bloomFilter.getBytes().toString());
            }
            bitcoinNodeManager.setBloomFilter(bloomFilter);
        };

        _mainThreadPool.execute(runnable);
    }

    protected BitcoinVerde(final InitData initData) {
        _initData = initData;
        _mainThreadPool = new CachedThreadPool(8, 30000L);
        _mainThreadPool.start();

        _wallet.setSatoshisPerByteFee(1D);

        @WeakOuter final Runnable initThreadRunnable = () -> {
            try {
                _initialize();
            }
            catch (final Exception exception) {
                Logger.error(exception);
                _mainThreadPool.execute(() -> {
                    BitcoinVerde.this.shutdown();
                });
            }
            finally {
                try {
                    final InputStream inputStream = _initData.bootstrapHeaders;
                    if (inputStream != null) {
                        inputStream.close();
                    }
                }
                catch (final Exception exception) {
                    Logger.error(exception);
                }

                //_initData = null; invalidates the database reference used in shutdown
                _initThread = null;
            }
        };

        _initThread = new Thread(initThreadRunnable);
        _initThread.setName("Init Thread");

        @WeakOuter final Thread.UncaughtExceptionHandler uncaughtExceptionHandler = (thread, exception) -> Logger.error("Uncaught exception in thread " + thread.getName() + "(" + thread.getId() + ")", exception);
        _initThread.setUncaughtExceptionHandler(uncaughtExceptionHandler);
        _initThread.start();
    }

    public void setOnInitCompleteCallback(final Runnable callback) {
        _onInitComplete = callback;

        if (isInit()) {
            final Runnable onInitCompleteCallback = _onInitComplete;
            if (onInitCompleteCallback != null) {
                _mainThreadPool.execute(onInitCompleteCallback);
            }
        }
    }

    public void setOnSynchronizationComplete(final Runnable callback) {
        _onSynchronizationComplete = callback;

        if (_isSynchronizationComplete()) {
            final Runnable onSynchronizationComplete = _onSynchronizationComplete;
            _onSynchronizationComplete = null;
            if (onSynchronizationComplete != null) {
                _mainThreadPool.execute(onSynchronizationComplete);
            }
        }
    }

    public void addNewBlockHeaderCallback(final NewBlockHeaderCallback callback) {
        _newBlockHeaderCallbacks.add(callback);
    }

    public void removeNewBlockHeaderCallback(final NewBlockHeaderCallback callback) {
        _newBlockHeaderCallbacks.remove(callback);
    }

    public void addNewMerkleBlockSyncUpdateCallback(final MerkleBlockSyncUpdateCallback callback) {
        _merkleBlockSyncUpdateCallbacks.add(callback);
    }

    public void removeMerkleBlockSyncUpdateCallback(final MerkleBlockSyncUpdateCallback callback) {
        _merkleBlockSyncUpdateCallbacks.remove(callback);
    }

    public BlockHeader getBlockHeader(final Long blockHeight) {
        _waitForInitialization();

        return _getBlockHeader(blockHeight);
    }

    public void setOnStatusUpdatedCallback(final Runnable callback) {
        _onStatusUpdatedCallback = callback;
        if (_spvModule != null) {
            _spvModule.setOnStatusUpdatedCallback(callback);
        }
    }

    public Status getStatus() {
        return _status;
    }

    public SpvModule.Status getSpvStatus() {
        return (_spvModule != null ? _spvModule.getStatus() : SpvModule.Status.OFFLINE);
    }

    public Long getBootstrapHeight() {
        return BOOTSTRAP_BLOCK_COUNT;
    }

    public Boolean isInit() {
        return _isInitialized;
    }

    public Long getBlockHeight() {
        _waitForInitialization();

        final Database database = _environment.getDatabase();
        try (final DatabaseConnection databaseConnection = database.newConnection()) {
            final SpvDatabaseManager databaseManager = new SpvDatabaseManager(databaseConnection, database.getMaxQueryBatchSize(), _checkpointConfiguration);
            final BlockHeaderDatabaseManager blockHeaderDatabaseManager = databaseManager.getBlockHeaderDatabaseManager();

            final BlockId headBlockHeaderId = blockHeaderDatabaseManager.getHeadBlockHeaderId();
            return blockHeaderDatabaseManager.getBlockHeight(headBlockHeaderId);
        }
        catch (final Exception exception) {
            Logger.error("Error loading head Block height.", exception);
            return null;
        }
    }

    public Float getSynchronizationPercent() {
        return _getSynchronizationPercent();
    }

    public Boolean isSynchronizationComplete() {
        return _isSynchronizationComplete();
    }

    public void setOnConnectedNodesChanged(final Runnable onConnectedNodesChanged) {
        _onConnectedNodesChanged = onConnectedNodesChanged;

        if ( (_spvModule != null) && (_spvModule.isInitialized()) ) {
            final BitcoinNodeManager bitcoinNodeManager = _spvModule.getBitcoinNodeManager();
            bitcoinNodeManager.setNodeListChangedCallback(onConnectedNodesChanged);
        }
    }

    public List<Node> getConnectedNodes() {
        _waitForInitialization();

        final MutableList<Node> peers = new MutableList<Node>();
        if (_spvModule == null) { return peers; }

        final BitcoinNodeManager bitcoinNodeManager = _spvModule.getBitcoinNodeManager();
        for (final BitcoinNode bitcoinNode : bitcoinNodeManager.getNodes()) {
            final Node node = new Node(bitcoinNode.getId());
            node._ip = bitcoinNode.getIp().toString();
            node._port = bitcoinNode.getPort();
            node._userAgent = bitcoinNode.getUserAgent();
            node._ping = bitcoinNode.getAveragePing();
            peers.add(node);
        }

        return peers;
    }

    public Wallet getWallet() {
        return _wallet;
    }

    public PrivateKey getPrivateKey(final PublicKey publicKey) {
        _waitForInitialization();

        return _secureKeyStore.getPrivateKey(publicKey);
    }

    public String getSeedPhrase(final PrivateKey privateKey) {
        _waitForInitialization();
        _initSeedPhraseGenerator();

        if (_seedPhraseGenerator == null) { return null; }
        return _seedPhraseGenerator.toSeedPhrase(privateKey);
    }

    public SeedPhraseGenerator getSeedPhraseGenerator() {
        _waitForInitialization();
        _initSeedPhraseGenerator();

        return _seedPhraseGenerator;
    }

    public List<String> getSeedWords() {
        _waitForInitialization();
        _initSeedPhraseGenerator();

        return _seedPhraseGenerator.getSeedWords();
    }

    public void addWalletUpdatedCallback(final Runnable walletUpdatedCallback) {
        _walletUpdatedCallbacks.add(walletUpdatedCallback);
    }

    public void removeWalletUpdatedCallback(final Runnable walletUpdatedCallback) {
        _walletUpdatedCallbacks.remove(walletUpdatedCallback);
    }

    public void setNewTransactionCallback(final NewTransactionCallback newTransactionCallback) {
        _newTransactionCallback = newTransactionCallback;
    }

    public void setTransactionValidityChangedCallback(final TransactionValidityChangedCallback transactionValidityChangedCallback) {
        _transactionValidityChangedCallback = transactionValidityChangedCallback;
    }

    public Long getConfirmationCount(final TransactionOutputIdentifier transactionOutputIdentifier) {
        _waitForInitialization();

        final Database database = _environment.getDatabase();
        try (final DatabaseConnection databaseConnection = database.newConnection()) {
            final SpvDatabaseManager databaseManager = new SpvDatabaseManager(databaseConnection, database.getMaxQueryBatchSize(), _checkpointConfiguration);
            final BlockchainDatabaseManager blockchainDatabaseManager = databaseManager.getBlockchainDatabaseManager();
            final BlockHeaderDatabaseManager blockHeaderDatabaseManager = databaseManager.getBlockHeaderDatabaseManager();
            final TransactionDatabaseManager transactionDatabaseManager = databaseManager.getTransactionDatabaseManager();

            final TransactionId transactionId = transactionDatabaseManager.getTransactionId(transactionOutputIdentifier.getTransactionHash());
            if (transactionId == null) { return 0L; }

            final BlockchainSegmentId blockchainSegmentId = blockchainDatabaseManager.getHeadBlockchainSegmentId();

            final BlockId blockId = transactionDatabaseManager.getBlockId(blockchainSegmentId, transactionId);
            if (blockId == null) { return 0L; }

            final BlockId headBlockId = blockHeaderDatabaseManager.getHeadBlockHeaderId();

            final Long headBlockHeight = blockHeaderDatabaseManager.getBlockHeight(headBlockId);
            final Long blockHeight = blockHeaderDatabaseManager.getBlockHeight(blockId);

            return ((headBlockHeight - blockHeight) + 1);
        }
        catch (final Exception exception) {
            Logger.error("Error calculating confirmation count.", exception);
            return null;
        }
    }

    public boolean broadcastTransaction(final Transaction transaction) {
        _waitForInitialization();

        Logger.debug("Broadcasting transaction: " + transaction.getHash().toString());

        try {
            _spvModule.storeTransaction(transaction);
        }
        catch (final Exception exception) {
            Logger.error("Unable to store transaction", exception);
            return false;
        }

        @WeakOuter final Runnable runnable = () -> _spvModule.broadcastTransaction(transaction);
        (new Thread(runnable)).start();

        for (final Runnable walletUpdatedCallback : _walletUpdatedCallbacks) {
            if (walletUpdatedCallback != null) {
                _mainThreadPool.execute(walletUpdatedCallback);
            }
        }

        return true;
    }

    public void disconnectNode(final NodeId nodeId) {
        _waitForInitialization();

        final BitcoinNodeManager bitcoinNodeManager = _spvModule.getBitcoinNodeManager();
        final BitcoinNode bitcoinNode = bitcoinNodeManager.getNode(nodeId);
        if (bitcoinNode != null) {
            bitcoinNode.disconnect();
        }
    }

    public void banNode(final Ip nodeIp) {
        _waitForInitialization();

        final BitcoinNodeManager bitcoinNodeManager = _spvModule.getBitcoinNodeManager();
        bitcoinNodeManager.banNode(nodeIp);
    }

    public void setIsConnected(final Boolean isConnected) {
        _waitForInitialization();

//        if ( (isConnected) && (! _isConnected) ) {
//            _spvModule.connectToSeedNodes();
//        }

        _isConnected = isConnected;
    }

    public void synchronizeMerkleBlocks() {
        _waitForInitialization();
        _synchronizeMerkleBlocks();
    }

    public void setSlpTokenId(final SlpTokenId slpTokenId) {
        final SlpTokenId originalSlpTokenId = _slpTokenId;
        _slpTokenId = slpTokenId;

        if (! Util.areEqual(originalSlpTokenId, slpTokenId)) {
            for (final Runnable callback : _slpTokenChangedCallbacks) {
                if (callback != null) {
                    callback.run();
                }
            }
        }
    }

    public SlpTokenId getSlpTokenId() {
        return _slpTokenId;
    }

    public void addSlpTokenChangedCallback(final Runnable callback) {
        _slpTokenChangedCallbacks.add(callback);
    }

    public void removeSlpTokenChangedCallback(final Runnable callback) {
        _slpTokenChangedCallbacks.remove(callback);
    }

    public void pingNodes() {
        _waitForInitialization();

        @WeakOuter final Runnable runnable = () -> {
            final BitcoinNodeManager bitcoinNodeManager = _spvModule.getBitcoinNodeManager();
            for (final BitcoinNode bitcoinNode : bitcoinNodeManager.getNodes()) {
                bitcoinNode.ping(null);
            }
        };
        _mainThreadPool.execute(runnable);
    }

    /**
     * Adds the PrivateKey to the Wallet and KeyStore.
     *  Updates the bloom filter for all peers.
     */
    public void addPrivateKey(final PrivateKey privateKey) {
        _secureKeyStore.storePrivateKey(privateKey);
        _wallet.addPrivateKey(privateKey);

        _updateBloomFilter();

        for (final Runnable walletUpdatedCallbacks : _walletUpdatedCallbacks) {
            walletUpdatedCallbacks.run();
        }
    }

    /**
     * Creates a new PrivateKey and adds it to the KeyStore.
     *  Updates the bloom filter for all peers.
     */
    public Address createReceivingAddress() {
        return _createCompressedReceivingAddress();
    }

    public Address getChangeAddress() {
        final String changeAddressString = _keyValueStore.getString(KeyValueStoreKeys.CHANGE_ADDRESS);

        if (Util.isBlank(changeAddressString)) {
            final Address address = _createCompressedReceivingAddress();
            _keyValueStore.putString(KeyValueStoreKeys.CHANGE_ADDRESS, address.toBase58CheckEncoded());
            _keyValueStore.putString(KeyValueStoreKeys.CHANGE_ADDRESS_IS_COMPRESSED, "1");
            return address;
        }

        final AddressInflater addressInflater = new AddressInflater();
        if (Util.parseBool(_keyValueStore.getString(KeyValueStoreKeys.CHANGE_ADDRESS_IS_COMPRESSED))) {
            return addressInflater.fromBase58Check(changeAddressString, true);
        }
        else {
            return addressInflater.fromBase58Check(changeAddressString, false);
        }
    }

    public void setChangeAddress(final Address address) {
        _keyValueStore.putString(KeyValueStoreKeys.CHANGE_ADDRESS, address.toBase58CheckEncoded());
        _keyValueStore.putString(KeyValueStoreKeys.CHANGE_ADDRESS_IS_COMPRESSED, "0");
    }

    /**
     * Clear user transactions data from the database.
     */
    public Boolean clearTransactionsDatabaseTables() {
        final Database database = _environment.getDatabase();
        try (final DatabaseConnection databaseConnection = database.newConnection()) {
            TransactionUtil.startTransaction(databaseConnection);
            _clearBlockTransactionsTable(databaseConnection);
            _clearTransactionDataTable(databaseConnection);
            _clearTransactionsTable(databaseConnection);
            _clearBlockMerkleTreesTable(databaseConnection);
            TransactionUtil.commitTransaction(databaseConnection);

            return true;
        }
        catch (final Exception exception) {
            Logger.error("Unable to clear user transaction data from database.", exception);
            return false;
        }
    }

    public void shutdown() {
        _shutdown();
    }

    public Double getDollarsPerBitcoin() {
        final Double dollarsPerBitcoin = _dollarsPerBitcoin;
        if (dollarsPerBitcoin != null) { return dollarsPerBitcoin; }

        _waitForInitialization();

        final PriceIndexer priceIndexer = _priceIndexer;
        if (priceIndexer == null) { return null; }

        _dollarsPerBitcoin = priceIndexer.getDollarsPerBitcoin();
        return _dollarsPerBitcoin;
    }

    public void clearMerkleBlocksAfterTimestamp(final Long timestampInSeconds) {
        final Database database = _environment.getDatabase();
        try (final DatabaseConnection databaseConnection = database.newConnection()) {
            final SpvDatabaseManager databaseManager = new SpvDatabaseManager(databaseConnection, database.getMaxQueryBatchSize(), _checkpointConfiguration);
            final BlockHeaderDatabaseManager blockHeaderDatabaseManager = databaseManager.getBlockHeaderDatabaseManager();
            final SpvBlockDatabaseManager spvBlockDatabaseManager = databaseManager.getBlockDatabaseManager();

            final Sha256Hash headBlockHash = blockHeaderDatabaseManager.getHeadBlockHeaderHash();
            BlockId blockId = blockHeaderDatabaseManager.getBlockHeaderId(headBlockHash);
            while (true) {
                final Long blockTimestamp = blockHeaderDatabaseManager.getBlockTimestamp(blockId);
                if (timestampInSeconds >= blockTimestamp) { break; }

                spvBlockDatabaseManager.deletePartialMerkleTree(blockId);
                blockId = blockHeaderDatabaseManager.getAncestorBlockId(blockId, 1);
                if (blockId == null) { break; }
            }
        }
        catch (final DatabaseException exception) {
            Logger.debug(exception);
        }
    }

    public Boolean repairMerkleBlocks() {
        _waitForInitialization();

        { // Download AddressBlocks if connected to an indexing node...
            final List<PrivateKey> privateKeys = _secureKeyStore.getPrivateKeys();

            final BitcoinNodeManager bitcoinNodeManager = _spvModule.getBitcoinNodeManager();
            @WeakOuter final NodeFilter nodeFilter = bitcoinNode -> bitcoinNode.hasFeatureEnabled(NodeFeatures.Feature.BLOCKCHAIN_INDEX_ENABLED);
            final BitcoinNode bitcoinVerdeNode = bitcoinNodeManager.getNode(nodeFilter);
            if (bitcoinVerdeNode == null) { return false; }

            final AddressInflater addressInflater = new AddressInflater();
            final MutableList<Address> addresses = new MutableList<Address>();
            for (final PrivateKey privateKey : privateKeys) {
                final Address address = addressInflater.fromPrivateKey(privateKey, false);
                final Address compressedAddress = addressInflater.fromPrivateKey(privateKey, true);

                addresses.add(address);
                addresses.add(compressedAddress);
            }

            Logger.info("Requesting AddressBlocks from: " + bitcoinVerdeNode.getConnectionString());
            if (Logger.isDebugEnabled()) {
                for (final Address address : addresses) {
                    Logger.debug(address.toBase58CheckEncoded());
                }
            }
            @WeakOuter final Runnable runnable = () -> bitcoinVerdeNode.getAddressBlocks(addresses, new BitcoinNode.BlockInventoryAnnouncementHandler() {
                @Override
                public void onNewInventory(BitcoinNode bitcoinNode, List<Sha256Hash> blockHashes) {
                    final Database database = _environment.getDatabase();
                    try (final DatabaseConnection databaseConnection = database.newConnection()) {
                        final SpvDatabaseManager databaseManager = new SpvDatabaseManager(databaseConnection, database.getMaxQueryBatchSize(), _checkpointConfiguration);
                        final BlockHeaderDatabaseManager blockHeaderDatabaseManager = databaseManager.getBlockHeaderDatabaseManager();
                        final SpvBlockDatabaseManager spvBlockDatabaseManager = databaseManager.getBlockDatabaseManager();

                        TransactionUtil.startTransaction(databaseConnection);
                        for (final Sha256Hash blockHash : blockHashes) {
                            final BlockId blockId = blockHeaderDatabaseManager.getBlockHeaderId(blockHash);
                            Logger.debug("Deleting Partial Merkle Tree: " + blockHash);
                            spvBlockDatabaseManager.deletePartialMerkleTree(blockId);
                        }
                        TransactionUtil.commitTransaction(databaseConnection);
                    }
                    catch (final DatabaseException exception) {
                        Logger.debug(exception);
                    }
                }

                @Override
                public void onNewHeaders(BitcoinNode bitcoinNode, List<BlockHeader> blockHeaders) {
                    final MutableList<Sha256Hash> blockHashes = new MutableList<>();
                    for (final BlockHeader blockHeader : blockHeaders) {
                        blockHashes.add(blockHeader.getHash());
                    }
                    this.onNewInventory(bitcoinNode, blockHashes);
                }
            });

            (new Thread(runnable)).start();

            return true;
        }
    }

    public void repairSlpValidity() {
        _waitForInitialization();

        final Database database = _environment.getDatabase();
        try (final DatabaseConnection databaseConnection = database.newConnection()) {
            final SpvDatabaseManager databaseManager = new SpvDatabaseManager(databaseConnection, database.getMaxQueryBatchSize(), _checkpointConfiguration);
            final SpvTransactionDatabaseManager transactionDatabaseManager = databaseManager.getTransactionDatabaseManager();

            TransactionUtil.startTransaction(databaseConnection);
            transactionDatabaseManager.clearSlpValidity();
            TransactionUtil.commitTransaction(databaseConnection);

            _wallet.clearSlpValidity();

            @WeakOuter final Runnable runnable = () -> _spvModule.synchronizeSlpValidity();
            (new Thread(runnable)).start();
        }
        catch (final DatabaseException exception) {
            Logger.debug(exception);
        }
    }
}
