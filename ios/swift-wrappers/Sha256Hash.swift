import Foundation

public protocol Sha256Hash {
    init(core: ComSoftwareverdeBitcoinHashSha256Sha256HashProtocol)
    
    static func fromHexString(hexString: String) -> Sha256Hash
    
    static func copyOf(bytes: IOSByteArray) -> Sha256Hash
    
    func toString() -> String
}

public class ImmutableSha256Hash : Sha256Hash {
    fileprivate let _core : ComSoftwareverdeBitcoinHashSha256ImmutableSha256Hash
    
    required public init(core: ComSoftwareverdeBitcoinHashSha256Sha256HashProtocol) {
        _core = core as! ComSoftwareverdeBitcoinHashSha256ImmutableSha256Hash
    }
    
    public init(core: ComSoftwareverdeBitcoinHashSha256ImmutableSha256Hash) {
        _core = core
    }
    
    public static func fromHexString(hexString: String) -> Sha256Hash {
        return ImmutableSha256Hash(core: ComSoftwareverdeBitcoinHashSha256Sha256Hash.fromHexString(with: hexString) as! ComSoftwareverdeBitcoinHashSha256ImmutableSha256Hash)
    }
    
    public static func copyOf(bytes: IOSByteArray) -> Sha256Hash {
        return ImmutableSha256Hash(core: ComSoftwareverdeBitcoinHashSha256Sha256Hash.copyOf(with: bytes) as! ComSoftwareverdeBitcoinHashSha256ImmutableSha256Hash)
    }
    
    public func toString() -> String {
        let data : Data = (_core.getBytes()?.toNSData())!
        return HexUtil.toHexString(data)!
    }
    
    public func getCore() -> ComSoftwareverdeBitcoinHashSha256ImmutableSha256Hash {
        return _core
    }
}

public class MutableSha256Hash : Sha256Hash {
    fileprivate let _core : ComSoftwareverdeBitcoinHashSha256MutableSha256Hash

    public required init(core: ComSoftwareverdeBitcoinHashSha256Sha256HashProtocol) {
        _core = core as! ComSoftwareverdeBitcoinHashSha256MutableSha256Hash
    }    
    
    public init(core: ComSoftwareverdeBitcoinHashSha256MutableSha256Hash) {
        _core = core
    }
    
    public static func fromHexString(hexString: String) -> Sha256Hash {
        return MutableSha256Hash(core: ComSoftwareverdeBitcoinHashSha256Sha256Hash.fromHexString(with: hexString) as! ComSoftwareverdeBitcoinHashSha256MutableSha256Hash)
    }
    
    public static func copyOf(bytes: IOSByteArray) -> Sha256Hash {
        return MutableSha256Hash(core: ComSoftwareverdeBitcoinHashSha256Sha256Hash.copyOf(with: bytes) as! ComSoftwareverdeBitcoinHashSha256MutableSha256Hash)
    }
    
    public func toString() -> String {
        let data : Data = (_core.getBytes()?.toNSData())!
        return HexUtil.toHexString(data)!
    }
    
    public func getCore() -> ComSoftwareverdeBitcoinHashSha256MutableSha256Hash {
        return _core
    }
}
