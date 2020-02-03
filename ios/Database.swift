import SQLite3

class WeakReference<T: AnyObject> {
    weak var value : T?

    init(_ object: T) {
        self.value = object
    }
}

public class Database : ComSoftwareverdeBitcoinServerDatabaseDatabase {
    private var _fileUrl : URL
    private var _databaseConnections : [WeakReference<DatabaseConnection>] = []

    public override init() {
        _fileUrl = try! FileManager.default.url(for: .documentDirectory, in: .userDomainMask, appropriateFor: nil, create: false)
            .appendingPathComponent("bitcoin.sqlite")

        super.init()
    }

    public override func newConnection() -> ComSoftwareverdeBitcoinServerDatabaseDatabaseConnection! {
        var sqliteDatabase : OpaquePointer?

        if sqlite3_open(_fileUrl.path, &sqliteDatabase) != SQLITE_OK {
            print("Error opening database " + _fileUrl.path)
            return nil
        }

        let databaseConnection = DatabaseConnection(database: sqliteDatabase!)
        _databaseConnections.append(WeakReference(databaseConnection))
        return ComSoftwareverdeBitcoinServerDatabaseDatabaseConnection(comSoftwareverdeDatabaseDatabaseConnection: databaseConnection)
    }

    public override func newConnectionFactory() -> ComSoftwareverdeBitcoinServerDatabaseDatabaseConnectionFactory {
        return self
    }

    public override func close() {
        print("Shutting down database...")
        var closedConnectionCount = 0
        for reference : WeakReference<DatabaseConnection> in _databaseConnections {
            if let databaseConnection = reference.value {
                databaseConnection.close()
                closedConnectionCount += 1
            }
        }
        _databaseConnections.removeAll()
        print("Closed " + String(closedConnectionCount) + " connections.")
    }
}
