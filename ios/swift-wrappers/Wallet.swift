import Foundation

public class Wallet {
    fileprivate let _core : ComSoftwareverdeBitcoinWalletWallet
    
    public init(core: ComSoftwareverdeBitcoinWalletWallet) {
        _core = core
    }
    
    public func getDustThreshold(addressIsCompressed: Bool) -> Int64 {
        let dustThresholdJavaLangLong : JavaLangLong = _core.getDustThreshold(with: JavaLangBoolean(boolean: addressIsCompressed))
        return Int64(dustThresholdJavaLangLong.longLongValue())
    }
    
    public func getOutputsToSpend(newTransactionOutputCount: Int, desiredSpendAmount: Int64) -> ComSoftwareverdeConstableListList {
        return _core.getOutputsToSpend(with: JavaLangInteger(integerLiteral: newTransactionOutputCount), with: JavaLangLong(value: desiredSpendAmount))
    }
    
    public func createTransaction(paymentAmounts: List<ComSoftwareverdeBitcoinWalletPaymentAmount>, changeAddress: Address, transactionOutputIdentifiers: List<ComSoftwareverdeBitcoinWalletPaymentAmount>) -> Transaction {
        let transactionCore : ComSoftwareverdeBitcoinTransactionTransactionProtocol = _core.createTransaction(with: paymentAmounts.getCore(), with: changeAddress.getCore(), with: transactionOutputIdentifiers.getCore())
        return Transaction(core: transactionCore)
    }
    
    public func createSlpTokenTransaction(slpTokenId: SlpTokenId, paymentAmounts: List<ComSoftwareverdeBitcoinWalletSlpSlpPaymentAmount>, address: Address, requiredTransactionOutputIdentifiersToSpend: List<TransactionOutputIdentifier>) -> Transaction? {
        guard let transactionCore : ComSoftwareverdeBitcoinTransactionTransactionProtocol = _core.createSlpTokenTransaction(with: slpTokenId.getCore(), with: paymentAmounts.getCore(), with: address.getCore(), with: requiredTransactionOutputIdentifiersToSpend.getCore()) else {
            return nil
        }
        
        return Transaction(core: transactionCore)
    }
    
    public func getSlpTokenBalance(slpTokenId: SlpTokenId) -> Int64 {
        return _core.getSlpTokenBalance(with: slpTokenId.getCore()).longLongValue()
    }
}
