import SQLite3

public class DatabaseConnection : NSObject, ComSoftwareverdeDatabaseDatabaseConnection {
    fileprivate static let _SQLITE_STATIC = unsafeBitCast(0, to: sqlite3_destructor_type.self)
    fileprivate static let _SQLITE_TRANSIENT = unsafeBitCast(-1, to: sqlite3_destructor_type.self)

    fileprivate typealias SqliteDatabase = OpaquePointer
    fileprivate typealias SqlQuery = OpaquePointer

    private var _database : OpaquePointer?
    private static let _actionLock = NSRecursiveLock()
    private static let _transactionLock = NSRecursiveLock()

    public init(database: OpaquePointer) {
        _database = database
    }

    private func _getErrorMessage() -> String {
        return String(cString: sqlite3_errmsg(_database))
    }

    private func _throwDatabaseException(_ errorMessage: String) {
        let exception = create_ComSoftwareverdeDatabaseDatabaseException_initWithNSString_(errorMessage)
        exception!.raise()
    }

    private func _cleanupQuery(_ query: String) -> String {
        var newQuery : String = query
        if let range = newQuery.range(of: "INSERT IGNORE") {
            newQuery = newQuery.replacingCharacters(in: range, with: "INSERT OR IGNORE")
        }
        newQuery = newQuery.replacingOccurrences(of: "IS NULL", with: "IN ('', NULL)")
        return newQuery
    }

    fileprivate func _bindQuery(query: String, parameters: IOSObjectArray) -> SqlQuery {
        var statement : SqlQuery? = nil

        if sqlite3_prepare_v2(_database, query, -1, &statement, nil) != SQLITE_OK {
            _throwDatabaseException("Unable to prepare statement: " + _getErrorMessage())
        }
        for i : jint in 0 ..< parameters.length() {
            let parameter : Any? = parameters.object(at: UInt(i))
            let parameterIndex = Int32(i+1)
            if parameter == nil {
                sqlite3_bind_null(statement, parameterIndex)
                continue
            }

            let stringParameter : String? = parameter as? String
            if (stringParameter != nil) {
                sqlite3_bind_text(statement, parameterIndex, stringParameter!, -1, DatabaseConnection._SQLITE_TRANSIENT)
                continue
            }

            let typedParameter : ComSoftwareverdeDatabaseQueryParameterTypedParameter? = parameter as? ComSoftwareverdeDatabaseQueryParameterTypedParameter
            if typedParameter != nil {
                let parameterType = typedParameter?.value(forKey: "type_") as! ComSoftwareverdeDatabaseQueryParameterParameterType
                let value = typedParameter?.value(forKey: "value_")

                if value == nil {
                    sqlite3_bind_null(statement, parameterIndex)
                    continue
                }
                else if parameterType == ComSoftwareverdeDatabaseQueryParameterParameterType_fromOrdinal(ComSoftwareverdeDatabaseQueryParameterParameterType_Enum.BYTE_ARRAY.rawValue) {
                    let byteArray = value as! IOSByteArray
                    let data = byteArray.toNSData()!
                    if data.withUnsafeBytes({ (pointer: UnsafeRawBufferPointer) -> Int32 in
                        sqlite3_bind_blob(statement, parameterIndex, pointer.baseAddress, Int32(data.count), DatabaseConnection._SQLITE_TRANSIENT)
                    }) != SQLITE_OK {
                        _throwDatabaseException("Unable to bind blob: " + _getErrorMessage())
                    }
                    continue
                }
                else if parameterType == ComSoftwareverdeDatabaseQueryParameterParameterType_fromOrdinal(ComSoftwareverdeDatabaseQueryParameterParameterType_Enum.STRING.rawValue) {
                    sqlite3_bind_text(statement, parameterIndex, value as! String , -1, DatabaseConnection._SQLITE_TRANSIENT)
                    continue
                }
                else if parameterType == ComSoftwareverdeDatabaseQueryParameterParameterType_fromOrdinal(ComSoftwareverdeDatabaseQueryParameterParameterType_Enum.BOOLEAN.rawValue) {
                    sqlite3_bind_int(statement, parameterIndex, value as! Int32)
                    continue
                }

                _throwDatabaseException("Unable to set parmeter " + String(i) + " to " + String(describing: parameter))
            }
        }

        return statement!
    }

    fileprivate func _execSql(query : String, parameters : IOSObjectArray) {
        let modifiedQuery = _cleanupQuery(query)

        let statement = _bindQuery(query: modifiedQuery, parameters: parameters)

        // must move the cursor to complete query
        sqlite3_step(statement)

        if sqlite3_finalize(statement) != SQLITE_OK {
            _throwDatabaseException("Query execution failed: " + _getErrorMessage())
        }
    }

