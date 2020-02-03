import Foundation

public class IOSKeyStore : NSObject, ComSoftwareverdeBitcoinAppLibKeyStore {
    public static let DEFAULT_KEY_STORE_INITIALIZATION_TIMESTAMP : UInt = 1558828800;
    
    private let _privateKeys : MutableList<ComSoftwareverdeBitcoinSecp256k1KeyPrivateKey>!
    
    public init(privateKeys: MutableList<ComSoftwareverdeBitcoinSecp256k1KeyPrivateKey>) {
        _privateKeys = privateKeys
    }
    
    public func hasKeys() -> JavaLangBoolean! {
        return JavaLangBoolean(boolean: true)
    }
    
    public func createPrivateKey() -> ComSoftwareverdeBitcoinSecp256k1KeyPrivateKey! {
        return nil // UnsupportedOperationException in Android Version
    }
    
    public func storePrivateKey(with privateKey: ComSoftwareverdeBitcoinSecp256k1KeyPrivateKey!) {
        if _privateKeys.contains(item: privateKey) {
            return
        }
        _privateKeys.add(item: privateKey)
    }
    
    public func getPrivateKeys() -> ComSoftwareverdeConstableListList! {
        return _privateKeys.getCore()
    }
    
    public func getPrivateKey(with: ComSoftwareverdeBitcoinSecp256k1KeyPublicKey) -> ComSoftwareverdeBitcoinSecp256k1KeyPrivateKey! {
        let compressedPublicKey : ComSoftwareverdeBitcoinSecp256k1KeyPublicKey = with.compress()
        let privateKeysSize : Int = _privateKeys.getSize()
        
        for i in 0..<privateKeysSize {
            let keyStorePrivateKey : ComSoftwareverdeBitcoinSecp256k1KeyPrivateKey = _privateKeys.get(index: i)
            let keyStorePublicKey : ComSoftwareverdeBitcoinSecp256k1KeyPublicKey = keyStorePrivateKey.getPublicKey()
            let compressedKeyStorePublicKey : ComSoftwareverdeBitcoinSecp256k1KeyPublicKey = keyStorePublicKey.compress()
            
            if Util.areEqual(a: compressedKeyStorePublicKey.getBytes(), b: compressedPublicKey.getBytes()) {
                return keyStorePrivateKey
            }
        }
        
        return nil
    }
    
    public func setInitializationTimestampWith(_ initializationTimestamp: JavaLangLong!) {
        print("BITCOIN_VERDE_KEYSTORE", "setInitializationTimestamp() called.")
    }
    
    public func getInitializationTimestamp() -> JavaLangLong! {
        let userDefaults : UserDefaults = UserDefaults.standard
        let identityDataSharedPreferences : [String : Any] = userDefaults.dictionary(forKey: IOSKeyManager.IDENTITY_DATA_SHARED_PREFERENCES_NAME) ?? [:]
        
        if let timestamp : UInt = identityDataSharedPreferences[IOSKeyManager.PRIMARY_IDENTITY_SYNCHRONIZATION_TIMESTAMP] as? UInt {
            return create_JavaLangLong_initWithLong_(jlong(timestamp))
        }
        
        print("Unable to determine PRIMARY_IDENTITY_SYNCHRONIZATION_TIMESTAMP in user defaults. Using default timestamp.")
        
        return create_JavaLangLong_initWithLong_(jlong(IOSKeyStore.DEFAULT_KEY_STORE_INITIALIZATION_TIMESTAMP)) // Bitcoin requires this value to be in SECONDS
    }
    
    public func clear() {
        print("BITCOIN_VERDE_KEYSTORE", "clear() called.")
    }
}
