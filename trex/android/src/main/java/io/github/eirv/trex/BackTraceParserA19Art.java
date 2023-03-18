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

import java.lang.reflect.ArtMethod;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Eirv
 * @date 2023/2/7 12:52
 */
class BackTraceParserA19Art implements BackTraceParser {
    private final Object[] backTrace;
    private final int[] dexPcs;
    private final TrexAndroidOption option;
    private final TrexStyle style;
    private final int stackFrameOptionHashCode;
    private final Method stubMethod;
    private StackTraceElement[] stackTraces;

    public BackTraceParserA19Art(
            StackTraceElement[] stackTraces,
            Object backTrace_,
            TrexOption option,
            int stackFrameOptionHashCode) {

        Object[] backTrace = (Object[]) backTrace_;
        this.stackTraces = stackTraces;
        this.backTrace = backTrace;
        dexPcs = (int[]) backTrace[backTrace.length - 1];
        this.option = (TrexAndroidOption) option;
        style = option.getStyle();
        this.stackFrameOptionHashCode = stackFrameOptionHashCode;
        stubMethod = TrexAndroidImpl.getStubMethod();
    }

    public static void resetMethod(Member stubMethod, Object artMethod) {
        Utils.setField(TrexAndroidImpl.sArtMethodField, stubMethod, artMethod);
    }

    public static List<Class<?>> getCallerClasses(Object backTrace_) {
        Object[] backTrace = (Object[]) backTrace_;
        int len = backTrace.length - 1;
        List<Class<?>> callerClasses = new ArrayList<>(len);
        for (Object artMethod : backTrace) {
            Class<?> clazz =
                    (Class<?>) Utils.getField(TrexAndroidImpl.sDeclaringClassField, artMethod);
            callerClasses.add(clazz);
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
        ArtMethod artMethod = (ArtMethod) backTrace[index];
        resetMethod(stubMethod, artMethod);
        int dexPc = dexPcs[index];

        StackTraceElement stackTrace = stackTraces[index];
        int hideFlags =
                Utils.getHideFlags(
                        stubMethod.getDeclaringClass(),
                        stackTrace.getMethodName(),
                        artMethod.getAccessFlags(),
                        option);
        VmMethodKey vmMethodKey = null;

        boolean cacheEnabled = option.isCacheEnabled();

        if (cacheEnabled) {
            vmMethodKey = new VmMethodKey(artMethod, stackFrameOptionHashCode, hideFlags != 0);
            StackFrame cache = Trex.sStackFrameCache.get(vmMethodKey);
            if (cache != null) {
                int lineNumber = stackTrace.getLineNumber();
                return cache.clone(lineNumber, dexPc);
            }
        }

        String descriptor =
                hideFlags != 0
                        ? style.getDescriptor(stackTrace, option, hideFlags)
                        : style.getDescriptor(stubMethod, option);
        String moduleName =
                option.isModuleNameFinderEnabled()
                        ? TrexAndroidImpl.getClassModuleNameInternal(stubMethod.getDeclaringClass())
                        : null;

        StackFrame stackFrame =
                TrexAndroidImpl.newStackFrame(
                        stackTrace,
                        stubMethod.getDeclaringClass(),
                        descriptor,
                        moduleName,
                        dexPc,
                        artMethod,
                        option.isProxyImplEnabled());

        if (cacheEnabled) {
            Trex.sStackFrameCache.put(vmMethodKey, stackFrame);
        }

        return stackFrame;
    }
}
