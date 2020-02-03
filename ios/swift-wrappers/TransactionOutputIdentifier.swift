import Foundation

public class TransactionOutputIdentifier {
    // TODO: public static fromTransactionInput
    
    fileprivate let _core : ComSoftwareverdeBitcoinTransactionOutputIdentifierTransactionOutputIdentifier
    
    public init(core: ComSoftwareverdeBitcoinTransactionOutputIdentifierTransactionOutputIdentifier) {
        _core = core
    }
    
    public func getCore() -> ComSoftwareverdeBitcoinTransactionOutputIdentifierTransactionOutputIdentifier {
        return _core
    }
}
