package dalvik.system;

@SuppressWarnings("all")
public final class VMRuntime {
    public static VMRuntime getRuntime() {
        throw new RuntimeException("Stub!");
    }

    public native void setHiddenApiExemptions(String... signaturePrefixes);
}