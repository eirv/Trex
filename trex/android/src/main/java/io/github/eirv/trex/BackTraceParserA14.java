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

import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * @author Eirv
 * @date 2023/2/7 12:52
 */
class BackTraceParserA14 implements BackTraceParser {
    private static final Map<Integer, String> sDvmDescriptorCache = new WeakHashMap<>();
    private static final Map<Integer, Member> sDvmMethodCache = new WeakHashMap<>();
    private final int[] backTrace;
    private final TrexAndroidOption option;
    private final TrexStyle style;
    private final int stackFrameOptionHashCode;
    private StackTraceElement[] stackTraces;

    public BackTraceParserA14(
            StackTraceElement[] stackTraces,
            Object backTrace,
            TrexOption option,
            int stackFrameOptionHashCode) {
        this.stackTraces = stackTraces;
        this.backTrace = (int[]) backTrace;
        this.option = (TrexAndroidOption) option;
        style = option.getStyle();
        this.stackFrameOptionHashCode = stackFrameOptionHashCode;
    }

    public static List<Class<?>> getCallerClasses(Object backTrace_) {
        int[] backTrace = (int[]) backTrace_;
        int len = backTrace.length / 2;
        List<Class<?>> callerClasses = new ArrayList<>(len);
        for (int i = 0; len > i; i++) {
            int dvmMethod = backTrace[i * 2];
            Member executable = TrexAndroid.nGetDvmMethod(dvmMethod);
            callerClasses.add(executable.getDeclaringClass());
        }
        return callerClasses;
    }

    static String getDvmDescriptor(
            int dvmMethod, boolean bootMethodTypeVisible, boolean synthesizedMethodTypeVisible) {
        int key =
                dvmMethod
                        + (bootMethodTypeVisible ? 1 : 2)
                        + (synthesizedMethodTypeVisible ? 3 : 4);
        String descriptor = sDvmDescriptorCache.get(key);
        if (descriptor == null) {
            descriptor =
                    TrexAndroid.nGetDvmDescriptor(
                            dvmMethod, bootMethodTypeVisible, synthesizedMethodTypeVisible);
            sDvmDescriptorCache.put(key, descriptor);
        }
        return descriptor;
    }

    static Member getDvmMethod(int dvmMethod) {
        Member executable = sDvmMethodCache.get(dvmMethod);
        if (executable == null) {
            executable = TrexAndroid.nGetDvmMethod(dvmMethod);
            sDvmMethodCache.put(dvmMethod, executable);
        }
        return executable;
    }

    @Override
    public int depth() {
        return backTrace.length / 2;
    }

    @Override
    public void setStackTrace(StackTraceElement[] stackTraces) {
        this.stackTraces = stackTraces;
    }

    @Override
    public StackFrame parse(int index) {
        int dvmMethod = backTrace[index * 2];
        int dexPc = backTrace[index * 2 + 1];

        StackTraceElement stackTrace = stackTraces[index];
        VmMethodKey vmMethodKey = null;

        boolean cacheEnabled = option.isCacheEnabled();
        boolean dalvikAccelerateEnabled = option.isDalvikAccelerateEnabled();

        String descriptor;
        String moduleName;
        Class<?> declaringClass;
        Object vmMethod;

        if (dalvikAccelerateEnabled) {
            descriptor =
                    getDvmDescriptor(
                            dvmMethod,
                            option.isBootMethodTypeVisible(),
                            option.isSynthesizedMethodTypeVisible());
            moduleName =
                    option.isModuleNameFinderEnabled()
                            ? ModuleNameFinder.find(stackTrace.getClassName())
                            : null;
            declaringClass = null;
            vmMethod = dvmMethod;

            if (option.isColorSchemeEnabled()) {
                descriptor = option.getColor(TrexOption.COLOR_TEXT).concat(descriptor);
            }
        } else {
            Member executable = getDvmMethod(dvmMethod);
            declaringClass = executable.getDeclaringClass();
            int hideFlags =
                    Utils.getHideFlags(
                            declaringClass,
                            stackTrace.getMethodName(),
                            executable.getModifiers(),
                            option);
            if (cacheEnabled) {
                vmMethodKey = new VmMethodKey(dvmMethod, stackFrameOptionHashCode, hideFlags != 0);
                StackFrame cache = Trex.sStackFrameCache.get(vmMethodKey);
                if (cache != null) {
                    int lineNumber = stackTrace.getLineNumber();
                    return cache.clone(lineNumber, dexPc);
                }
            }
            descriptor =
                    hideFlags != 0
                            ? style.getDescriptor(stackTrace, option, hideFlags)
                            : style.getDescriptor(executable, option);
            moduleName =
                    option.isModuleNameFinderEnabled()
                            ? TrexAndroidImpl.getClassModuleNameInternal(declaringClass)
                            : null;
            vmMethod = executable;
        }

        StackFrame stackFrame =
                TrexAndroidImpl.newStackFrame(
                        stackTrace,
                        declaringClass,
                        descriptor,
                        moduleName,
                        dexPc,
                        vmMethod,
                        option.isProxyImplEnabled());

        if (cacheEnabled && !dalvikAccelerateEnabled) {
            Trex.sStackFrameCache.put(vmMethodKey, stackFrame);
        }

        return stackFrame;
    }
}
