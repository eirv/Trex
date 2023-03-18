-repackageclasses io.github.eirv.trex
-overloadaggressively
-keepattributes SourceFile,LineNumberTable

-keep public class io.github.eirv.trex.* {
    public *;
    protected *;
}
-keep public class io.github.eirv.trex.TrexAndroid {
    native *;
    proxy(...);
}
-keepclassmembers,allowshrinking,allowobfuscation class io.github.eirv.trex.TrexAndroidImpl {
    public *;
    proxy(...);
}

-assumenosideeffects class io.github.eirv.trex.Trex {
    boolean hasAndroidClass() return true;
    boolean isAndroid() return true;
}
