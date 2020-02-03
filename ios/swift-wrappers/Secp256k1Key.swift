import Foundation

public class Secp256k1PrivateKey : NSObject {
    public static func fromPrivateKey(_ core: ComSoftwareverdeBitcoinSecp256k1KeyPrivateKey) -> Secp256k1PrivateKey {
        return Secp256k1PrivateKey(core: core)
    }

    public static func createNewKey() -> Secp256k1PrivateKey {
        let core : ComSoftwareverdeBitcoinSecp256k1KeyPrivateKey = ComSoftwareverdeBitcoinSecp256k1KeyPrivateKey_createNewKey()
        return Secp256k1PrivateKey(core: core)
    }

    public static func fromHexString(_ hexString: String) -> Secp256k1PrivateKey? {
        if let core : ComSoftwareverdeBitcoinSecp256k1KeyPrivateKey = ComSoftwareverdeBitcoinSecp256k1KeyPrivateKey_fromHexStringWithNSString_(hexString) {
            return Secp256k1PrivateKey(core: core)
        }
        return nil
    }
    
    fileprivate let _core : ComSoftwareverdeBitcoinSecp256k1KeyPrivateKey

    fileprivate init(core: ComSoftwareverdeBitcoinSecp256k1KeyPrivateKey) {
        _core = core
    }
    
    fileprivate init(keyBytes: ComSoftwareverdeConstableBytearrayByteArrayProtocol) {
        _core = ComSoftwareverdeBitcoinSecp256k1KeyPrivateKey(comSoftwareverdeConstableBytearrayByteArray: keyBytes)
    }

    public func getPublicKey() -> Secp256k1PublicKey {
        let publicKeyCore : ComSoftwareverdeBitcoinSecp256k1KeyPublicKey = _core.getPublicKey()
        return Secp256k1PublicKey(core: publicKeyCore)
    }

    public func getCore() -> ComSoftwareverdeBitcoinSecp256k1KeyPrivateKey {
        return _core
    }
}

public class Secp256k1PublicKey : NSObject {
    fileprivate let _core : ComSoftwareverdeBitcoinSecp256k1KeyPublicKey

    fileprivate init(core: ComSoftwareverdeBitcoinSecp256k1KeyPublicKey) {
        _core = core
    }

    public static func fromCore(core: ComSoftwareverdeBitcoinSecp256k1KeyPublicKey) -> Secp256k1PublicKey {
        return Secp256k1PublicKey(core: core)
    }
    
    public static func fromBytes(bytes: IOSByteArray) -> Secp256k1PublicKey {
        let byteArray : ComSoftwareverdeConstableBytearrayByteArrayProtocol! = ComSoftwareverdeConstableBytearrayImmutableByteArray(byteArray: bytes)
        return Secp256k1PublicKey(core: ComSoftwareverdeBitcoinSecp256k1KeyPublicKey.fromBytes(with: byteArray))
    }
    
    public func compress() -> Secp256k1PublicKey {
        let compressed : ComSoftwareverdeBitcoinSecp256k1KeyPublicKey = _core.compress();
        return Secp256k1PublicKey(core: compressed);
    }

    public func getCore() -> ComSoftwareverdeBitcoinSecp256k1KeyPublicKey {
        return _core
    }

    public func toString() -> String {
        return _core.description()
    }
}

