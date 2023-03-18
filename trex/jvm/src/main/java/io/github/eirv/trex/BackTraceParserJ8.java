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

import sun.misc.Unsafe;

import java.lang.ref.SoftReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Eirv
 * @date 2023/2/7 13:02
 */
class BackTraceParserJ8 implements BackTraceParser {
    private static final List<ExecutableItem> executableItems = new ArrayList<>();
    private final short[] slots;
    private final Object[] classes;
    private final TrexOption option;
    private final TrexStyle style;
    private final int stackFrameOptionHashCode;
    private StackTraceElement[] stackTraces;

    public BackTraceParserJ8(
            StackTraceElement[] stackTraces,
            Object backTrace_,
            TrexOption option,
            int stackFrameOptionHashCode) {
        Object[] backTrace = (Object[]) backTrace_;
        this.stackTraces = stackTraces;
        this.option = option;
        style = option.getStyle();
        this.stackFrameOptionHashCode = stackFrameOptionHashCode;

        short[] slots = null;
        Object[] classes = null;
        do {
            slots = mergeArray(slots, (short[]) backTrace[0]);
            classes = mergeArray(classes, (Object[]) backTrace[2]);
            backTrace = (Object[]) backTrace[4];
        } while (backTrace != null);
        this.slots = slots;
        this.classes = classes;
    }

    private static short[] mergeArray(short[] arrayA, short[] arrayB) {
        if (arrayA == null) return arrayB;
        int lenA = arrayA.length;
        int lenB = arrayB.length;
        short[] result = new short[lenA + lenB];
        mergeArray(result, arrayA, lenA, arrayB, lenB);
        return result;
    }

    private static Object[] mergeArray(Object[] arrayA, Object[] arrayB) {
        if (arrayA == null) return arrayB;
        int lenA = arrayA.length;
        int lenB = arrayB.length;
        Object[] result = new Object[lenA + lenB];
        mergeArray(result, arrayA, lenA, arrayB, lenB);
        return result;
    }

    private static void mergeArray(
            Object target, Object arrayA, int lenA, Object arrayB, int lenB) {
        System.arraycopy(arrayA, 0, target, 0, lenA);
        System.arraycopy(arrayB, 0, target, lenA, lenB);
    }

    public static List<Class<?>> getCallerClasses(Object backTrace_) {
        Object[] backTrace = (Object[]) backTrace_;
        Object[] classes = (Object[]) backTrace[2];
        List<Class<?>> callerClasses = new ArrayList<>(classes.length);
        for (Object clazz : classes) {
            callerClasses.add((Class<?>) clazz);
        }
        return callerClasses;
    }

    @Override
    public int depth() {
        int len = classes.length;
        for (int i = 0; len > i; i++) {
            if (classes[i] == null) return i;
        }
        return len;
    }

    @Override
    public void setStackTrace(StackTraceElement[] stackTraces) {
        this.stackTraces = stackTraces;
    }

    @Override
    public StackFrame parse(int index) {
        int slot = slots[index] & 0xFFFF;
        Class<?> declaringClass = (Class<?>) classes[index];

        ExecutableItem executableItem = new ExecutableItem(declaringClass);
        int executableItemIndex = executableItems.indexOf(executableItem);
        if (executableItemIndex != -1) {
            executableItem = executableItems.get(executableItemIndex);
        } else {
            executableItems.add(executableItem);
        }

        StackTraceElement stackTrace = stackTraces[index];
        Executable executable = executableItem.findBySlot(slot);
        int hideFlags =
                Utils.getHideFlags(
                        declaringClass,
                        stackTrace.getMethodName(),
                        executable.getModifiers(),
                        option);
        VmMethodKey vmMethodKey = null;

        boolean cacheEnabled = executable != null && option.isCacheEnabled();

        if (cacheEnabled) {
            vmMethodKey = new VmMethodKey(executable, stackFrameOptionHashCode, hideFlags != 0);
            StackFrame cache = Trex.sStackFrameCache.get(vmMethodKey);
            if (cache != null) {
                int lineNumber = stackTrace.getLineNumber();
                return cache.clone(lineNumber, -1);
            }
        }

        String descriptor =
                executable != null && hideFlags == 0
                        ? style.getDescriptor(executable, option)
                        : style.getDescriptor(stackTrace, option, hideFlags);

        StackFrameImpl stackFrame =
                new StackFrameImpl(stackTrace, descriptor, null, -1, executable);
        if (TrexJvm.JAVA_VERSION >= 9) {
            stackFrame.moduleName = stackTrace.getModuleName();
            stackFrame.moduleVersion = stackTrace.getModuleVersion();
            stackFrame.classLoaderName = stackTrace.getClassLoaderName();
        }

        if (cacheEnabled) {
            Trex.sStackFrameCache.put(vmMethodKey, stackFrame);
        }

        return stackFrame;
    }

    private static class ExecutableItem {
        private final Class<?> clazz;
        private SoftReference<Pair<int[], Executable[]>> pairRef;

        public ExecutableItem(Class<?> clazz) {
            this.clazz = clazz;
        }

        public Executable findBySlot(int slot) {
            Pair<int[], Executable[]> pair = pairRef != null ? pairRef.get() : null;
            if (pair == null) {
                pair = findExecutables();
            }
            int[] slots = pair.first;
            Executable[] executables = pair.second;
            for (int i = 0, len = slots.length; len > i; i++) {
                if (slots[i] == slot) {
                    return executables[i];
                }
            }
            return null;
        }

        private Pair<int[], Executable[]> findExecutables() {
            Unsafe unsafe = Utils.UNSAFE;

            Constructor<?>[] constructors = clazz.getDeclaredConstructors();
            Method[] methods = clazz.getDeclaredMethods();

            int cLen = constructors.length;
            int mLen = methods.length;
            int len = cLen + mLen;

            int[] slots = new int[len];
            Executable[] executables = new Executable[len];

            int i = 0;
            for (; cLen > i; i++) {
                Executable executable = constructors[i];
                slots[i] = unsafe.getInt(executable, TrexJvm.OFF_FIELD_SLOT);
                executables[i] = executable;
            }
            for (int j = 0; mLen > j; j++, i++) {
                Executable executable = methods[j];
                slots[i] = unsafe.getInt(executable, TrexJvm.OFF_FIELD_SLOT);
                executables[i] = executable;
            }

            Pair<int[], Executable[]> pair = new Pair<>(slots, executables);
            pairRef = new SoftReference<>(pair);
            return pair;
        }

        @Override
        public int hashCode() {
            return clazz.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof ExecutableItem)) return false;

            ExecutableItem peer = (ExecutableItem) obj;
            return clazz == peer.clazz;
        }
    }
}