    open func getInsertId() -> JavaLangLong! {
        return JavaLangLong(long: sqlite3_last_insert_rowid(_database));
    }

    public func executeDdl(with query: String!) {
        if sqlite3_exec(_database, query, nil, nil, nil) != SQLITE_OK {
            _throwDatabaseException("Failed DDL: " + _getErrorMessage())
        }
    }

    public func executeDdl(with query: ComSoftwareverdeDatabaseQuery!) {
        let queryString = query.getString()
        if sqlite3_exec(_database, queryString, nil, nil, nil) != SQLITE_OK {
            _throwDatabaseException("Failed DDL: " + _getErrorMessage())
        }
    }

    private func _executeWithInsertId(query: String!, parameters: IOSObjectArray!) -> JavaLangLong! {
        DatabaseConnection._actionLock.lock()
        defer {
            DatabaseConnection._actionLock.unlock()
        }

        let requiresMiniTransaction = _getAutoCommit()
        if requiresMiniTransaction {
            startTransaction()
        }

        _execSql(query: query, parameters: parameters)
        let insertId = getInsertId()

        if requiresMiniTransaction {
            rollbackTransaction()
        }

        return insertId
    }

    public func executeSql(with query: String!, withNSStringArray parameters: IOSObjectArray!) -> JavaLangLong! {
        return _executeWithInsertId(query: query, parameters: parameters)
    }

    public func executeSql(with query: ComSoftwareverdeDatabaseQuery!) -> JavaLangLong! {
        let parameters = query.getParameters()!.toArray()
        let queryString = query.getString()!

        return _executeWithInsertId(query: queryString, parameters: parameters)
    }

    public func query(with query: String!, withNSStringArray parameters: IOSObjectArray!) -> JavaUtilList! {
        DatabaseConnection._actionLock.lock()
        defer {
            DatabaseConnection._actionLock.unlock()
        }

        let statement : SqlQuery = _bindQuery(query: query, parameters: parameters)

        let results : JavaUtilList = JavaUtilArrayList()

        while (sqlite3_step(statement) == SQLITE_ROW) {
            var row : Dictionary<String, Any?> = Dictionary<String, Any?>()

            let columnCount : Int = NSInteger(sqlite3_data_count(statement)) as Int
            for i : Int in 0 ..< columnCount {
                let index = Int32(i)

                var columnName : String! = String(cString: sqlite3_column_name(statement, index))
                if (columnName == nil) {
                    columnName = "column_\(i)"
                }

                var columnValue : Any? = nil

                let columnType = sqlite3_column_type(statement, index)
                if columnType == SQLITE_TEXT {
                    let rawColumnValue : UnsafePointer<UInt8>? = sqlite3_column_text(statement, index)
                    if (rawColumnValue != nil) {
                        columnValue = String(cString: rawColumnValue!)
                    }
                }
                else if columnType == SQLITE_BLOB {
                    let rawColumnValue : UnsafeRawPointer? = sqlite3_column_blob(statement, index)
                    if (rawColumnValue != nil) {
                        let size = sqlite3_column_bytes(statement, index)
                        columnValue = IOSByteArray(nsData: Data(bytes: rawColumnValue!, count: Int(size)))
                    }
                }
                else if columnType == SQLITE_INTEGER {
                    let size = sqlite3_column_bytes(statement, index)
                    if size > 0 {
                        let rawColumnValue = sqlite3_column_int(statement, index)
                        columnValue = Int(rawColumnValue)
                    }
                }

                row[columnName] = columnValue
            }

            results.add(withId: DatabaseRow.fromDictionary(row))
        }

        if sqlite3_finalize(statement) != SQLITE_OK {
            _throwDatabaseException("Unable to executed query: " + _getErrorMessage())
        }

        return results
    }

    public func query(with query: ComSoftwareverdeDatabaseQuery!) -> JavaUtilList! {
        let parameters = query.getParameters()?.toArray()
        let queryString = query.getString()

        return self.query(with: queryString, withNSStringArray: parameters)
    }

    public func getRawConnection() -> Any! {
        return RawConnection(databaseConnection: self)
    }

    public func setAutoCommit(_ autoCommit: Bool) {
        if getAutoCommit() {
            if !autoCommit {
                startTransaction()
            }
        }
        else {
            if autoCommit {
                rollbackTransaction()
            }
        }
    }

    private func _getAutoCommit() -> Bool {
        return sqlite3_get_autocommit(_database) != 0
    }

    public func getAutoCommit() -> Bool {
        return _getAutoCommit()
    }

    public func startTransaction() {
        DatabaseConnection._transactionLock.lock()
        if sqlite3_exec(_database, "BEGIN TRANSACTION", nil, nil, nil) != SQLITE_OK {
            DatabaseConnection._transactionLock.unlock()
            _throwDatabaseException("Unable to start transaction: " + _getErrorMessage())
        }
    }

