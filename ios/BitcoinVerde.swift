
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

        public func getCore() -> ComSoftwareverdeBitcoinAppLibBitcoinVerde_InitData {
            return _core
        }
    }

    private var _core : ComSoftwareverdeBitcoinAppLibBitcoinVerde

    init(initData: InitData) {
        _core = ComSoftwareverdeBitcoinAppLibBitcoinVerde(comSoftwareverdeBitcoinAppLibBitcoinVerde_InitData: initData.getCore());
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

    public func isInitialized() -> Bool {
        return _core.isInit()?.booleanValue() ?? false
    }

    public func getStatus() -> ComSoftwareverdeBitcoinAppLibBitcoinVerde_Status {
        return _core.getStatus()
    }

    public func shutdown() {
        _core.shutdown()
    }
}
