-if class io.github.eirv.trex.TrexAndroid {
    initNative();
}
-keepclassmembers class io.github.eirv.trex.TrexAndroid {
    native *;
}

-if class io.github.eirv.trex.TrexAndroid {
    hookNativeFunc(...);
}
-keepclassmembers class io.github.eirv.trex.TrexAndroid {
    proxy(...);
}

-keepclassmembers,allowshrinking,allowobfuscation class io.github.eirv.trex.TrexAndroid {
    public static <methods>;
}


-keepclassmembers,allowshrinking,allowobfuscation class io.github.eirv.trex.TrexAndroidImpl {
    public static <methods>;
    proxy(...);
}
