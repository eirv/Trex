package sun.misc;

@SuppressWarnings("all")
public final class Unsafe {
    private static Unsafe theUnsafe;
    private static Unsafe THE_ONE;

    public static Unsafe getUnsafe() {
        throw new RuntimeException("Stub!");
    }

    public native int getInt(Object obj, long offset);

    public native void putInt(Object obj, long offset, int newValue);

    public native void putLong(Object obj, long offset, long newValue);

    public native void putObject(Object obj, long offset, Object newValue);

    public native byte getByte(Object obj, long offset);

    public native void putByte(Object obj, long offset, byte newValue);

    public native void putBoolean(Object obj, long offset, boolean newValue);

    public native int getInt(long address);

    // HiddenApi.java

    public native long getLong(Object obj, long offset);

    public native int addressSize();

    public native void putInt(long address, int x);

    public native long getLong(long address);
}
