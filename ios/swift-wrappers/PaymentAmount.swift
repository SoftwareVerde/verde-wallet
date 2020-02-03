import Foundation

public class PaymentAmount {
    fileprivate let _core : ComSoftwareverdeBitcoinWalletPaymentAmount
    
    public init(core: ComSoftwareverdeBitcoinWalletPaymentAmount) {
        _core = core
    }
    
    public func getCore() -> ComSoftwareverdeBitcoinWalletPaymentAmount {
        return _core
    }
}

public class SlpPaymentAmount : PaymentAmount {
    public init(address: Address, amount: Int64, tokenAmount: Int64) {
        let javaAmount : JavaLangLong = create_JavaLangLong_initWithLong_(jlong(exactly: amount)!)
        let javaTokenAmount : JavaLangLong = create_JavaLangLong_initWithLong_(jlong(exactly: tokenAmount)!)
        
        let core : ComSoftwareverdeBitcoinWalletSlpSlpPaymentAmount = ComSoftwareverdeBitcoinWalletSlpSlpPaymentAmount(comSoftwareverdeBitcoinAddressAddress: address.getCore(), with: javaAmount, with: javaTokenAmount)
        
        super.init(core: core)
    }
}
