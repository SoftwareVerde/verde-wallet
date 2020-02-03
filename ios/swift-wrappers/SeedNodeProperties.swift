
import Foundation

public class SeedNodeProperties {
    var _core : ComSoftwareverdeBitcoinServerConfigurationSeedNodeProperties

    public init(host: String, port: Int?) {
        let javaPort : JavaLangInteger? = (port == nil ? nil : new_JavaLangInteger_initWithInt_(jint(port!)))
        _core = ComSoftwareverdeBitcoinServerConfigurationSeedNodeProperties.init(nsString: host, with: javaPort)
    }

    public func getAddress() -> String {
        return _core.getAddress()
    }

    public func getPort() -> Int32? {
        return _core.getPort()?.intValue()
    }

    public func getCore() -> ComSoftwareverdeBitcoinServerConfigurationSeedNodeProperties {
        return _core
    }
}
