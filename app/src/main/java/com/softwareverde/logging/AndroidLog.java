package com.softwareverde.logging;

import com.softwareverde.logging.log.BufferedSystemWriter;
import com.softwareverde.logging.log.SystemLog;

public class AndroidLog extends LineNumberAnnotatedLog {
    protected static final Object INSTANCE_MUTEX = new Object();
    protected static volatile AndroidLog INSTANCE = null;
    public static AndroidLog getInstance() {
        if (INSTANCE == null) {
            synchronized (INSTANCE_MUTEX) {
                if (INSTANCE == null) {
                    INSTANCE = new AndroidLog(
                        SystemLog.wrapSystemStream(System.out),
                        SystemLog.wrapSystemStream(System.err)
                    );
                }
            }
        }

        return INSTANCE;
    }

    protected static volatile AndroidLog BUFFERED_INSTANCE = null;
    public static AndroidLog getBufferedInstance() {
        if (BUFFERED_INSTANCE == null) {
            synchronized (INSTANCE_MUTEX) {
                if (BUFFERED_INSTANCE == null) {
                    BUFFERED_INSTANCE = new AndroidLog(
                            new BufferedSystemWriter(BufferedSystemWriter.Type.SYSTEM_OUT),
                            new BufferedSystemWriter(BufferedSystemWriter.Type.SYSTEM_ERR)
                    );
                }
            }
        }

        return BUFFERED_INSTANCE;
    }

    protected Integer _getCallingDepth() {
        final Exception exception = new Exception();
        final StackTraceElement[] stackTraceElements = exception.getStackTrace();

        for (int i = 1; i < stackTraceElements.length; ++i) {
            final StackTraceElement stackTraceElement = stackTraceElements[i];
            final String callingClass = stackTraceElement.getClassName();
            if (callingClass != null) {
                if (! callingClass.startsWith(StackTraceManager.LOGGING_PACKAGE_NAME)) {
                    return (i - 1);
                }
            }
        }

        return 2;
    }

    protected AndroidLog(final Writer outWriter, final Writer errWriter) {
        super(outWriter, errWriter);
    }

    @Override
    protected String _getClassAnnotation(final Class<?> callingClass) {
        final Exception exception = new Exception();

        final int backtraceIndex = _getCallingDepth();

        final StackTraceElement[] stackTraceElements = exception.getStackTrace();
        if (backtraceIndex >= stackTraceElements.length) {
            return super._getClassAnnotation(callingClass);
        }

        final StackTraceElement stackTraceElement = stackTraceElements[backtraceIndex];
        return stackTraceElement.getFileName() + ":" + stackTraceElement.getLineNumber();
    }
}
