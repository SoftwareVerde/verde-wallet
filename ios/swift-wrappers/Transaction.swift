import Foundation

public class Transaction {
    fileprivate let _core : ComSoftwareverdeBitcoinTransactionTransactionProtocol!
    
    public init(core: ComSoftwareverdeBitcoinTransactionTransactionProtocol) {
        _core = core
    }
    
    public func getCore() -> ComSoftwareverdeBitcoinTransactionTransactionProtocol {
        return _core
    }
    
    public func getVersion() -> UInt {
        return _core.getVersion() as! UInt
    }
    
    public func toJson() -> Json {
        return Json.fromJavaJson(_core.toJson())
    }
}

public class MutableTransaction : Transaction {
    public init() {
        let core : ComSoftwareverdeBitcoinTransactionMutableTransaction = ComSoftwareverdeBitcoinTransactionMutableTransaction()
        super.init(core: core)
    }
    
    public init(core: ComSoftwareverdeBitcoinTransactionMutableTransaction) {
        super.init(core: core)
    }
}
