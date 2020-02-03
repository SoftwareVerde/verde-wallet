import Foundation

public class Secp256k1Util {
    public static func signData(privateKey: Secp256k1PrivateKey, data: Data) -> Signature {
        let iosByteArray : IOSByteArray = IOSByteArray(nsData: data)
        let coreSignature : ComSoftwareverdeBitcoinSecp256k1SignatureSignature = ComSoftwareverdeUtilSecp256k1Util.signData(with: privateKey.getCore(), with: iosByteArray)
        return Signature(core: coreSignature)
    }

    public static func verifySignature(signature: Signature, publicKey: Secp256k1PublicKey, data: Data) -> Bool {
        let iosByteArray : IOSByteArray = IOSByteArray(nsData: data)
        return ComSoftwareverdeUtilSecp256k1Util.verifySignature(with: signature.getCore(), with: publicKey.getCore(), with: iosByteArray)
    }
}

public class Signature {
    public static func fromJavaSignature(_ javaSignature: ComSoftwareverdeBitcoinSecp256k1SignatureSignature) -> Signature {
        return Signature(core: javaSignature)
    }

    fileprivate let _core : ComSoftwareverdeBitcoinSecp256k1SignatureSignature

    fileprivate init(core: ComSoftwareverdeBitcoinSecp256k1SignatureSignature) {
        _core = core
    }

    public func encode() -> Data? {
        let derBytes : IOSByteArray? = _core.encode()?.getBytes()
        return derBytes?.toNSData()
    }

    public func getCore() -> ComSoftwareverdeBitcoinSecp256k1SignatureSignature {
        return _core
    }

    public func toString() -> String {
        return HexUtil.toHexString(self.encode()!)!
    }
}

public class Secp256k1Signature : Signature {
    public static func fromBytes(_ bytes : IOSByteArray) -> Signature {
        let byteArray : ComSoftwareverdeConstableBytearrayImmutableByteArray = ComSoftwareverdeConstableBytearrayImmutableByteArray(byteArray: bytes)
        let core : ComSoftwareverdeBitcoinSecp256k1SignatureSecp256k1Signature = ComSoftwareverdeBitcoinSecp256k1SignatureSecp256k1Signature.fromBytes(with: byteArray)
        return Secp256k1Signature(core: core)
    }
    
    public init(core: ComSoftwareverdeBitcoinSecp256k1SignatureSecp256k1Signature) {
        super.init(core: core)
    }
}

public class BitcoinUtil {
    public static func doubleSha256(data: IOSByteArray) -> IOSByteArray {
        return ComSoftwareverdeUtilBitcoinUtil.doubleSha256(with: data)
    }
}