    public func commitTransaction() {
        defer {
            DatabaseConnection._transactionLock.unlock()
        }
        if sqlite3_exec(_database, "COMMIT TRANSACTION", nil, nil, nil) != SQLITE_OK {
            _throwDatabaseException("Unable to commit transaction: " + _getErrorMessage())
        }
    }

    public func rollbackTransaction() {
        defer {
            DatabaseConnection._transactionLock.unlock()
        }
        if sqlite3_exec(_database, "ROLLBACK TRANSACTION", nil, nil, nil) != SQLITE_OK {
            _throwDatabaseException("Unable to rollback transaction: " + _getErrorMessage())
        }
    }

    public func close() {
        DatabaseConnection._actionLock.lock()
        defer {
            DatabaseConnection._actionLock.unlock()
        }

        if _database != nil {
            if DatabaseConnection._transactionLock.try() {
                defer {
                    DatabaseConnection._transactionLock.unlock()
                }
                if !_getAutoCommit() {
                    if sqlite3_exec(_database, "ROLLBACK TRANSACTION", nil, nil, nil) != SQLITE_OK {
                        print("Unable to rollback transaction on close: " + _getErrorMessage())
                    }
                }
            }
        }

        if sqlite3_close(_database) != SQLITE_OK {
            print("Error closing database: " + _getErrorMessage())
        }

        _database = nil
    }
}

class DatabaseRow : NSObject, ComSoftwareverdeDatabaseRow {
    public static func fromDictionary(_ dictionary : Dictionary<String, Any?>) -> DatabaseRow {
        let iosDatabaseRow : DatabaseRow = DatabaseRow()
        for columnName in dictionary.keys {
            let columnValue : Any? = dictionary[columnName]!

            iosDatabaseRow._columnNames.append(columnName)

            iosDatabaseRow._columnValues.updateValue(columnValue, forKey: columnName)
        }

        return iosDatabaseRow
    }

    fileprivate var _columnNames : Array<String> = Array<String>()
    fileprivate var _columnValues : Dictionary<String, Any?> = Dictionary<String, Any?>()

    fileprivate override init() { }

    open func getColumnNames() -> JavaUtilList! {
        let javaList : JavaUtilList = JavaUtilArrayList(int: Int32(_columnNames.count))

        for columnName : String in _columnNames {
            javaList.add(withId: columnName)
        }

        return javaList
    }

    open func getValueWith(_ columnName: String!) -> String! {
        if (_columnValues[columnName] as? String == nil) {
            return nil
        }
        return (_columnValues[columnName!]! as! String)
    }

    public func getStringWith(_ columnName: String!) -> String! {
        if (_columnValues[columnName] as? String == nil) {
            return nil
        }
        return (_columnValues[columnName]! as! String)
    }

    public func getIntegerWith(_ columnName: String!) -> JavaLangInteger! {
        if (_columnValues[columnName] as? Int == nil) {
            return nil
        }
        return JavaLangInteger(value: _columnValues[columnName] as! Int)
    }

    public func getLongWith(_ columnName: String!) -> JavaLangLong! {
        if (_columnValues[columnName] as? Int == nil) {
            return nil
        }
        return JavaLangLong(long: Int64(_columnValues[columnName] as! Int))
    }

    public func getFloatWith(_ columnName: String!) -> JavaLangFloat! {
        if (_columnValues[columnName] == nil) {
            return nil
        }
        return JavaLangFloat(value: _columnValues[columnName] as! Int)
    }

    public func getDoubleWith(_ columnName: String!) -> JavaLangDouble! {
        if (_columnValues[columnName] as? Int == nil) {
            return nil
        }
        return JavaLangDouble(value: _columnValues[columnName]! as! Int)
    }

    public func getBooleanWith(_ columnName: String!) -> JavaLangBoolean! {
        if (_columnValues[columnName] as? Int == nil) {
            return nil
        }
        return JavaLangBoolean(boolean: (_columnValues[columnName]! as! Int) > 0)
    }

    public func getBytesWith(_ columnName: String!) -> IOSByteArray! {
        if (_columnValues[columnName] as? IOSByteArray == nil) {
            return nil
        }
        return (_columnValues[columnName]! as! IOSByteArray)
    }
}

class RawConnection : NSObject, JavaSqlConnectionProtocol {
    private var _databaseConnection : DatabaseConnection

    init(databaseConnection: DatabaseConnection) {
        _databaseConnection = databaseConnection
    }

    func close() {
        _databaseConnection.close()
    }

    func createStatement() -> JavaSqlStatementProtocol! {
        return nil
    }

