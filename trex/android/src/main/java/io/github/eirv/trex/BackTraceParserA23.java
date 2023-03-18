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

import android.os.Build;

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Eirv
 * @date 2023/2/7 12:52
 */
class BackTraceParserA23 implements BackTraceParser {
    private final Object[] backTrace;
    private final boolean is64Bit;
    private final int[] methods32;
    private final long[] methods64;
    private final TrexAndroidOption option;
    private final TrexStyle style;
    private final int stackFrameOptionHashCode;
    private final Method stubMethod;
    private StackTraceElement[] stackTraces;

    public BackTraceParserA23(
            StackTraceElement[] stackTraces,
            Object backTrace_,
            TrexOption option,
            int stackFrameOptionHashCode) {

        Object[] backTrace = (Object[]) backTrace_;
        this.stackTraces = stackTraces;
        this.backTrace = backTrace;
        Object methods = backTrace[0];
        if (is64Bit = methods instanceof long[]) {
            methods32 = null;
            methods64 = (long[]) methods;
        } else {
            methods32 = (int[]) methods;
            methods64 = null;
        }
        this.option = (TrexAndroidOption) option;
        style = option.getStyle();
        this.stackFrameOptionHashCode = stackFrameOptionHashCode;
        stubMethod = TrexAndroidImpl.getStubMethod();
    }

    public static void resetMethod(Member stubMethod, Class<?> declaringClass, long artMethod) {
        try {
            if (TrexAndroidImpl.sDeclaringClassField != null) {
                TrexAndroidImpl.sDeclaringClassField.set(stubMethod, declaringClass);
            } else {
                Utils.UNSAFE.putObject(
                        stubMethod, TrexAndroidImpl.OFF_FIELD_DECLARING_CLASS, declaringClass);
            }

            if (TrexAndroidImpl.sArtMethodField != null) {
                TrexAndroidImpl.sArtMethodField.setLong(stubMethod, artMethod);
            } else {
                Utils.UNSAFE.putLong(stubMethod, TrexAndroidImpl.OFF_FIELD_ART_METHOD, artMethod);
            }

            int accessFlags = TrexAndroidImpl.getInt(artMethod + TrexAndroidImpl.OFF_ACCESS_FLAGS);
            if (TrexAndroidImpl.sAccessFlagsField != null) {
                TrexAndroidImpl.sAccessFlagsField.setInt(stubMethod, accessFlags);
            } else {
                Utils.UNSAFE.putInt(
                        stubMethod, TrexAndroidImpl.OFF_FIELD_ACCESS_FLAGS, accessFlags);
            }

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                TrexAndroidImpl.sDeclaringClassOfOverriddenMethodField.set(
                        stubMethod, declaringClass);
                int dexMethodIndex =
                        TrexAndroidImpl.getInt(artMethod + TrexAndroidImpl.OFF_DEX_METHOD_INDEX);
                TrexAndroidImpl.sDexMethodIndexField.setInt(stubMethod, dexMethodIndex);
            }
        } catch (IllegalAccessException e) {
            // this will never happen
            throw Utils.wrap(e);
        }
    }

    public static List<Class<?>> getCallerClasses(Object backTrace_) {
        Object[] backTrace = (Object[]) backTrace_;
        int len = backTrace.length;
        List<Class<?>> callerClasses = new ArrayList<>(len);
        for (int i = 1; len > i; i++) {
            callerClasses.add((Class<?>) backTrace[i]);
        }
        return callerClasses;
    }

    @Override
    public int depth() {
        return backTrace.length - 1;
    }

    @Override
    public void setStackTrace(StackTraceElement[] stackTraces) {
        this.stackTraces = stackTraces;
    }

    @Override
    public StackFrame parse(int index) {
        Class<?> declaringClass = (Class<?>) backTrace[index + 1];
        long artMethod = is64Bit ? methods64[index] : methods32[index];
        int dexPcIdx = stackTraces.length + index;
        int dexPc = is64Bit ? (int) methods64[dexPcIdx] : methods32[dexPcIdx];

        StackTraceElement stackTrace = stackTraces[index];
        ArtMethodM artMethodM = new ArtMethodM(declaringClass, artMethod);
        int hideFlags =
                Utils.getHideFlags(
                        declaringClass,
                        stackTrace.getMethodName(),
                        artMethodM.getAccessFlags(),
                        option);
        VmMethodKey vmMethodKey = null;

        boolean cacheEnabled = option.isCacheEnabled();

        if (cacheEnabled) {
            vmMethodKey = new VmMethodKey(artMethodM, stackFrameOptionHashCode, hideFlags != 0);
            StackFrame cache = Trex.sStackFrameCache.get(vmMethodKey);
            if (cache != null) {
                int lineNumber = stackTrace.getLineNumber();
                return cache.clone(lineNumber, dexPc);
            }
        }

        String descriptor;
        if (hideFlags != 0) {
            descriptor = style.getDescriptor(stackTrace, option, hideFlags);
        } else {
            resetMethod(stubMethod, declaringClass, artMethod);
            descriptor = style.getDescriptor(stubMethod, option);
        }
        String moduleName =
                option.isModuleNameFinderEnabled()
                        ? TrexAndroidImpl.getClassModuleNameInternal(stubMethod.getDeclaringClass())
                        : null;

        StackFrame stackframe =
                TrexAndroidImpl.newStackFrame(
                        stackTrace,
                        declaringClass,
                        descriptor,
                        moduleName,
                        dexPc,
                        artMethodM,
                        option.isProxyImplEnabled());

        if (option.isCacheEnabled()) {
            Trex.sStackFrameCache.put(vmMethodKey, stackframe);
        }

        return stackframe;
    }
}
