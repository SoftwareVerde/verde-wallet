import Foundation

public class Runnable : NSObject, JavaLangRunnable {
    fileprivate let _runnable : () -> ()
    
    public init(runnable: @escaping () -> ()) {
        _runnable = runnable
    }
    
    public func run() {
        _runnable()
    }
}