    func prepareStatement(with sql: String!) -> JavaSqlPreparedStatement! {
        return nil
    }

    func prepareCall(with sql: String!) -> JavaSqlCallableStatement! {
        return nil
    }

    func nativeSQL(with sql: String!) -> String! {
        return nil
    }

    func setAutoCommitWithBoolean(_ autoCommit: jboolean) {
        if !autoCommit {
            _databaseConnection.startTransaction()
        }
    }

    func getAutoCommit() -> jboolean {
        return _databaseConnection.getAutoCommit()
    }

    func commit() {
        _databaseConnection.commitTransaction()
    }

    func rollback() {
        _databaseConnection.rollbackTransaction()
    }

    func isClosed() -> jboolean {
        return false
    }

    func getMetaData() -> JavaSqlDatabaseMetaDataProtocol! {
        return nil
    }

    func setReadOnlyWithBoolean(_ readOnly: jboolean) {

    }

    func isReadOnly() -> jboolean {
        return false
    }

    func setCatalogWith(_ catalog: String!) {

    }

    func getCatalog() -> String! {
        return nil
    }

    private var _isolationLevel : Int32 = JavaSqlConnection_TRANSACTION_READ_UNCOMMITTED

    func setTransactionIsolationWith(_ level: jint) {
        _isolationLevel = level
    }

    func getTransactionIsolation() -> jint {
        return _isolationLevel
    }

    func getWarnings() -> JavaSqlSQLWarning! {
        return nil
    }

    func clearWarnings() {

    }

    func createStatement(with resultSetType: jint, with resultSetConcurrency: jint) -> JavaSqlStatementProtocol! {
        return nil
    }

    func prepareStatement(with sql: String!, with resultSetType: jint, with resultSetConcurrency: jint) -> JavaSqlPreparedStatement! {
        return nil
    }

    func prepareCall(with sql: String!, with resultSetType: jint, with resultSetConcurrency: jint) -> JavaSqlCallableStatement! {
        return nil
    }

    func getTypeMap() -> JavaUtilMap! {
        return nil
    }

    func setTypeMapWith(_ map: JavaUtilMap!) {

    }

    func setHoldabilityWith(_ holdability: jint) {

    }

    func getHoldability() -> jint {
        return -1
    }

    func setSavepoint() -> JavaSqlSavepoint! {
        return nil
    }

    func setSavepointWith(_ name: String!) -> JavaSqlSavepoint! {
        return nil
    }

    func rollback(with savepoint: JavaSqlSavepoint!) {

    }

    func releaseSavepoint(with savepoint: JavaSqlSavepoint!) {

    }

    func createStatement(with resultSetType: jint, with resultSetConcurrency: jint, with resultSetHoldability: jint) -> JavaSqlStatementProtocol! {
        return nil
    }

    func prepareStatement(with sql: String!, with resultSetType: jint, with resultSetConcurrency: jint, with resultSetHoldability: jint) -> JavaSqlPreparedStatement! {
        return nil
    }

    func prepareCall(with sql: String!, with resultSetType: jint, with resultSetConcurrency: jint, with resultSetHoldability: jint) -> JavaSqlCallableStatement! {
        return nil
    }

    func prepareStatement(with sql: String!, with autoGeneratedKeys: jint) -> JavaSqlPreparedStatement! {
        return nil
    }

    func prepareStatement(with sql: String!, with columnIndexes: IOSIntArray!) -> JavaSqlPreparedStatement! {
        return nil
    }

    func prepareStatement(with sql: String!, withNSStringArray columnNames: IOSObjectArray!) -> JavaSqlPreparedStatement! {
        return nil
    }

    func createClob() -> JavaSqlClob! {
        return nil
    }

    func createBlob() -> JavaSqlBlob! {
        return nil
    }

    func createNClob() -> JavaSqlNClob! {
        return nil
    }

    func createSQLXML() -> JavaSqlSQLXML! {
        return nil
    }

    func isValid(with timeout: jint) -> jboolean {
        return false
    }

    func setClientInfoWith(_ name: String!, with value: String!) {

    }

    func setClientInfoWith(_ properties: JavaUtilProperties!) {

    }

    func getClientInfo(with name: String!) -> String! {
        return nil
    }

    func getClientInfo() -> JavaUtilProperties! {
        return nil
    }

    func createArrayOf(with typeName: String!, withNSObjectArray elements: IOSObjectArray!) -> JavaSqlArray! {
        return nil
    }

    func createStruct(with typeName: String!, withNSObjectArray attributes: IOSObjectArray!) -> JavaSqlStruct! {
        return nil
    }

    func unwrap(with iface: IOSClass!) -> Any! {
        return nil
    }

    func isWrapperFor(with iface: IOSClass!) -> jboolean {
        return false
    }
}
