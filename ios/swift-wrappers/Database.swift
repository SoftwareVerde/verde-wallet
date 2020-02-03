import SQLite3

class WeakReference<T: AnyObject> {
    weak var value : T?

    init(_ object: T?) {
        self.value = object
    }
}

public class Database : ComSoftwareverdeDatabaseBitcoinPlaceholderBitcoinDatabase {
    private var _fileUrl : URL
    private var _databaseConnections : [WeakReference<DatabaseConnection>] = []
    private var _databaseConnectionAppenderQueue : DispatchQueue

    public override init() {
        _fileUrl = try! FileManager.default.url(for: .documentDirectory, in: .userDomainMask, appropriateFor: nil, create: false)
            .appendingPathComponent("bitcoin.sqlite")

        _databaseConnectionAppenderQueue = DispatchQueue(label: "database_connection_appender_queue")
    }

    public override func newConnection() -> ComSoftwareverdeBitcoinServerDatabaseDatabaseConnection! {
        var sqliteDatabase : OpaquePointer?

        if sqlite3_open(_fileUrl.path, &sqliteDatabase) != SQLITE_OK {
            print("Error opening database " + _fileUrl.path)
            return nil
        }

        let databaseConnection : DatabaseConnection = DatabaseConnection(database: sqliteDatabase!)
        weak var weakDatabaseConnection : DatabaseConnection? = databaseConnection

        _databaseConnectionAppenderQueue.sync {
            _databaseConnections.append(WeakReference(weakDatabaseConnection))
        }

        return ComSoftwareverdeBitcoinServerDatabaseDatabaseConnectionCore(comSoftwareverdeDatabaseDatabaseConnection: databaseConnection)
    }

    public override func newConnectionFactory() -> ComSoftwareverdeBitcoinServerDatabaseDatabaseConnectionFactory {
        return self
    }

    public override func close() {
        print("Shutting down database...up to \(_databaseConnections.count) connections may be closed.")
        var closedConnectionCount = 0
        for reference : WeakReference<DatabaseConnection> in _databaseConnections {
            if let databaseConnection : DatabaseConnection = reference.value {
                databaseConnection.close()
                closedConnectionCount += 1
            }
        }
        _databaseConnections.removeAll()
        print("Closed " + String(closedConnectionCount) + " connections.")
    }
}
