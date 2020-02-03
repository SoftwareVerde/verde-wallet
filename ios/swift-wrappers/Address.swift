import Foundation


public class Address {
    fileprivate let _core : ComSoftwareverdeBitcoinAddressAddress
    
    public static let BASE_32_LABEL : String = ComSoftwareverdeBitcoinAddressAddress_BASE_32_LABEL
    
    public init(bytes: IOSByteArray) {
        _core = ComSoftwareverdeBitcoinAddressAddress(byteArray: bytes)
    }
    
    public init(core: ComSoftwareverdeBitcoinAddressAddress) {
        _core = core
    }
    
    public func getCore() -> ComSoftwareverdeBitcoinAddressAddress {
        return _core
    }
    
    public func isCompressed() -> Bool {
        return _core.isCompressed()!.booleanValue()
    }
    
    public func toBase58CheckEncoded() -> String {
        return _core.toBase58CheckEncoded()
    }
    
    public func toBase32CheckEncoded() -> String {
        return _core.toBase32CheckEncoded()
    }
    
    public func toString() -> String {
        return _core.description()
    }
}

public class CompressedAddress : Address {
    public init(core: ComSoftwareverdeBitcoinAddressCompressedAddress) {
        super.init(core: core)
    }
    
    public override init(bytes: IOSByteArray) {
        let core : ComSoftwareverdeBitcoinAddressCompressedAddress = ComSoftwareverdeBitcoinAddressCompressedAddress(byteArray: bytes)
        super.init(core: core)
    }
}
