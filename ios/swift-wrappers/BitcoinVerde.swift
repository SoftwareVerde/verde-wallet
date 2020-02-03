
public class BitcoinVerde {
    public class InitData {
        private var _core : ComSoftwareverdeBitcoinAppLibBitcoinVerde_InitData

        init() {
            _core = new_ComSoftwareverdeBitcoinAppLibBitcoinVerde_InitData_init()
        }

        public func setDatabase(database: Database) {
            _core.setValue(database, forKey: "database_")
        }

        public func setBootstrapHeaders(bootstrapHeaders: Data) {
            let byteArray = IOSByteArray(nsData: bootstrapHeaders)
            let inputStream = JavaIoByteArrayInputStream(byteArray: byteArray)
            _core.setValue(inputStream, forKey: "bootstrapHeaders_")
        }

        public func setPostBootstrapInitSql(sql: String) {
            _core.setValue(sql, forKey: "postBootstrapInitSql_")
        }
        
        public func setKeyStore(keyStore: IOSKeyStore) {
            _core.setValue(keyStore, forKey: "keyStore_")
        }

        public func setShouldOnlyConnectToSeedNodes(shouldOnlyConnectToSeedNodes: Bool) {
            _core.setValue(shouldOnlyConnectToSeedNodes, forKey: "shouldOnlyConnectToSeedNodes_")
        }

        public func setSeedNodes(seedNodePropertiesList: [SeedNodeProperties]) {
            let listSize = UInt(seedNodePropertiesList.count)
            let array : IOSObjectArray = IOSObjectArray(length: listSize, type: ComSoftwareverdeBitcoinServerConfigurationSeedNodeProperties_class_())
            for i in 0..<listSize {
                array.replaceObject(at: i, with: seedNodePropertiesList[Int(i)].getCore())
            }
            _core.setValue(array, forKey: "seedNodes_");
        }

        public func getCore() -> ComSoftwareverdeBitcoinAppLibBitcoinVerde_InitData {
            return _core
        }
    }

    private var _core : ComSoftwareverdeBitcoinAppLibBitcoinVerde

    
    public static func init__(initData: InitData) {
        ComSoftwareverdeBitcoinAppLibBitcoinVerde.init__(with: initData.getCore())
    }
    
    init(core: ComSoftwareverdeBitcoinAppLibBitcoinVerde) {
        _core = core
    }
    
    public static func getInstance() -> BitcoinVerde? {
        if let core : ComSoftwareverdeBitcoinAppLibBitcoinVerde = ComSoftwareverdeBitcoinAppLibBitcoinVerde.getInstance() {
            return BitcoinVerde(core: core)
        }
        
        return nil
    }

    public static func runSqlInitFile(databaseConnection: ComSoftwareverdeDatabaseDatabaseConnection, queries: String) {
        ComSoftwareverdeBitcoinAppLibBitcoinVerde_runSqlInitFileWithComSoftwareverdeDatabaseDatabaseConnection_withNSString_(databaseConnection, queries)
    }

    public func getBlockHeight() -> Int64 {
        return _core.getBlockHeight()?.longLongValue() ?? 0
    }

    public func getSynchronizationPercent() -> Float {
        if isSynchronizationComplete() {
            return 1.0
        }

        return _core.getSynchronizationPercent()?.floatValue() ?? 0
    }

    public func isSynchronizationComplete() -> Bool {
        return _core.isSynchronizationComplete()?.booleanValue() ?? false
    }
    
    public func synchronizeMerkleBlocks() {
        _core.synchronizeMerkleBlocks()
    }

    public func isInitialized() -> Bool {
        return _core.isInit()?.booleanValue() ?? false
    }

    public func getStatus() -> ComSoftwareverdeBitcoinAppLibBitcoinVerde_Status {
        return _core.getStatus()
    }

    public func shutdown() {
        _core.shutdown()
    }
    
