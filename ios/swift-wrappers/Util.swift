import Foundation

public class HexUtil {
    public static func toHexString(_ bytes: Data) -> String? {
        let iosByteArray : IOSByteArray = IOSByteArray(nsData: bytes)
        return ComSoftwareverdeUtilHexUtil.toHexString(with: iosByteArray)
    }
    
    public static func toHexString(_ bytes: IOSByteArray) -> String {
        return ComSoftwareverdeUtilHexUtil.toHexString(with: bytes)
    }

    public static func hexStringToByteArray(_ hexString : String) -> Data? {
        let iosByteArray : IOSByteArray = ComSoftwareverdeUtilHexUtil.hexStringToByteArray(with: hexString)
        return iosByteArray.toNSData()
    }
    
    public static func hexStringToByteArray(_ hexString : String) -> IOSByteArray {
        let iosByteArray : IOSByteArray = ComSoftwareverdeUtilHexUtil.hexStringToByteArray(with: hexString)
        return iosByteArray
    }
}

public class JavaUtilListUtil {
    public static func toJavaUtilList(array: Array<Any>) -> JavaUtilList {
        let javaUtilList : JavaUtilList = JavaUtilArrayList()
        for item in array {
            javaUtilList.add(withId: item)
        }
        
        return javaUtilList
    }
    
    public static func toSwiftArray(javaUtilList: JavaUtilList) -> Array<Any> {
        var swiftArray : Array<Any> = Array()
        let javaUtilIterator : JavaUtilIterator  = javaUtilList.iterator()
        while javaUtilIterator.hasNext() {
            swiftArray.append(javaUtilIterator.next() as Any)
        }
        
        return swiftArray
    }
}

public class Base64Util {
    public static func toBase64String(buffer: IOSByteArray) -> String {
        return ComSoftwareverdeUtilBase64Util.toBase64String(with: buffer)
    }
    
    public static func base64StringToByteArray(string: String) -> IOSByteArray {
        return ComSoftwareverdeUtilBase64Util.base64StringToByteArray(with: string)
    }
}

public class Util {
    public static func areEqual<T, S>(a: T, b: S) -> Bool {
        return ComSoftwareverdeUtilUtil.areEqual(withId: a, withId: b)!.booleanValue()
    }
}

