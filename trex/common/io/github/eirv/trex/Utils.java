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

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * @author Eirv
 * @date 2023/2/7 12:19
 */
class Utils {
    static final boolean ANDROID;
    static final String LINE_SEPARATOR = System.getProperty("line.separator");
    static final ClassLoader BOOT_CLASS_LOADER = ClassLoader.class.getClassLoader();
    static final ClassLoader SYSTEM_CLASS_LOADER = getSystemClassLoader();
    static final Unsafe UNSAFE;

    private static final int ACC_BRIDGE = 0x40;
    private static final int ACC_SYNTHETIC = 0x1000;
    private static final int OFF_FIELD_OVERRIDE;

    static {
        ANDROID = isAndroid();

        try {
            // Android 可以直接反射调用 Unsafe#getUnsafe 获取 Unsafe
            // JVM 只能反射取字段 Unsafe#theUnsafe 获取 Unsafe
            Field theUnsafeField;
            try {
                theUnsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            } catch (NoSuchFieldException e) {
                if (ANDROID) {
                    theUnsafeField = Unsafe.class.getDeclaredField("THE_ONE");
                } else throw e;
            }
            theUnsafeField.setAccessible(true);
            UNSAFE = (Unsafe) theUnsafeField.get(null);

            if (!ANDROID) {
                // 取基本类型感觉内存搜索应该比写死偏移更稳定
                int offset = -1;
                for (int i = 0; 32 >= i; i++) {
                    byte value = UNSAFE.getByte(theUnsafeField, i);
                    if (value != 1) continue;
                    theUnsafeField.setAccessible(false);
                    value = UNSAFE.getByte(theUnsafeField, i);
                    if (value == 0) {
                        offset = i;
                        break;
                    }
                    theUnsafeField.setAccessible(true);
                }
                if (offset == -1) {
                    throw new NoSuchFieldException("AccessibleObject#override");
                }
                OFF_FIELD_OVERRIDE = offset;
            } else {
                OFF_FIELD_OVERRIDE = 0;
            }
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    static Field findFieldIfExists(Class<?> clazz, String name) {
        try {
            return findField(clazz, name);
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    static Field findField(Class<?> clazz, String name) throws NoSuchFieldException {
        Field field = clazz.getDeclaredField(name);
        if (ANDROID) {
            field.setAccessible(true);
        } else {
            UNSAFE.putBoolean(field, OFF_FIELD_OVERRIDE, true);
        }
        return field;
    }

    static void setField(Field field, Object receiver, Object value) {
        assert field != null;
        try {
            field.set(receiver, value);
        } catch (IllegalAccessException e) {
            // this will never happen
            throw wrap(e);
        }
    }

    static Object getField(Field field, Object receiver) {
        assert field != null;
        try {
            return field.get(receiver);
        } catch (IllegalAccessException e) {
            // this will never happen
            throw wrap(e);
        }
    }

    static Error wrap(IllegalAccessException e) {
        return new IllegalAccessError(e.getMessage());
    }

    static <T> Set<T> newIdentityHashSet() {
        return Collections.newSetFromMap(new IdentityHashMap<>());
    }

    static String[] splitClassName(String className) {
        // return className.split("\\.");
        List<String> strings = new ArrayList<>(6);
        char[] chars = className.toCharArray();
        int i = 0, off = 0;
        for (int len = chars.length; len > i; i++) {
            if (chars[i] == '.') {
                strings.add(new String(chars, off, i - off));
                off = ++i;
            }
        }
        strings.add(new String(chars, off, i - off));
        return strings.toArray(new String[strings.size()]);
    }

    static <T> T select(T expectedValue, T defaultValue) {
        if (expectedValue != null) return expectedValue;
        return defaultValue;
    }

    static boolean isNotEmpty(String str) {
        return str != null && !str.isEmpty();
    }

    static int hashCode(boolean z) {
        return z ? 1231 : 1237;
    }

    static int hashCode(Object o) {
        return o != null ? o.hashCode() : 0;
    }

    static boolean equals(String stringA, String stringB) {
        return stringA != null ? stringA.equals(stringB) : stringB == null;
    }

    static void requireNonNull(Object obj, String name) {
        if (obj == null) {
            throw new NullPointerException(name.concat(" == null"));
        }
    }

    static int getHideFlags(
            Class<?> declaringClass, String name, int accessFlags, TrexOption option) {
        int flags = 0;

        if (!option.isBootMethodTypeVisible()) {
            ClassLoader classLoader = declaringClass.getClassLoader();
            boolean bootClassLoader =
                    classLoader == Utils.BOOT_CLASS_LOADER
                            || classLoader == Utils.SYSTEM_CLASS_LOADER;
            boolean proxyClass = Proxy.isProxyClass(declaringClass);
            if (bootClassLoader && !proxyClass) {
                flags |= TrexStyle.FLAG_BOOT_CLASS_LOADER;
            }
        }

        if (!option.isSynthesizedMethodTypeVisible()) {
            if (declaringClass.isSynthetic()) {
                flags |= TrexStyle.FLAG_CLASS_SYNTHETIC;
            }
            boolean synthetic = (accessFlags & ACC_SYNTHETIC) != 0;
            boolean bridge = (accessFlags & ACC_BRIDGE) != 0;
            if (synthetic || bridge) {
                flags |= TrexStyle.FLAG_METHOD_SYNTHETIC;
            }
        }

        if (!option.isUniqueMethodTypeVisible()) {
            ExecutableNames executableNames = Trex.sExecutableNamesCache.get(declaringClass);
            if (executableNames == null) {
                executableNames = new ExecutableNames(declaringClass);
                Trex.sExecutableNamesCache.put(declaringClass, executableNames);
            }
            if ("<clinit>".equals(name)
                    || ("<init>".equals(name) && executableNames.isOnlyOneConstructor())
                    || executableNames.contains(name)) {
                flags |= TrexStyle.FLAG_EXECUTABLE_UNIQUE;
            }
        }

        return flags;
    }

    private static ClassLoader getSystemClassLoader() {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        for (; ; ) {
            ClassLoader parentClassLoader = classLoader.getParent();
            if (parentClassLoader != null) {
                classLoader = parentClassLoader;
            } else {
                return classLoader;
            }
        }
    }

    private static boolean isAndroid() {
        try {
            SYSTEM_CLASS_LOADER.loadClass("android.R");
            return true;
        } catch (ClassNotFoundException ignored) {
        }
        return false;
    }
}
