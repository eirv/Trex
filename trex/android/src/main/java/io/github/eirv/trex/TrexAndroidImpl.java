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

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR2;
import static android.os.Build.VERSION_CODES.KITKAT;
import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static android.os.Build.VERSION_CODES.M;
import static android.os.Build.VERSION_CODES.N;
import static android.os.Build.VERSION_CODES.O;

import android.app.ActivityThread;
import android.app.Application;
import android.content.pm.ApplicationInfo;

import libcore.io.Memory;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.ArtMethod;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * @author Eirv
 * @date 2023/1/8 12:57
 */
class TrexAndroidImpl implements Platform {
    static final int OFF_FIELD_ART_METHOD = 24;
    static final int OFF_FIELD_DECLARING_CLASS = 12;
    static final int OFF_FIELD_ACCESS_FLAGS = 0;
    static final int OFF_ACCESS_FLAGS = 4;
    static int OFF_DEX_METHOD_INDEX;

    static boolean sArtVM;
    static Field sBacktraceField;
    static Field sStackTraceField;
    static Field sArtMethodField;
    static Field sDeclaringClassField;
    static Field sDeclaringClassOfOverriddenMethodField;
    static Field sAccessFlagsField;
    static Field sDexMethodIndexField;

    static boolean sNativeLockEnabled;
    static boolean sStackTraceProxyEnabled = true;
    static ClassLoader sAppClassLoader;
    static String sSelfModuleName;

    private static final int ACC_CONSTRUCTOR = 0x10000;

    public static void initJava() {
        try {
            Class<?> executableClass = Method.class.getSuperclass();
            assert executableClass != null;
            sArtVM =
                    SDK_INT >= LOLLIPOP
                            || (SDK_INT >= KITKAT && executableClass != AccessibleObject.class);
            sBacktraceField =
                    Utils.findField(Throwable.class, SDK_INT >= N ? "backtrace" : "stackState");
            sStackTraceField = Utils.findFieldIfExists(Throwable.class, "stackTrace");

            if (sArtVM) {
                sArtMethodField = Utils.findFieldIfExists(executableClass, "artMethod");
                sDeclaringClassField =
                        Utils.findFieldIfExists(
                                SDK_INT >= M ? executableClass : ArtMethod.class, "declaringClass");
                if (SDK_INT >= M) {
                    sAccessFlagsField = Utils.findFieldIfExists(executableClass, "accessFlags");
                    if (SDK_INT < O) {
                        OFF_DEX_METHOD_INDEX = getDexMethodIndexOffset(executableClass);
                        sDeclaringClassOfOverriddenMethodField =
                                Utils.findField(
                                        executableClass, "declaringClassOfOverriddenMethod");
                        sDexMethodIndexField = Utils.findField(executableClass, "dexMethodIndex");
                    }
                } else assert sDeclaringClassField != null;
            }
        } catch (Exception e) {
            throw new UnsupportedOperationException(
                    "An exception occurred during initialization", e);
        }
    }

    public static void initXposed() {
        XposedBridge.getXposedVersion();
        initJava();

        XposedHelpers.findAndHookMethod(
                Throwable.class,
                SDK_INT >= N ? "getOurStackTrace" : "getInternalStackTrace",
                new XC_MethodHook() {
                    private static final String KEY_BACKTRACE = "backtrace";

                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        param.setObjectExtra(
                                KEY_BACKTRACE, getBackTrace((Throwable) param.thisObject));
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        Object backTrace = param.getObjectExtra(KEY_BACKTRACE);
                        if (backTrace != null) {
                            setBackTrace((Throwable) param.thisObject, backTrace);
                        }
                    }
                });

