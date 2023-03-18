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

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.util.List;

/**
 * @author Eirv
 * @date 2023/1/8 12:57
 */
public final class TrexJvm implements Platform {
    static final int JAVA_VERSION;
    static final int OFF_FIELD_SLOT;
    static final int OFF_FIELD_BACKTRACE;

    private static Field sBacktraceField;
    private static Field sDepthField;
    private static boolean sInitialized;

    static {
        JAVA_VERSION = getJavaVersion();
        OFF_FIELD_SLOT = JAVA_VERSION > 11 ? 28 : 32;
        OFF_FIELD_BACKTRACE = JAVA_VERSION >= 9 ? 16 : 12;
        if (!Utils.ANDROID) {
            Trex.setPlatform(new TrexJvm());
        }
    }

    private TrexJvm() {}

    public static void init() {
        sBacktraceField = Utils.findFieldIfExists(Throwable.class, "backtrace");
        sDepthField = Utils.findFieldIfExists(Throwable.class, "depth");
        sInitialized = true;
    }

    private static int getJavaVersion() {
        String version = System.getProperty("java.specification.version");
        if (version.startsWith("1.")) {
            version = version.substring(2);
        }
        return Float.valueOf(version).intValue();
    }

    @Override
    public boolean isInitialized0() {
        return sInitialized;
    }

    @Override
    public Object getBackTrace0(Throwable throwable) {
        if (sBacktraceField == null) {
            return Utils.UNSAFE.getObject(throwable, OFF_FIELD_BACKTRACE);
        } else {
            return Utils.getField(sBacktraceField, throwable);
        }
    }

    @Override
    public void setBackTrace0(Throwable throwable, Object backTrace) {
        if (sBacktraceField == null) {
            Utils.UNSAFE.putObject(throwable, OFF_FIELD_BACKTRACE, backTrace);
        } else {
            Utils.setField(sBacktraceField, throwable, backTrace);
        }
    }

    @Override
    public void setDepth0(Throwable throwable, int depth) {
        try {
            sDepthField.setInt(throwable, depth);
        } catch (IllegalAccessException e) {
            // this will never happen
            throw Utils.wrap(e);
        }
    }

    @Override
    public Object getBackTrace0(StackTraceElement stackTrace) {
        return null;
    }

    @Override
    public BackTraceParser newBackTraceParser0(
            StackTraceElement[] stackTraces,
            Object backTrace,
            TrexOption option,
            int stackFrameOptionHashCode) {
        return new BackTraceParserJ8(stackTraces, backTrace, option, stackFrameOptionHashCode);
    }

    @Override
    public Class<?> getDeclaringClass0(Object vmMethod) {
        return ((Member) vmMethod).getDeclaringClass();
    }

    @Override
    public Member getExecutable0(Object vmMethod) {
        return (Member) vmMethod;
    }

    @Override
    public String getModuleName0(Class<?> clazz) {
        if (JAVA_VERSION >= 9) {
            return clazz.getModule().getName();
        } else {
            return null;
        }
    }

    @Override
    public List<Class<?>> getCallerClasses0(Object backTrace) {
        return BackTraceParserJ8.getCallerClasses(backTrace);
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
        if (JAVA_VERSION >= 9) {
            return new StackTraceElement(
                    classLoaderName,
                    moduleName,
                    moduleVersion,
                    declaringClass,
                    methodName,
                    fileName,
                    lineNumber);
        }
        return null;
    }

    @Override
    public void printStackTraceInfoPrefix0(
            TrexPrinter printer, StackTraceElement stackTrace, TrexOption option) {
        if (JAVA_VERSION >= 9) {
            String classLoaderName = stackTrace.getClassLoaderName();
            String moduleName = stackTrace.getModuleName();
            String moduleVersion = stackTrace.getModuleVersion();
            TrexStyle.printStackTraceInfoPrefix(
                    printer, classLoaderName, moduleName, moduleVersion, option);
        }
    }

    @Override
    public TrexOption cloneOption0(TrexOption option) {
        return option.clone();
    }
}
