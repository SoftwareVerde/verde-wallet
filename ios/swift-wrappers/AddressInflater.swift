import Foundation

public class AddressInflater {
    fileprivate let _core : ComSoftwareverdeBitcoinAddressAddressInflater
    
    public init() {
        _core = ComSoftwareverdeBitcoinAddressAddressInflater()
    }
    
    public init(core: ComSoftwareverdeBitcoinAddressAddressInflater) {
        _core = core
    }
    
    public func getCore() -> ComSoftwareverdeBitcoinAddressAddressInflater {
        return _core
    }
    
    public func fromPublicKey(publicKey: Secp256k1PublicKey) -> Address {
        let addressCore : ComSoftwareverdeBitcoinAddressAddress = _core.fromPublicKey(with: publicKey.getCore())
        return Address(core: addressCore)
    }
    
    public func compressedFromPublicKeys(publicKey: Secp256k1PublicKey) -> CompressedAddress {
        let compressedAddressCore : ComSoftwareverdeBitcoinAddressCompressedAddress = _core.compressedFromPublicKey(with: publicKey.getCore())
        return CompressedAddress(core: compressedAddressCore)
    }
    
    public func fromBase32Check(base32String: String) -> Address? {
        if let addressCore : ComSoftwareverdeBitcoinAddressAddress = _core.fromBase32Check(with: base32String) {
            return Address(core: addressCore)
        }
        
        return nil
    }
}