        XposedHelpers.findAndHookMethod(
                Throwable.class,
                "nativeGetStackTrace",
                Object.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (!isStackTraceProxyEnabled()) return;
                        param.setResult(getProxyStackTrace((Throwable) param.thisObject, null));
                    }
                });
    }

    public static int lockAll(Throwable throwable) {
        Utils.requireNonNull(throwable, "throwable");
        Throwable[] throwables = findAllThrowable(throwable, true);
        int len = throwables.length;
        int done = 0;
        for (Throwable t : throwables) {
            if (lock(t)) done++;
        }
        return done == len ? 0 : done == 0 ? -1 : -2;
    }

    private static Throwable[] findAllThrowable(Throwable throwable, boolean includeSuppressed) {
        Set<Throwable> throwables = Utils.newIdentityHashSet();
        List<Throwable> list = new LinkedList<>();
        list.add(throwable);
        while (!list.isEmpty()) {
            Throwable t = list.remove(0);
            if (throwables.contains(t)) continue;
            throwables.add(t);
            Throwable cause = t.getCause();
            if (cause != null) {
                list.add(cause);
            }
            if (includeSuppressed) {
                list.addAll(Arrays.asList(t.getSuppressed()));
            }
        }
        return throwables.toArray(new Throwable[throwables.size()]);
    }

    public static boolean lock(Throwable throwable) {
        Utils.requireNonNull(throwable, "throwable");
        Object backTrace = getBackTrace(throwable);
        if (backTrace == null) return false;
        if (backTrace instanceof FakeBackTrace) return true;
        throwable.getStackTrace();
        setBackTrace(throwable, backTrace);
        return true;
    }

    public static boolean isNativeLockEnabled() {
        return sNativeLockEnabled;
    }

    public static void setNativeLockEnabled(boolean nativeLockEnabled) {
        sNativeLockEnabled = nativeLockEnabled;
    }

    public static String dumpThrowableNative(Throwable throwable) {
        if (throwable == null || !sArtVM) return "";

        Throwable[] throwables = findAllThrowable(throwable, false);
        int len = throwables.length;
        Object[] backTraces = new Object[len];

        for (int i = 0; len > i; i++) {
            Throwable t = throwables[i];
            Object backTrace = getBackTrace(t);
            if (backTrace == null) continue;
            backTraces[i] = backTrace;
            FakeBackTrace fakeBackTrace = FakeBackTrace.from(backTrace);
            if (fakeBackTrace != null) {
                setBackTrace(t, fakeBackTrace.getBackTrace());
            }
        }

        String result = TrexAndroid.nDumpThrowable(throwable);

        for (int i = 0; len > i; i++) {
            Object backTrace = backTraces[i];
            if (backTrace == null) continue;
            Throwable t = throwables[i];
            setBackTrace(t, backTrace);
        }

        return result;
    }

    public static void hookNativeFunc() {
        TrexAndroid.nHookNativeFunc();
    }

    public static boolean isStackTraceProxyEnabled() {
        return sStackTraceProxyEnabled;
    }

    public static void setStackTraceProxyEnabled(boolean stackTraceProxyEnabled) {
        sStackTraceProxyEnabled = stackTraceProxyEnabled;
    }

    public static boolean replaceToProxyStackTrace(Throwable throwable, TrexOption option) {
        Utils.requireNonNull(throwable, "throwable");
        setStackTraceProxyEnabled(true);
        StackTraceElement[] originStackTraces = throwable.getStackTrace();
        if (originStackTraces[0] instanceof StackFrameProxyImpl) return true;
        StackTraceElement[] proxyStackTraces = getProxyStackTrace(throwable, option);
        if (proxyStackTraces == null) return false;
        if (sStackTraceField != null) {
            Utils.setField(sStackTraceField, throwable, proxyStackTraces);
        } else {
            throwable.setStackTrace(proxyStackTraces);
        }
        return true;
    }

    public static int replaceAllToProxyStackTrace(Throwable throwable, TrexOption option) {
        Utils.requireNonNull(throwable, "throwable");
        Throwable[] throwables = findAllThrowable(throwable, true);
        int len = throwables.length;
        int done = 0;
        for (Throwable t : throwables) {
            if (replaceToProxyStackTrace(t, option)) done++;
        }
        return done == len ? 0 : done == 0 ? -1 : -2;
    }

    @SuppressWarnings("ConstantConditions")
    public static StackTraceElement[] getProxyStackTrace(Throwable throwable, TrexOption option) {
        Utils.requireNonNull(throwable, "throwable");
        option = Utils.select(option, TrexOption.getDefault());

        StackFrame[] stackFrames = Trex.getStackFrame(throwable, option);
        if (stackFrames == null) return null;
        int len = stackFrames.length;
        StackTraceElement[] result = new StackTraceElement[len];

        if (stackFrames[0] instanceof StackFrameProxyImpl) {
            for (int i = 0; len > i; i++) {
                result[i] = (StackFrameProxyImpl) stackFrames[i];
            }
        } else {
            for (int i = 0; len > i; i++) {
                result[i] = new StackFrameProxyImpl((StackFrameImpl) stackFrames[i]);
            }
        }
        return result;
    }

    public static String getSourceFile(Class<?> clazz) {
        if (!TrexAndroid.isNativeInitialized()) return null;
        Utils.requireNonNull(clazz, "clazz");
        return TrexAndroid.nGetSourceFile(clazz);
    }

    public static void addModuleName(String name, Class<?> clazz) {
        Utils.requireNonNull(name, "name");
        Utils.requireNonNull(clazz, "clazz");
        ModuleNameFinder.addModuleName(name, clazz);
    }

    public static void addModuleName(String name, String[] rules) {
        Utils.requireNonNull(name, "name");
        Utils.requireNonNull(rules, "rules");
        ModuleNameFinder.addModuleName(name, rules);
    }

    public static void addModuleName(String name, ClassLoader classLoader, String[] rules) {
        Utils.requireNonNull(name, "name");
        Utils.requireNonNull(classLoader, "classLoader");
        Utils.requireNonNull(rules, "rules");
        ModuleNameFinder.addModuleName(name, classLoader, rules);
    }

    public static void removeModuleName(String name) {
        Utils.requireNonNull(name, "name");
        ModuleNameFinder.removeModuleName(name);
    }

    public static void removeModuleName(String name, ClassLoader classLoader) {
        Utils.requireNonNull(name, "name");
        Utils.requireNonNull(classLoader, "classLoader");
        ModuleNameFinder.removeModuleName(name, classLoader);
    }

    public static String getModuleName(Class<?> clazz) {
        Utils.requireNonNull(clazz, "clazz");
        if (clazz.isArray() || clazz.isPrimitive()) {
            return null;
        }
        return getClassModuleNameInternal(clazz);
    }

    static String getClassModuleNameInternal(Class<?> clazz) {
        String moduleName = findModuleName(clazz);
        if (moduleName == null) {
            ClassLoader classLoader = clazz.getClassLoader();
            if (classLoader == getAppClassLoader()) {
                moduleName = getSelfModuleName();
            }
        }
        return moduleName;
    }

    private static ClassLoader getAppClassLoader() {
        if (sAppClassLoader == null) {
            Application application = ActivityThread.currentApplication();
            if (application != null) {
                ClassLoader classLoader = application.getClassLoader();
                if (classLoader != null) {
                    sAppClassLoader = classLoader;
                    return classLoader;
                }
            }
            return TrexAndroidImpl.class.getClassLoader();
        }
        return sAppClassLoader;
    }

    private static String getSelfModuleName() {
        String selfModuleName = sSelfModuleName;
        if (selfModuleName == null) {
            if (SDK_INT >= JELLY_BEAN_MR2) {
                selfModuleName = ActivityThread.currentPackageName();
            } else {
                try {
                    ActivityThread activityThread = ActivityThread.currentActivityThread();
                    if (activityThread == null) return null;
                    Field mBoundApplicationField =
                            Utils.findField(ActivityThread.class, "mBoundApplication");
                    Object mBoundApplication =
                            Utils.getField(mBoundApplicationField, activityThread);
                    if (mBoundApplication == null) return null;
                    Field appInfoField = Utils.findField(mBoundApplication.getClass(), "appInfo");
                    ApplicationInfo appInfo =
                            (ApplicationInfo) Utils.getField(appInfoField, mBoundApplication);
                    if (appInfo == null) return null;
                    selfModuleName = appInfo.packageName;
                } catch (NoSuchFieldException ignored) {
                }
            }

            if (selfModuleName != null) {
                String[] prefixes = {"com.", "cn.", "org.", "io.github.", "io."};
                for (String prefix : prefixes) {
                    if (!selfModuleName.startsWith(prefix)) continue;
                    selfModuleName = selfModuleName.substring(prefix.length());
                    int index = selfModuleName.indexOf('.');
                    if (index != -1) {
                        selfModuleName = selfModuleName.substring(index + 1);
                    }
                    break;
                }
                sSelfModuleName = selfModuleName;
            }
        }
        return selfModuleName;
    }

    public static void setSelfModuleName(String selfModuleName) {
        sSelfModuleName = selfModuleName;
    }

    static String findModuleName(Class<?> clazz) {
        if (Proxy.isProxyClass(clazz)) {
            return "jdk.proxy";
        }
        ClassLoader classLoader = clazz.getClassLoader();
        String moduleName = null;
        if (classLoader == null
                || classLoader == Utils.BOOT_CLASS_LOADER
                || classLoader == Utils.SYSTEM_CLASS_LOADER) {
            String name = clazz.getName();
            moduleName = ModuleNameFinder.find(name);
        }
        if (moduleName == null) {
            moduleName = ModuleNameFinder.findCustom(clazz);
        }
        return moduleName;
    }

    static Object getBackTrace(Throwable throwable) {
        return Utils.getField(sBacktraceField, throwable);
    }

    static void setBackTrace(Throwable throwable, Object backTrace) {
        Utils.setField(sBacktraceField, throwable, backTrace);
    }

    static Constructor<?> getStubConstructor() {
        try {
            return Compiler.class.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            // this will never happen
            throw new NoSuchMethodError(e.getMessage());
        }
    }

    static Method getStubMethod() {
        try {
            return Compiler.class.getDeclaredMethod("enable");
        } catch (NoSuchMethodException e) {
            // this will never happen
            throw new NoSuchMethodError(e.getMessage());
        }
    }

    static int getInt(long addr) {
        return SDK_INT >= N ? Utils.UNSAFE.getInt(addr) : Memory.peekInt(addr, false);
    }

    static StackFrame newStackFrame(
            StackTraceElement stackTrace,
            Class<?> clazz,
            String descriptor,
            String moduleName,
            int dexPc,
            Object vmMethod,
            boolean proxy) {

        String className = stackTrace.getClassName();
        String methodName = stackTrace.getMethodName();
        String fileName = stackTrace.getFileName();
        int lineNumber = stackTrace.getLineNumber();

        if (SDK_INT >= O && fileName == null) {
            fileName = getSourceFile(clazz);
        }

        if (proxy) {
            return new StackFrameProxyImpl(
                    descriptor,
                    className,
                    methodName,
                    fileName,
                    lineNumber,
                    moduleName,
                    dexPc,
                    vmMethod);
        } else {
            return new StackFrameImpl(
                    descriptor,
                    className,
                    methodName,
                    fileName,
                    lineNumber,
                    moduleName,
                    dexPc,
                    vmMethod);
        }
    }

    @SuppressWarnings("ConstantConditions")
    static StackTraceElement[] proxy(Object backTrace, StackTraceElement[] stackTraces) {
        try {
            if (isNativeLockEnabled()) {
                int len = stackTraces.length;
                if (len > 0) {
                    stackTraces[0] = new StackTraceElementExt(stackTraces[0], backTrace);
                }
            }
        } catch (Throwable ignored) {
        }
        return stackTraces;
    }

    private static int getDexMethodIndexOffset(Class<?> executableClass) {
        Field dexMethodIndexField = Utils.findFieldIfExists(executableClass, "dexMethodIndex");
        if (dexMethodIndexField != null) {
            Method method = getStubMethod();
            try {
                int dexMethodIndex = dexMethodIndexField.getInt(method);
                long artMethod = sArtMethodField.getLong(method);
                for (int off = 0; 20 >= off; off += 2) {
                    int current = getInt(artMethod + off);
                    if (current == dexMethodIndex) return off;
                }
            } catch (IllegalAccessException e) {
                // this will never happen
                throw Utils.wrap(e);
            }
        }
        return 12;
    }

    @Override
    public boolean isInitialized0() {
        return TrexAndroid.isInitialized();
    }

    @Override
    public Object getBackTrace0(Throwable throwable) {
        return getBackTrace(throwable);
    }

    @Override
    public void setBackTrace0(Throwable throwable, Object backTrace) {
        setBackTrace(throwable, backTrace);
    }

    @Override
    public void setDepth0(Throwable throwable, int depth) {}

    @Override
    public Object getBackTrace0(StackTraceElement stackTrace) {
        if ((Object) stackTrace instanceof Callable<?>) {
            Callable<?> callable = (Callable<?>) (Object) stackTrace;
            try {
                return callable.call();
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    @Override
    public BackTraceParser newBackTraceParser0(
            StackTraceElement[] stackTraces,
            Object backTrace,
            TrexOption option,
            int stackFrameOptionHashCode) {

        if (SDK_INT >= M) {
            return new BackTraceParserA23(stackTraces, backTrace, option, stackFrameOptionHashCode);
        } else if (sArtVM) {
            return new BackTraceParserA19Art(
                    stackTraces, backTrace, option, stackFrameOptionHashCode);
        } else {
            return new BackTraceParserA14(stackTraces, backTrace, option, stackFrameOptionHashCode);
        }
    }

    @Override
    public Class<?> getDeclaringClass0(Object vmMethod) {
        if (SDK_INT >= M) {
            ArtMethodM artMethodM = (ArtMethodM) vmMethod;
            return artMethodM.declaringClass;
        } else if (sArtVM) {
            ArtMethod artMethod = (ArtMethod) vmMethod;
            return (Class<?>) Utils.getField(sDeclaringClassField, artMethod);
        } else {
            return null;
        }
    }

    @Override
    public Member getExecutable0(Object vmMethod) {
        if (sArtVM) {
            boolean atLeastM = SDK_INT >= M;

            int accessFlags;
            long artMethod;
            Class<?> declaringClass;

            if (atLeastM) {
                ArtMethodM artMethodM = (ArtMethodM) vmMethod;
                artMethod = artMethodM.artMethod;
                accessFlags = getInt(artMethod + OFF_ACCESS_FLAGS);
                declaringClass = artMethodM.declaringClass;
            } else {
                accessFlags = ((ArtMethod) vmMethod).getAccessFlags();
                artMethod = 0;
                declaringClass = null;
            }

            Member stub;
            if ((accessFlags & ACC_CONSTRUCTOR) != 0) {
                stub = getStubConstructor();
            } else {
                stub = getStubMethod();
            }

            if (atLeastM) {
                BackTraceParserA23.resetMethod(stub, declaringClass, artMethod);
            } else {
                BackTraceParserA19Art.resetMethod(stub, vmMethod);
            }

            return stub;
        } else {
            if (vmMethod instanceof Integer) {
                return BackTraceParserA14.getDvmMethod((int) vmMethod);
            } else {
                return (Member) vmMethod;
            }
        }
    }

    @Override
    public String getModuleName0(Class<?> clazz) {
        return getClassModuleNameInternal(clazz);
    }

    @Override
    public List<Class<?>> getCallerClasses0(Object backTrace) {
        if (SDK_INT >= M) {
            return BackTraceParserA23.getCallerClasses(backTrace);
        } else if (sArtVM) {
            return BackTraceParserA19Art.getCallerClasses(backTrace);
        } else {
            return BackTraceParserA14.getCallerClasses(backTrace);
        }
    }

    @Override
    public StackTraceElement newStackTraceElement0(
            String declaringClass,
            String methodName,
            String fileName,
            int lineNumber,
            String classLoaderName,
            String moduleName,
            String moduleVersion) {
        return null;
    }

    @Override
    public void printStackTraceInfoPrefix0(
            TrexPrinter p, StackTraceElement stackTrace, TrexOption option) {}

    @Override
    public TrexOption cloneOption0(TrexOption option) {
        return option instanceof TrexAndroidOption
                ? option.clone()
                : TrexAndroidOption.clone(option);
    }
}
