import Foundation

public class SlpTokenId {
    fileprivate let _core : ComSoftwareverdeBitcoinSlpSlpTokenId!
    
    public init(core: ComSoftwareverdeBitcoinSlpSlpTokenId) {
        _core = core
    }
    
    public static func fromHexString(hexString: String) -> SlpTokenId {
        let core : ComSoftwareverdeBitcoinSlpSlpTokenId = ComSoftwareverdeBitcoinSlpSlpTokenId.fromHexString(with: hexString)
        return SlpTokenId(core: core)
    }
    
    public func getCore() -> ComSoftwareverdeBitcoinSlpSlpTokenId {
        return _core
    }
}
