import Foundation

public class List<T> {
    fileprivate let _core : ComSoftwareverdeConstableListList!
    
    public init(core: ComSoftwareverdeConstableListList) {
        _core = core
    }
    
    func getCore() -> ComSoftwareverdeConstableListList {
        return _core
    }
    
    func get(index: Int) -> T {
        return _core.getWith(jint(index)) as! T
    }
    
    func getSize() -> Int {
        return Int(_core.getSize())
    }
    
    func isEmpty() -> Bool {
        return _core.isEmpty()
    }
    
    func contains(item: T) -> Bool {
        return _core.contains(withId: item)
    }
    
    func indexOf(item: T) -> Int {
        return Int(_core.indexOf(withId: item))
    }
}

public class MutableList<T> : List<T> {
    public init() {
        let core : ComSoftwareverdeConstableListMutableMutableList = ComSoftwareverdeConstableListMutableMutableList()
        super.init(core: core)
    }
    
    public func add(item: T) {
        let core : ComSoftwareverdeConstableListMutableMutableList = _core as! ComSoftwareverdeConstableListMutableMutableList
        core.add(withId: item)
    }
    
    public func add(index: Int, item: T) {
        let core : ComSoftwareverdeConstableListMutableMutableList = _core as! ComSoftwareverdeConstableListMutableMutableList
        core.add(with: jint(index), withId: item)
    }
    
    public func set(index: Int, item: T) {
        let core : ComSoftwareverdeConstableListMutableMutableList = _core as! ComSoftwareverdeConstableListMutableMutableList
        core.setWith(jint(index), withId: item)
    }
}

public class ImmutableList<T> : List<T> {
    public init(collection: JavaUtilCollection) {
        let core : ComSoftwareverdeConstableListImmutableImmutableList = ComSoftwareverdeConstableListImmutableImmutableList(javaUtilCollection: collection)
        super.init(core: core)
    }
}


