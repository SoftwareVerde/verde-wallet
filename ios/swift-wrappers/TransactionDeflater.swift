import Foundation

public class TransactionDeflater {
    fileprivate let _core : ComSoftwareverdeBitcoinTransactionTransactionDeflater
    
    public init() {
        _core = ComSoftwareverdeBitcoinTransactionTransactionDeflater()
    }
    
    public func getCore() -> ComSoftwareverdeBitcoinTransactionTransactionDeflater {
        return _core
    }
    
    public func toBytes(transaction: Transaction) -> ComSoftwareverdeConstableBytearrayByteArrayProtocol {
        return _core.toBytes(with: transaction.getCore())
    }
}
