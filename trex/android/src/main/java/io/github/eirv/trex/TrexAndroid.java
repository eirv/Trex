/*
 * Copyright (C) 2023 Wanli Zhu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.eirv.trex;

import android.annotation.SuppressLint;
import android.os.Build;
import android.util.Log;

import dalvik.annotation.optimization.FastNative;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;

/**
 * @author Eirv
 * @date 2023/1/8 12:57
 */
public final class TrexAndroid {
    private static final int FLAG_INIT_OK = 1 << 0;
    private static final int FLAG_INIT_JAVA_OK = 1 << 3;
    private static final int FLAG_INIT_NATIVE_OK = 1 << 4;
    private static final int FLAG_INIT_XPOSED_OK = 1 << 5;

    private static final int OFF_ACCESS_FLAGS = 64;

    static int sInitFlags;
    static String sNativeLibraryPath;

    static {
        try {
            Class<?> clazz = StackTraceElement.class;
            try {
                Field accessFlagsField = Utils.findField(Class.class, "accessFlags");
                int accessFlags = accessFlagsField.getInt(clazz);
                accessFlagsField.setInt(clazz, accessFlags & ~Modifier.FINAL);
            } catch (Exception e) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    int accessFlags = Utils.UNSAFE.getInt(clazz, OFF_ACCESS_FLAGS);
                    Utils.UNSAFE.putInt(clazz, OFF_ACCESS_FLAGS, accessFlags & ~Modifier.FINAL);
                } else {
                    Log.w("Trex", "Failed to remove final: " + clazz, e);
                }
            }
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private TrexAndroid() {}

    public static void init() {
        try {
            initNative();
        } catch (UnsatisfiedLinkError ignored) {
        }
        initJava();
    }

    public static void initJava() {
        if ((sInitFlags & FLAG_INIT_JAVA_OK) != 0) return;

        TrexAndroidImpl.initJava();
        if (Utils.ANDROID) {
            Trex.setPlatform(new TrexAndroidImpl());
            TrexOption.setDefault(new TrexAndroidOption());
        }

        sInitFlags |= FLAG_INIT_OK | FLAG_INIT_JAVA_OK;
    }

    @SuppressLint("UnsafeDynamicallyLoadedCode")
    public static void initNative() {
        if ((sInitFlags & FLAG_INIT_NATIVE_OK) != 0) return;

        if (sNativeLibraryPath != null) {
            System.load(sNativeLibraryPath);
            sNativeLibraryPath = null;
        } else {
            System.loadLibrary("trex");
        }

        sInitFlags |= FLAG_INIT_OK | FLAG_INIT_NATIVE_OK;
    }

    public static void initXposed() {
        if ((sInitFlags & FLAG_INIT_XPOSED_OK) != 0) return;

        TrexAndroidImpl.initXposed();

        sInitFlags |= FLAG_INIT_XPOSED_OK;
    }

    static boolean isNativeInitialized() {
        return (sInitFlags & FLAG_INIT_NATIVE_OK) != 0;
    }

    static boolean isInitialized() {
        return (sInitFlags & FLAG_INIT_OK) != 0;
    }

    public static void setNativeLibraryPath(String nativeLibraryPath) {
        sNativeLibraryPath = nativeLibraryPath;
    }

    public static int lockAll(Throwable throwable) {
        return TrexAndroidImpl.lockAll(throwable);
    }

    public static boolean lock(Throwable throwable) {
        return TrexAndroidImpl.lock(throwable);
    }

    public static boolean isNativeLockEnabled() {
        return TrexAndroidImpl.isNativeLockEnabled();
    }

    public static void setNativeLockEnabled(boolean nativeLockEnabled) {
        TrexAndroidImpl.setNativeLockEnabled(nativeLockEnabled);
    }

    public static String dumpThrowableNative(Throwable throwable) {
        return TrexAndroidImpl.dumpThrowableNative(throwable);
    }

    public static void hookNativeFunc() {
        TrexAndroidImpl.hookNativeFunc();
    }

    public static boolean isStackTraceProxyEnabled() {
        return TrexAndroidImpl.isStackTraceProxyEnabled();
    }

    public static void setStackTraceProxyEnabled(boolean stackTraceProxyEnabled) {
        TrexAndroidImpl.setStackTraceProxyEnabled(stackTraceProxyEnabled);
    }

    public static boolean replaceToProxyStackTrace(Throwable throwable) {
        return TrexAndroidImpl.replaceToProxyStackTrace(throwable, null);
    }

    public static boolean replaceToProxyStackTrace(Throwable throwable, TrexOption option) {
        return TrexAndroidImpl.replaceToProxyStackTrace(throwable, option);
    }

    public static int replaceAllToProxyStackTrace(Throwable throwable) {
        return TrexAndroidImpl.replaceAllToProxyStackTrace(throwable, null);
    }

    public static int replaceAllToProxyStackTrace(Throwable throwable, TrexOption option) {
        return TrexAndroidImpl.replaceAllToProxyStackTrace(throwable, option);
    }

    public static StackTraceElement[] getProxyStackTrace(Throwable throwable) {
        return TrexAndroidImpl.getProxyStackTrace(throwable, null);
    }

    public static StackTraceElement[] getProxyStackTrace(Throwable throwable, TrexOption option) {
        return TrexAndroidImpl.getProxyStackTrace(throwable, option);
    }

    public static String getSourceFile(Class<?> clazz) {
        return TrexAndroidImpl.getSourceFile(clazz);
    }

    public static void setSelfModuleName(String selfModuleName) {
        TrexAndroidImpl.setSelfModuleName(selfModuleName);
    }

    public static void addModuleName(String name, Class<?> clazz) {
        TrexAndroidImpl.addModuleName(name, clazz);
    }

    public static void addModuleName(String name, String... rules) {
        TrexAndroidImpl.addModuleName(name, rules);
    }

    public static void addModuleName(String name, ClassLoader classLoader, String... rules) {
        TrexAndroidImpl.addModuleName(name, classLoader, rules);
    }

    public static void removeModuleName(String name) {
        TrexAndroidImpl.removeModuleName(name);
    }

    public static void removeModuleName(String name, ClassLoader classLoader) {
        TrexAndroidImpl.removeModuleName(name, classLoader);
    }

    public static String getModuleName(Class<?> clazz) {
        return TrexAndroidImpl.getModuleName(clazz);
    }

    @SuppressWarnings("unused")
    private static StackTraceElement[] proxy(
            Object javaStackState, StackTraceElement[] stackTraces) {
        return TrexAndroidImpl.proxy(javaStackState, stackTraces);
    }

    @FastNative
    static native String nDumpThrowable(Throwable throwable);

    @FastNative
    static native void nHookNativeFunc();

    @FastNative
    static native String nGetSourceFile(Class<?> clazz);

    static native String nGetDvmDescriptor(
            int dvmMethod, boolean bootMethodTypeVisible, boolean synthesizedMethodTypeVisible);

    static native Member nGetDvmMethod(int dvmMethod);
}
