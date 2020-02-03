import Foundation

public class Json {
    public static func parse(_ string: String) -> Json? {
        let data : Any? = try? JSONSerialization.jsonObject(with: string.data(using: String.Encoding.utf8)!, options: [])
        if (data == nil) { return nil }

        return Json(data: data!)
    }

    public static func fromJavaJson(_ javaJson: ComSoftwareverdeJsonJson) -> Json {
        return Json.parse(javaJson.description())!
    }
    
    public static func toJavaJson(_ json: Json) -> ComSoftwareverdeJsonJson {
        return ComSoftwareverdeJsonJson.parse(with: json.toString())
    }
    
    public static func isJson(value: String) -> Bool {
        return (ComSoftwareverdeJsonJson.isJson(with: value) != nil)
    }

    private var _data : Any?

    private init(data: Any?) {
        _data = data
    }

    private func _put(key: String, value: Any?) {
        if (_data == nil) { _data = Dictionary<String, Any?>() }
        if (!(_data is Dictionary<String, Any?>)) {
            _data = Dictionary<String, Any?>()
        }

        var data : Dictionary<String, Any?> = (_data as! Dictionary<String, Any>)
        data[key] = value
        _data = data
    }

    private func _add(value: Any?) {
        if (_data == nil) { _data = Array<Any?>() }
        if (!(_data is Array<Any?>)) {
            _data = Array<Any?>()
        }

        var data : Array<Any?> = (_data as! Array<Any?>)
        data.append(value)
        _data = data
    }

    public init() {
        _data = Dictionary<String, Any?>()
    }

    public init(isArray: Bool) {
        if (isArray) {
            _data = Array<Any?>()
        }
        else {
            _data = Dictionary<String, Any?>()
        }
    }
    
    public func hasKey(key: String) -> Bool {
        if let data : Dictionary<String, Any?> = _data as? Dictionary<String, Any?> {
            let dataKeys : Dictionary<String, Any?>.Keys = data.keys
            return dataKeys.contains(key)
        }
        
        return false
    }
    
    public func keys() -> Dictionary<String, Any?>.Keys? {
        if let data : Dictionary<String, Any?> = _data as? Dictionary<String, Any?> {
            let dataKeys : Dictionary<String, Any?>.Keys = data.keys
            return dataKeys
        }
        
        return nil
    }

    public func getJson(key: String) -> Json! {
        return Json(data: (_data as? Dictionary<String, Any?>?)??[key] as Any?)
    }

    public func getJson(index: Int) -> Json! {
        return Json(data: (_data as? Array<Any?>?)??[index])
    }

    public func getString(key: String) -> String! {
        return (_data as? Dictionary<String, Any?>?)??[key] as? String
    }

    public func getString(index: Int) -> String! {
        return (_data as? Array<Any?>?)??[index] as? String
    }

    public func getInteger(key: String) -> Int! {
        return (_data as? Dictionary<String, Any?>?)??[key] as? Int
    }

    public func getInteger(index: Int) -> Int! {
        return (_data as? Array<Any?>?)??[index] as? Int
    }

    public func getBoolean(key: String) -> Bool! {
        return (_data as? Dictionary<String, Any?>?)??[key] as? Bool
    }

    public func getBoolean(index: Int) -> Bool! {
        return (_data as? Array<Any?>?)??[index] as? Bool
    }

    public func getDouble(key: String) -> Double! {
        return (_data as? Dictionary<String, Any?>?)??[key] as? Double
    }

    public func getDouble(index: Int) -> Double! {
        return (_data as? Array<Any?>?)??[index] as? Double
    }

    public func putString(key: String, value: String?) {
        _put(key: key, value: value)
    }

    public func putInteger(key: String, value: Int?) {
        _put(key: key, value: value)
    }

    public func putDouble(key: String, value: Double?) {
        _put(key: key, value: value)
    }

    public func putBoolean(key: String, value: Bool?) {
        _put(key: key, value: value)
    }

    public func putJson(key: String, value: Json?) {
        _put(key: key, value: value?._data)
    }

    public func addString(value: String?) {
        _add(value: value)
    }

    public func addInteger(value: Int?) {
        _add(value: value)
    }

    public func addDouble(value: Double?) {
        _add(value: value)
    }

    public func addBoolean(value: Bool?) {
        _add(value: value)
    }

    public func addJson(value: Json?) {
        _add(value: value?._data)
    }

    public func clear() {
        if (_data is Array<Any>?) {
            _data = Array<Any>()
        }
        else {
            _data = Dictionary<String, Any>()
        }
    }

    public func toString() -> String {
        let data : Data = try! JSONSerialization.data(withJSONObject: _data!, options: [])
        return String(data: data, encoding: String.Encoding.utf8)!
    }
    
    public func length() -> Int {
        let dataAsArray : Array<Any?>! = (_data as? Array<Any?>?)!
        return dataAsArray.count
    }
}