    public func repairMerkleBlocks(repairFinishedCallback: @escaping () -> ()) -> Bool {
        defer {
            repairFinishedCallback()
        }
        
        return _core.repairMerkleBlocks()?.booleanValue() ?? false
    }
    
    public func getWallet() -> Wallet {
        return Wallet(core: _core.getWallet())
    }
    
    public func broadcastTransaction(transaction: Transaction) {
        _core.broadcastTransaction(with: transaction.getCore())
    }
    
    public func clearTransactionsDatabaseTables() -> Bool {
        return _core.clearTransactionsDatabaseTables()?.booleanValue() ?? false
    }
    
    public func addNewMerkleBlockSyncUpdateCallback(callback: MerkleBlockSyncUpdateCallback) {
        _core.addNewMerkleBlockSyncUpdateCallback(with: callback)
    }
    
    public func removeMerkleBlockSyncUpdateCallback(callback: MerkleBlockSyncUpdateCallback) {
        _core.removeMerkleBlockSyncUpdateCallback(with: callback)
    }
    
    public func addWalletUpdatedCallback(walletUpdatedCallback: Runnable) {
        _core.addWalletUpdatedCallback(with: walletUpdatedCallback)
    }
    
    public func removeWalletUpdatedCallback(walletUpdatedCallback: Runnable) {
        _core.removeWalletUpdatedCallback(with: walletUpdatedCallback)
    }
    
    public func setOnInitCompleteCallback(callback: Runnable) {
        _core.setOnInitCompleteCallbackWith(callback)
    }
    
    public func setOnSynchronizationCompleteCallback(callback: Runnable) {
        _core.setOnSynchronizationCompleteWith(callback)
    }
    
    public func setOnConnectedNodesChanged(onConnectedNodesChanged: Runnable) {
        _core.setOnConnectedNodesChangedWith(onConnectedNodesChanged)
    }
    
    public func setNewTransactionCallback(newTransactionCallback: NewTransactionCallback) {
        _core.setNewTransactionCallbackWith(newTransactionCallback)
    }
    
    public func setOnStatusUpdatedCallback(callback: Runnable) {
        _core.setOnStatusUpdatedCallbackWith(callback)
    }
    
    public func getConfirmationCount(transactionOutputIdentifier: TransactionOutputIdentifier) -> Int64 {
        return Int64(_core.getConfirmationCount(with: transactionOutputIdentifier.getCore()))
    }
    
    public func addSlpTokenChangedCallback(callback: Runnable) {
        _core.addSlpTokenChangedCallback(with: callback)
    }
    
    public func removeSlpTokenChangedCallback(callback: Runnable) {
        _core.removeSlpTokenChangedCallback(with: callback)
    }
}

public class MerkleBlockSyncUpdateCallback : NSObject, ComSoftwareverdeBitcoinAppLibMerkleBlockSyncUpdateCallback {
    fileprivate let _callback : (JavaLangLong, JavaLangBoolean) -> ()
    
    public init(callback: @escaping (JavaLangLong, JavaLangBoolean) -> ()) {
        _callback = callback
        
        super.init()
    }
    
    public func onMerkleBlockHeightUpdated(with currentBlockHeight: JavaLangLong!, with isSynchronizing: JavaLangBoolean!) {
        _callback(currentBlockHeight, isSynchronizing)
    }
}

public class NewTransactionCallback : NSObject, ComSoftwareverdeBitcoinAppLibBitcoinVerde_NewTransactionCallback {
    fileprivate let _callback : (ComSoftwareverdeBitcoinTransactionTransactionProtocol) -> ()
    
    public init(callback: @escaping (ComSoftwareverdeBitcoinTransactionTransactionProtocol) -> ()) {
        _callback = callback
        
        super.init()
    }
    
    public func onNewTransaction(with transaction: ComSoftwareverdeBitcoinTransactionTransactionProtocol!) {
        _callback(transaction)
    }
}
