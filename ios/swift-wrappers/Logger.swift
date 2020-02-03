import Foundation

public class Logger {
    public static func configureLogFactory() {
        // configure slf4j-simple
        JavaLangSystem.setPropertyWith(OrgSlf4jImplSimpleLogger_DATE_TIME_FORMAT_KEY, with: "HH:mm:ss:SSS")
        JavaLangSystem.setPropertyWith(OrgSlf4jImplSimpleLogger_SHOW_DATE_TIME_KEY, with: "true")
        JavaLangSystem.setPropertyWith(OrgSlf4jImplSimpleLogger_SHOW_LOG_NAME_KEY, with: "false")
        JavaLangSystem.setPropertyWith(OrgSlf4jImplSimpleLogger_SHOW_SHORT_LOG_NAME_KEY, with: "true")
        JavaLangSystem.setPropertyWith(OrgSlf4jImplSimpleLogger_DEFAULT_LOG_LEVEL_KEY, with: "TRACE")
        // configure softwareverde logger
        ComSoftwareverdeLoggingLogger_initialize()
        ComSoftwareverdeLoggingLogger.setLogFactoryWith(ComSoftwareverdeLoggingSlf4jSlf4jLogFactory())
        ComSoftwareverdeLoggingLogLevel_initialize()
        ComSoftwareverdeLoggingLogger_set_DEFAULT_LOG_LEVEL(ComSoftwareverdeLoggingLogLevel_get_DEBUG())
        ComSoftwareverdeLoggingLogger.setLogLevelWith(ComSoftwareverdeAsyncLockIndexLock_class_(), with: ComSoftwareverdeLoggingLogLevel_get_OFF())
        ComSoftwareverdeLoggingLogger.setLogLevelWith(ComSoftwareverdeLoggingStackTraceManager_class_(), with: ComSoftwareverdeLoggingLogLevel_get_OFF())
    }
}
