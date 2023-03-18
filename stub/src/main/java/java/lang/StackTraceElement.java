package java.lang;

import java.io.Serializable;

@SuppressWarnings("all")
public class StackTraceElement implements Serializable {
    public StackTraceElement(
            String declaringClass, String methodName, String fileName, int lineNumber) {
        throw new RuntimeException("Stub!");
    }

    public String getFileName() {
        throw new RuntimeException("Stub!");
    }

    public int getLineNumber() {
        throw new RuntimeException("Stub!");
    }

    public String getClassName() {
        throw new RuntimeException("Stub!");
    }

    public String getMethodName() {
        throw new RuntimeException("Stub!");
    }

    public boolean isNativeMethod() {
        throw new RuntimeException("Stub!");
    }
}
