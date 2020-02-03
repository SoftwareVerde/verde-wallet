import Foundation

class AesKey {
    fileprivate let _core : ComSoftwareverdeSecurityAesAesKey
    
    public init() {
        _core = ComSoftwareverdeSecurityAesAesKey()
    }
    
    public init(core: ComSoftwareverdeSecurityAesAesKey) {
        _core = core
    }
    
    public init(key: IOSByteArray) {
        _core = ComSoftwareverdeSecurityAesAesKey(byteArray: key)
    }
    
    public func getBytes() -> IOSByteArray {
        return _core.getBytes()
    }
    
    public func getCore() -> ComSoftwareverdeSecurityAesAesKey {
        return _core
    }
}
